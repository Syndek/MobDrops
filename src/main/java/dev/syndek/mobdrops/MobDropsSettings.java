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
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class MobDropsSettings {
    private final MobDropsPlugin plugin;

    private final Map<EntityType, Collection<Drop>> drops = new EnumMap<>(EntityType.class);

    public MobDropsSettings(final @NotNull MobDropsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();

        // Clear drops map on each load. We don't need to recreate it each time.
        this.drops.clear();

        final Configuration config = this.plugin.getConfig();

        // Process custom items first so that drops using them are valid.
        final Map<String, ItemStack> items = config.getMapList("items").stream()
            .map(this::parseItem)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Pair::getKey, Pair::getItem));

        config.getMapList("drops").forEach(dropData -> this.parseDrop(dropData, items));
    }

    public @NotNull Iterable<Drop> getDropsFor(final @NotNull EntityType entityType) {
        final Iterable<Drop> drops = this.drops.get(entityType);
        return drops == null ? Collections.emptyList() : drops;
    }

    private @Nullable Pair parseItem(final @NotNull Map<?, ?> itemData) {
        final String name = (String) itemData.get("name");
        if (name == null) {
            return null;
        }

        final String materialString = (String) itemData.get("material");
        if (materialString == null) {
            return null;
        }

        final Material material = Material.matchMaterial(materialString);
        if (material == null || !material.isItem()) {
            return null;
        }

        final ItemStack item = new ItemStack(material);
        final Pair pair = new Pair(name, item);

        final Map<?, ?> metaData = (Map<?, ?>) itemData.get("meta");
        if (metaData != null) {
            final ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return pair;
            }
            if (metaData.containsKey("display-name")) {
                final String displayName = (String) metaData.get("display-name");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            if (metaData.containsKey("lore")) {
                final String lore = (String) metaData.get("lore");
                final List<String> loreLines = Arrays.stream(lore.split("\n"))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
                meta.setLore(loreLines);
            }
            if (metaData.containsKey("unbreakable")) {
                meta.setUnbreakable((Boolean) metaData.get("unbreakable"));
            }
            item.setItemMeta(meta);
        }

        return pair;
    }

    private void parseDrop(final @NotNull Map<?, ?> dropData, final @NotNull Map<String, ItemStack> items) {
        final List<String> entityTypeNames = (List<String>) dropData.get("entity-types");
        if (entityTypeNames == null || entityTypeNames.isEmpty()) {
            return;
        }

        final String itemName = (String) dropData.get("item");
        if (itemName == null) {
            return;
        }

        final ItemStack item;
        if (itemName.startsWith("custom:")) {
            final ItemStack customItem = items.get(itemName.substring(7));
            if (customItem == null) {
                return;
            }
            item = customItem;
        } else {
            final Material material = Material.matchMaterial(itemName);
            if (material == null) {
                return;
            }
            item = new ItemStack(material);
        }

        final Iterable<EntityType> entityTypes = entityTypeNames.stream()
            .map(String::toUpperCase)
            .map(EntityType::valueOf)
            .collect(Collectors.toList());

        final List<String> applicableWorldNames = (List<String>) dropData.get("applicable-worlds");
        final Collection<UUID> applicableWorldIds = applicableWorldNames == null ? null : applicableWorldNames.stream()
            .map(this.plugin.getServer()::getWorld)
            .filter(Objects::nonNull)
            .map(World::getUID)
            .collect(Collectors.toList());

        final Number chanceValue = (Number) dropData.get("chance");

        final Drop drop = new Drop(
            applicableWorldIds,
            dropData.containsKey("player-kills-only")
                ? (Boolean) dropData.get("player-kills-only")
                : false,
            (String) dropData.get("permission-node"),
            item,
            (Integer) dropData.get("quantity"),
            chanceValue.floatValue() / 100
        );

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