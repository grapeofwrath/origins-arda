# Arda Origins: Tolkien ancestries and classes for Origins-Reborn

This is an add-on for the Origins-Reborn PaperMC plugin. They are based on the works of Tolkien. I have no intention of officially distributing this on any mod platforms, but maybe it'll be useful to you as well. It was built for a server with my friends, and I used Claude to code the addon itself. The read me is pretty much up to date. I changed out some abilities here and there. I'll update when I have time.

## Install

1. Install JDK 25 and Gradle.
2. Copy your server's `plugins/Origins-Reborn*.jar` to `addon/libs/Origins-Reborn.jar`.
3. In `addon/`, run `gradle build`. The jar is in `addon/build/libs/`. Put it in your server's `plugins/` folder.
4. Copy everything in `origins/` into `plugins/Origins-Reborn/origins/`.
5. Delete (or set `"unchoosable": "true"` on) any stock origins you don't want players to see.
6. Restart the server. The new `class` and `size` layers register themselves and appear in `plugins/Origins-Reborn/config.yml`, where you can set its default origin, GUI priority, and whether it is randomised on join / on Orb of Origin use. Layer priority sets the GUI order (higher appears first): give `origin` the highest value, then `class`, then `size`. Set the `size` layer's default origin to `size_normal` if you want it skippable.


## Ancestries (layer `origin`), four powers each

| File | Pathfinder analogue | Powers |
|---|---|---|
| edain | Human | Gift of Men (+25% XP), Hope Unquenched (low-health Resistance/Regen, 90s), extra reach, sprint jump |
| noldor | Elf (High) | Cat vision, Undying Body (no Poison/Hunger), Deep Lore (enchanting costs 1 less level), chime (amethyst heals) |
| sindar | Elf (Woodland) | Silent, fast arrows, Woodland Stride (Speed in forests), slow falling |
| teleri | Elf (Sea) | Aqua affinity, swim speed, water vision, Breath of the Sea (3x air) |
| peredhil | Half-elf | Tailwind, Undying Body, Hope Unquenched, elegy (stronger below 3 hearts) |
| naugrim | Dwarf | Natural armor, Delver's Instinct (Haste + sight in deep tunnels), Forge-hardened (-40% fire damage), fortune increaser |
| hobbit | Halfling | Silent, Second Breakfast (extra food), Fortunate Folk (Luck), creeper ally |
| uruk_hai | Half-orc | Cat vision, Battle-fury (kill gives Speed/Strength), Iron Stomach (no bad food effects), strong arms |
| eagle | Winged ancestry (Aasimar/Tengu-style) | Elytra flight (double-tap jump), rideable by other players, aerial combatant, fall immunity |
| beorning | Shapechanger (Werebear-style) | Moonbound (night Strength; more on gibbous and full moons), kill boost, double health, queen bee |

**Moonbound by moon phase (night only, Overworld):** Strength I on any night. Gibbous moons add Speed. A full moon is Strength II, Speed and Resistance.

## Classes (layer `class`)

| File | Pathfinder class | Powers |
|---|---|---|
| fighter (Warrior) | Fighter | First Strike (+20% opening hit), Cleave (half damage to a neighbouring hostile) |
| ranger | Ranger | Perfect shot, Keen Tracker (arrows add Glowing), Wayfarer (25% less hunger loss) |
| rogue (Burglar) | Rogue | Backstab (+35% from behind), Quiet Steps (faster sneaking) |
| bard (Minstrel) | Bard | Song of Valor (goat horn buffs allies in 16 blocks, 60s), Silver Tongue (cheaper trades) |
| champion (Defender) | Champion | Shield Wall (-15% damage holding a shield, allies -10%), strong skin |
| cleric (Healer) | Cleric | Hands of the Healer (sneak + empty-hand click: Regen II, cures Poison/Wither), flower power |
| wizard (Loremaster) | Wizard | Light of the Staff (blaze rod: knockback + Glowing, ignites undead, 20s), regeneration when still |
| druid (Beastfriend) | Druid | Friend of Beasts (wild beasts don't target you), alpha wolf |
| alchemist (Herbalist) | Alchemist | Potion master, better potions |
| inventor (Smith) | Inventor | Mending Craft (self-repairing gear), Forge-hardened |

## Size (layer `size`)

| File | Powers |
|---|---|
| size_normal | None (regular height) |
| size_small | `fantasyorigins:small_body` (one block tall; needs Origins-Fantasy) |
