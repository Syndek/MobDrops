# MobDrops

MobDrops is a Bukkit plugin for providing custom drops when killing mobs.

Users can configure custom drops for any type of mob, limit those drops to certain worlds/players,
as well as configure custom items to use in drops.

Currently, custom item customisation is limited to basic meta customisation (display name, lore, etc.).
In time, I plan to add support for more detailed customisation (e.g. book meta).

## Versioning

MobDrops is built for Java 8 using Gradle.
It should work on pretty much any version of CraftBukkit, Spigot, Paper, etc. that you're running,
but feel free to get in contact if you run into any problems.

Version numbers roughly follow the [Semantic Versioning Guidelines](https://semver.org).

## Commands

Currently, the plugin only registers one command.

`/mdreload` - Reloads the plugin configuration from the disk.

## Permissions

- `mobdrops.*`  
  Provides access to all plugin features.
  - `mobdrops.reload`  
    Provides access to the reload command, `/mdreload`.
    
It should be noted that if you set permission nodes for drops, those nodes will *not* be covered by `mobdrops.*`.
You will need to give those permission nodes out to players using your permissions plugin of choice.