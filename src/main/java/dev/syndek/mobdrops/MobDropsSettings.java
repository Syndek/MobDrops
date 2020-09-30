/*
 * Copyright (C) 2020 Louis Salkeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.syndek.mobdrops;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MobDropsSettings {
    private final MobDropsPlugin plugin;

    private final Map<EntityType, Collection<Drop>> drops       = new EnumMap<>(EntityType.class);
    private final Collection<Drop>                  globalDrops = new ArrayList<>();

    public MobDropsSettings(final @NotNull MobDropsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();

        // Clear drops on each load. We don't need to recreate the collections each time.
        this.drops.clear();
        this.globalDrops.clear();

        final Configuration config = this.plugin.getConfig();

        // Process custom items first so that drops using them are valid.
        final Map<String, ItemStack> items = new HashMap<>();
        for (final Map<?, ?> itemData : config.getMapList("items")) {
            try {
                final Pair pair = this.parseItem(itemData);
                items.put(pair.getKey(), pair.getItem());
            } catch (final InvalidConfigurationException | ClassCastException ex) {
                this.plugin.getLogger().warning("Error whilst loading configuration: " + ex.getMessage());
                this.plugin.getLogger().warning("The item will be skipped.");
            }
        }

        for (final Map<?, ?> dropData : config.getMapList("drops")) {
            try {
                this.parseDrop(dropData, items);
            } catch (final InvalidConfigurationException | IllegalArgumentException | ClassCastException ex) {
                this.plugin.getLogger().warning("Error whilst loading configuration: " + ex.getMessage());
                this.plugin.getLogger().warning("The drop will be skipped.");
            }
        }
    }

    public @NotNull Iterable<Drop> getDropsFor(final @NotNull EntityType entityType) {
        final Iterable<Drop> drops = this.drops.get(entityType);
        return drops == null ? Collections.emptyList() : drops;
    }

    public @NotNull Iterable<Drop> getGlobalDrops() {
        return this.globalDrops;
    }

    private @NotNull Pair parseItem(final @NotNull Map<?, ?> itemData) throws InvalidConfigurationException {
        final String name = (String) itemData.get("name");
        if (name == null) {
            throw new InvalidConfigurationException("Missing key 'name' in custom item.");
        }

        final String materialString = (String) itemData.get("material");
        if (materialString == null) {
            throw new InvalidConfigurationException("Missing key 'material' in custom item '" + name + "'.");
        }

        final Material material = Material.matchMaterial(materialString);
        if (material == null || !material.isItem()) {
            throw new InvalidConfigurationException("Invalid material in custom item '" + name + "'.");
        }

        final ItemStack item = new ItemStack(material);
        final Pair pair = new Pair(name, item);

        final Map<?, ?> metaData = (Map<?, ?>) itemData.get("meta");
        if (metaData != null) {
            final ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return pair;
            }

            final String displayName = (String) metaData.get("display-name");
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }

            final String lore = (String) metaData.get("lore");
            if (lore != null) {
                final List<String> loreLines = Arrays.stream(lore.split("\n"))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
                meta.setLore(loreLines);
            }

            final Boolean unbreakable = (Boolean) metaData.get("unbreakable");
            if (unbreakable != null) {
                meta.setUnbreakable((Boolean) metaData.get("unbreakable"));
            }

            item.setItemMeta(meta);
        }

        return pair;
    }

    @SuppressWarnings("unchecked") // Fall back on ClassCastException for configuration errors.
    private void parseDrop(final @NotNull Map<?, ?> dropData, final @NotNull Map<String, ItemStack> items) throws InvalidConfigurationException {
        final String itemName = (String) dropData.get("item");
        if (itemName == null) {
            throw new InvalidConfigurationException("Missing key 'item' in drop.");
        }

        final ItemStack item;
        if (itemName.startsWith("custom:")) {
            final ItemStack customItem = items.get(itemName.substring(7));
            if (customItem == null) {
                throw new InvalidConfigurationException("Invalid custom item '" + itemName + "' in drop.");
            }
            item = customItem;
        } else {
            final Material material = Material.matchMaterial(itemName);
            if (material == null) {
                throw new InvalidConfigurationException("Invalid material '" + itemName + "' in drop.");
            }
            item = new ItemStack(material);
        }

        final List<String> applicableWorldNames = (List<String>) dropData.get("applicable-worlds");
        final Collection<UUID> applicableWorldIds = applicableWorldNames == null ? null : applicableWorldNames.stream()
            .map(this.plugin.getServer()::getWorld)
            .filter(Objects::nonNull)
            .map(World::getUID)
            .collect(Collectors.toList());

        final Drop drop = new Drop(
            applicableWorldIds,
            dropData.containsKey("player-kills-only")
                ? (Boolean) dropData.get("player-kills-only")
                : false,
            (String) dropData.get("permission-node"),
            item,
            (Integer) dropData.get("quantity"),
            ((Number) dropData.get("chance")).floatValue()
        );

        final List<String> entityTypeNames = (List<String>) dropData.get("entity-types");

        // If no entity-types key is provided, the drop is global (applicable to all entities).
        if (entityTypeNames == null) {
            this.globalDrops.add(drop);
            return;
        }

        // If entity-types is present, but is empty, this is an error.
        if (entityTypeNames.isEmpty()) {
            throw new InvalidConfigurationException("Key 'entity-types' is present, but array is empty.");
        }

        final Iterable<EntityType> entityTypes = entityTypeNames.stream()
            .map(String::toUpperCase)
            .map(type -> {
                try {
                    return EntityType.valueOf(type);
                } catch (final IllegalArgumentException ex) {
                    // We want to ignore unrecognised entity types.
                    // They might change from update to update, and it's not worth breaking configuration over that.
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        for (final EntityType entityType : entityTypes) {
            if (this.drops.containsKey(entityType)) {
                this.drops.get(entityType).add(drop);
            } else {
                final Collection<Drop> drops = new ArrayList<>();
                drops.add(drop);
                this.drops.put(entityType, drops);
            }
        }
    }

    private static class Pair {
        private final String    key;
        private final ItemStack item;

        public Pair(final @NotNull String key, final @NotNull ItemStack item) {
            this.key = key;
            this.item = item;
        }

        public @NotNull String getKey() {
            return this.key;
        }

        public @NotNull ItemStack getItem() {
            return this.item;
        }
    }
}