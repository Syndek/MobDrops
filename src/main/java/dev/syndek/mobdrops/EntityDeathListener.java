/*
 * Copyright (C) 2021 Louis Salkeld
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

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class EntityDeathListener implements Listener {
    private final MobDropsPlugin plugin;

    public EntityDeathListener(final @NotNull MobDropsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onEntityDeath(final @NotNull EntityDeathEvent event) {
        this.handleDrops(event, this.plugin.getSettings().getGlobalDrops());
        this.handleDrops(event, this.plugin.getSettings().getDropsFor(event.getEntity().getType()));
    }

    private void handleDrops(final @NotNull EntityDeathEvent event, final @NotNull Iterable<Drop> drops) {
        for (final Drop drop : drops) {
            // Don't drop if the current world is not applicable for the drop.
            if (!drop.canDropIn(event.getEntity().getWorld())) {
                return;
            }

            final Player killer = event.getEntity().getKiller();

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

            // Only drop if random <= drop.getChance().
            if (random > drop.getChance()) {
                return;
            }

            event.getDrops().add(drop.getItem());
        }
    }
}
