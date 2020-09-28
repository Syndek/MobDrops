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
        final Collection<UUID> applicableWorldIds,
        final boolean isApplicableOnlyToPlayerKills,
        final String permissionNode,
        final ItemStack item,
        final int quantity,
        final float chance
    ) {
        this.applicableWorldIds = applicableWorldIds;
        this.isApplicableOnlyToPlayerKills = isApplicableOnlyToPlayerKills;
        this.permissionNode = permissionNode;
        this.item = item.clone();
        this.item.setAmount(quantity);
        this.chance = chance;
    }

    public boolean isApplicableOnlyToPlayerKills() {
        return this.isApplicableOnlyToPlayerKills;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public float getChance() {
        return this.chance;
    }

    public boolean canDropIn(final World world) {
        return this.applicableWorldIds == null || this.applicableWorldIds.contains(world.getUID());
    }

    public boolean canDropFor(final Permissible permissible) {
        return this.permissionNode == null || permissible.hasPermission(this.permissionNode);
    }
}