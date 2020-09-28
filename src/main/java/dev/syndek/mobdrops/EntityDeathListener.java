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

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class EntityDeathListener implements Listener {
    private final MobDropsPlugin plugin;

    public EntityDeathListener(final MobDropsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onEntityDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        for (final Drop drop : this.plugin.getSettings().getDropsFor(entity.getType())) {
            // Don't drop if the current world is not applicable for the drop.
            if (!drop.canDropIn(entity.getWorld())) {
                return;
            }

            final Player killer = entity.getKiller();

            // Don't drop if the entity was not killed by a player and the drop is only applicable for player kills.
            if (killer == null && drop.isApplicableOnlyToPlayerKills()) {
                return;
            }

            // Don't drop if the entity was killed by a player who doesn't have permission for the drop.
            if (killer != null && !drop.canDropFor(killer)) {
                return;
            }

            final float random =
                Drop.MIN_CHANCE +
                    ThreadLocalRandom.current().nextFloat() *
                        (Drop.MAX_CHANCE - Drop.MIN_CHANCE);

            // Only drop if drop.getChance() <= random.
            if (drop.getChance() > random) {
                return;
            }

            event.getDrops().add(drop.getItem());
        }
    }
}