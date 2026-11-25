# Mount Armor Tweaks

Craftable, enchantable and optionally damageable horse and nautilus armor. Runs on the server;
every feature can be switched off. Vanilla clients can join.

## Features

- **Recipes** for copper, iron, golden and diamond horse armor (the leather horse armor shape, 7
  material) and nautilus armor (a ring of 4 material). Unlocked in the recipe book when you pick up
  the material.
- **Enchanting** of all horse and nautilus armor, leather and netherite included: at the enchanting
  table and with an anvil and books. Only enchantments in a per-armor-type whitelist are accepted;
  by default the protection enchantments, plus Feather Falling for horse armor.
- **Durability** (off by default): mount armor wears down when the mount takes damage, like wolf
  armor, and can be repaired with its material on an anvil. Unbreaking and Mending become available
  while it is on.
- **Armorer trades** (off by default): armorers can sell iron and golden mount armor, and enchanted
  diamond horse armor (level 4) or nautilus armor (level 5).

## Configuration

`config/mountarmortweaks/config.json`:

```json
{
  "recipes": {
    "copper_horse_armor": true,
    "iron_horse_armor": true,
    "golden_horse_armor": true,
    "diamond_horse_armor": true,
    "copper_nautilus_armor": true,
    "iron_nautilus_armor": true,
    "golden_nautilus_armor": true,
    "diamond_nautilus_armor": true
  },
  "enchanting": {
    "table": true,
    "anvil": true
  },
  "durability": false,
  "trades": {
    "mode": "off"
  }
}
```

`trades.mode` is `off`, `horse`, `nautilus` or `both`.

`config/mountarmortweaks/enchantments.json` lists the enchantments each armor type accepts.
Entries are enchantment ids or `#tags`, including enchantments from other mods:

```json
{
  "horse_armor": ["minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
                  "minecraft:projectile_protection", "minecraft:feather_falling"],
  "nautilus_armor": ["minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
                     "minecraft:projectile_protection"],
  "with_durability": ["minecraft:unbreaking", "minecraft:mending"]
}
```

Rules the lists cannot override:

- Only enchantments that apply to armor in general (slot `armor` or `any`) are accepted. Sharpness
  or Respiration, for example, are ignored with a warning in the log.
- Enchantments that change durability or repair from XP (Unbreaking, Mending) are only accepted
  while `durability` is on, and only from `with_durability`. If durability is turned off later, they
  stay on the armor but have no effect.

Curse of Binding and Curse of Vanishing work on mounts if you add them.

**When changes apply:** `recipes`, `enchanting.anvil` and `enchantments.json` on `/reload`.
`enchanting.table`, `durability` and `trades.mode` when the server restarts (in singleplayer:
when the world is re-opened). A missing file is created with the defaults; a broken file is
reported in the log and the defaults are used.

## Compatibility

| | |
|---|---|
| Minecraft | 26.3 |
| Mod loader | Fabric (Fabric Loader 0.20.1 or newer) |
| Dependencies | [Fabric API](https://modrinth.com/mod/fabric-api) |
| Side | Server. Optional on clients; install it on the client to use it in singleplayer. |

What players see depends on their client:

- **Vanilla client on a modded server:** everything works, but the client does not know about the
  new item properties. Mount armor shows no durability bar or tooltip (it still wears down and
  breaks), and the enchanting table may not show a preview.
- **Client with the mod:** follows the server's settings, whatever its own config says. On a server
  without the mod it shows vanilla items.

Notes:

- Recipe viewers (JEI, REI, EMI) may not list the recipes on a client, because servers no longer
  send full recipes to clients. The vanilla recipe book works.
- Every added trade takes a share of the vanilla offers at its level (a villager picks 2 offers per
  level). Only villagers that level up after the change are affected.
- With the experimental "Villager Trade Rebalance" data pack the added trades are absent.
- **Lithium**, **Carpet**: not yet tested.

## Installation

Download the jar that matches your Minecraft version and put it in the `mods` folder, together with
Fabric API.

## Building from source

Requires JDK 25.

```
./gradlew build
```

The jars are written to `versions/<minecraft>-<loader>/build/libs/`. Design notes are in
[docs/design.md](docs/design.md).

## License

[MIT](LICENSE)
