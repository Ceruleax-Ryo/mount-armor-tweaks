# Manual test checklist

In-game checks that the sandbox could not run (it has no client). Use a creative test world unless
noted. Config files are in `run/config/mountarmortweaks/` when started from Gradle.

Already verified on a dedicated server (2026-11-26, via RCON): recipes and their load conditions
incl. `/reload`; `/enchant` whitelist and hard rules (body slot, durability group, curses, unknown
ids logged); anvil switch on `/reload`; durability on horse, zombie horse, skeleton horse, nautilus
and zombie nautilus, llama carpet untouched; trades per mode, enchanted trades only with the table
switch on; enchanting-table rolls; broken config falls back to defaults.

## Singleplayer (`./gradlew :26.3-fabric:runClient`)

1. Default config. Pick up an iron ingot: the iron horse/nautilus armor recipes appear in the recipe
   book. Craft one of each shape.
2. Enchanting table with diamond horse armor: offers appear and give only protection types or Feather
   Falling. Nautilus armor never gets Feather Falling.
3. Anvil: Protection IV book on iron horse armor works; Sharpness book is refused; Mending book is
   refused (durability off).
4. Equip enchanted armor on a horse and a nautilus; the armor renders and the protection reduces damage.
5. Set `durability: true`, leave and re-open the world (no game restart). New and existing armor
   shows a durability bar. Let a horse take damage: the bar drops; repair it on an anvil with the
   material.
6. With durability on: Unbreaking and Mending books apply. Hold damaged armor with Mending and pick
   up XP: it repairs.
7. Set `durability: false`, re-open the world. The Mending armor from step 6: no durability bar, XP
   no longer goes into it.
8. Curses: add `minecraft:binding_curse` and `minecraft:vanishing_curse` to `horse_armor`, `/reload`,
   apply both by anvil to armor on a horse. The armor cannot be taken out of the horse's slot and cannot
   be sheared off; kill the horse: the armor does not drop.
9. Set `recipes.iron_horse_armor: false`, `/reload`: the recipe is gone from the book and the grid.
10. `trades.mode: "both"`, re-open the world, level up armorers (or summon them at level 2–5): the new
    offers appear; enchanted diamond armor at level 4 (horse) / 5 (nautilus).

## Multiplayer

11. Dedicated server with the mod and `durability: true`, **vanilla client** (no mod): joins, can
    craft and enchant, armor wears and breaks, but no durability bar. Note what the enchanting table
    preview shows.
12. Same server, **client with the mod** whose own config has `durability: false`: the durability
    bar is shown (server setting wins). Check the log for errors on join.
13. Client with the mod joining a **server without the mod**: mount armor behaves like vanilla
    (not enchantable at the table, no durability bar).
14. Client with the mod: play singleplayer with `durability: true`, then join the server from 13:
    no durability bar there; back in singleplayer it is shown again.

## Compatibility (when builds exist)

15. Lithium and Carpet on the server: repeat 5 (mount armor wears down) and 2.
