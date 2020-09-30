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

import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class Drop {
    public static final float MIN_CHANCE = 0.00001f;
    public static final float MAX_CHANCE = 1.00000f;

    private final Collection<UUID> applicableWorldIds;
    private final boolean          isApplicableOnlyToPlayerKills;
    private final String           permissionNode;
    private final ItemStack        item;
    private final float            chance;

    public Drop(
        final @Nullable Collection<UUID> applicableWorldIds,
        final boolean isApplicableOnlyToPlayerKills,
        final @Nullable String permissionNode,
        final @NotNull ItemStack item,
        final int quantity,
        final float chance
    ) {
        if (quantity < 1 || quantity > item.getMaxStackSize()) {
            throw new IllegalArgumentException(
                "Quantity value must be between 1 and " + item.getMaxStackSize() + ", inclusive."
            );
        }

        if (chance < Drop.MIN_CHANCE || chance > Drop.MAX_CHANCE) {
            throw new IllegalArgumentException(
                "Chance value must be between " + Drop.MIN_CHANCE + " and " + Drop.MAX_CHANCE + ", inclusive."
            );
        }

        this.applicableWorldIds = applicableWorldIds;
        this.isApplicableOnlyToPlayerKills = isApplicableOnlyToPlayerKills;
        this.permissionNode = permissionNode;
        this.item = item.clone();
        this.item.setAmount(quantity);
        this.chance = chance;
    }

    public boolean isApplicableOnlyToPlayerKills() {
        return this.permissionNode != null || this.isApplicableOnlyToPlayerKills;
    }

    public @NotNull ItemStack getItem() {
        return this.item;
    }

    public float getChance() {
        return this.chance;
    }

    public boolean canDropIn(final @NotNull World world) {
        return this.applicableWorldIds == null || this.applicableWorldIds.contains(world.getUID());
    }

    public boolean canDropFor(final @NotNull Permissible permissible) {
        return this.permissionNode == null || permissible.hasPermission(this.permissionNode);
    }
}