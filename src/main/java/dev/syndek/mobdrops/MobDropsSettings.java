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

import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class MobDropsSettings {
    private final MobDropsPlugin                  plugin;
    private final Map<EntityType, Iterable<Drop>> drops = new EnumMap<>(EntityType.class);

    public MobDropsSettings(final MobDropsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();

        // We don't recreate the 'drops' map on each call to load, so ensure it is empty.
        this.drops.clear();
    }

    public Iterable<Drop> getDropsFor(final EntityType entityType) {
        final Iterable<Drop> drops = this.drops.get(entityType);
        return drops == null ? Collections.emptyList() : drops;
    }
}