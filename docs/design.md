# Design

Design notes for Mount Armor Tweaks: what each feature does, why it was built that way, and the
Minecraft 26.3 facts each choice depends on. User-facing behaviour is in the [README](../README.md).

## Scope

Horse armor (leather, copper, iron, golden, diamond, netherite) and nautilus armor (copper, iron,
golden, diamond, netherite), 11 items in total. The mod runs on the server; the client part is
optional and only keeps a modded client's view of item components in line with the server.

Fabric API modules used: resource-loader-v1 (jar `data/`), resource-conditions-api-v1, item-api-v1
(`EnchantmentEvents.ALLOW_ENCHANTING`, `DefaultItemComponentEvents.MODIFY`), lifecycle-events-v1,
networking-api-v1.

## 1. Recipes

- Horse armor (copper, iron, golden, diamond): shaped `X X / XXX / X X`, 7 material, the vanilla
  leather horse armor shape.
- Nautilus armor (copper, iron, golden, diamond): shaped ` X / X X / X `, 4 material. The only
  vanilla recipe with this shape is the decorated pot (bricks); copper grate uses a different shape.
- Netherite: vanilla smithing recipes already exist. Nothing added.
- Each recipe is `mountarmortweaks:<item>` with a recipe-book unlock advancement
  (`inventory_changed` on the material). Both carry `fabric:load_conditions` with
  `mountarmortweaks:config` / `feature: recipe/<item>`, so a disabled recipe never enters the
  registry. Follows `/reload`.

## 2. Enchanting

Applies to all 11 items.

- **Enchanting table** (`enchanting.table`): adds the `enchantable` default component with the
  vanilla armor material's enchantability (leather 15, copper 8, iron 9, gold 25, diamond 10,
  netherite 15). The table only rolls `#in_enchanting_table`, so curses and Mending never come from it.
- **Anvil** (`enchanting.anvil`): `ALLOW_ENCHANTING` with `EnchantingContext.ACCEPTABLE` covers the
  anvil, `/enchant` and `enchant_randomly`. `PRIMARY` covers the table and `enchant_with_levels`.
- The handler returns `TRUE` for permitted enchantments, otherwise `DEFAULT`, never `FALSE`, so other
  mods keep the final say.
- Vanilla rules still apply: exclusive sets (one protection type) and maximum levels.
- Whitelists per armor type in `enchantments.json`; entries are ids or `#tags`.

Hard rules in code, which the whitelist cannot override:

1. The enchantment must satisfy `Enchantment.matchingSlot(EquipmentSlot.BODY)`, i.e. its slot group
   is `armor` or `any`. Works for modded enchantments too. Violating entries are logged.
2. Enchantments with `minecraft:item_damage` or `minecraft:repair_with_xp` effects (Unbreaking,
   Mending and modded equivalents) are allowed only while `durability` is on, and only through the
   `with_durability` list.
3. If durability is turned off later, such enchantments stay on the item but do nothing: without
   `max_damage` the item is not damageable, so there is nothing to protect or repair (verified:
   `isDamageableItem` is false and Mending's `getRandomItemWith(..., ItemStack::isDamaged)` skips it).

Curses work on mounts when whitelisted: Binding blocks taking the armor out of the mount's slot,
Vanishing is checked by `Mob.dropCustomDeathLoot` for the body slot.

## 3. Durability (default off)

- Default components: `max_damage` = material durability × 16 (wolf armor formula; leather 80,
  copper 176, iron 240, gold 112, diamond 528, netherite 592), `damage` 0, `repairable` = the
  material's repair tag, and `equippable` rebuilt with `damage_on_hurt = true`.
- The only mixin: `@Inject(HEAD)` into `LivingEntity.hurtArmor(DamageSource, float)`, whose vanilla
  body is empty. `Horse` and `Wolf` already override it with `doHurtEquipment(source, damage, BODY)`;
  zombie horses, skeleton horses, nautili and zombie nautili do not. The inject calls the same method
  for `AbstractHorse` (except `Horse`) and `AbstractNautilus`. Injecting rather than adding overrides
  keeps other mods' injects working. Llama carpets have no `max_damage`, so `doHurtEquipment` skips them.

## 4. Armorer trades (default off)

`trades.mode`: `off`, `horse`, `nautilus` or `both`.

| Level | Horse | Nautilus |
|---|---|---|
| 2 | iron horse armor, 8 emeralds, 12 uses | iron nautilus armor, 8 emeralds, 12 uses |
| 3 | golden horse armor, 10 emeralds, 12 uses | golden nautilus armor, 10 emeralds, 12 uses |
| 4 | enchanted diamond horse armor, 14 emeralds, 3 uses | |
| 5 | | enchanted diamond nautilus armor, 14 emeralds, 3 uses |

- Data-driven: `villager_trade` entries appended to `#minecraft:armorer/level_N` with
  `"required": false`. Two offers are drawn per level, so every addition dilutes the vanilla offers.
- Enchanted trades copy the vanilla diamond-armor trades (`enchant_with_levels` 5–19 from
  `#on_traded_equipment`, then `filtered` + `discard` if nothing was applied). They need the
  `enchantable` component, so they also carry the `enchanting_table` condition and are absent while
  the table switch is off.
- `villager_trade` is a world registry, loaded once per server start; `/reload` does not change it.
- The experimental `trade_rebalance` pack replaces the armorer tags (`replace: true`), so these
  trades are absent there.

## 5. Config

Files in `config/mountarmortweaks/`, written with defaults when missing. A parse error or a wrong
type is logged and replaced by the default; the server never fails to start because of the config.

| Setting | Applies |
|---|---|
| `recipes.*`, `enchanting.anvil`, `enchantments.json` | on `/reload` |
| `enchanting.table`, `durability`, `trades.mode` | when the world is (re)started |

Default item components are bound once per data load and copied into every new stack, and the
trade registry is world-level. Keeping these three settings fixed while a world runs keeps all stacks
and trades consistent. They are re-read when the server stops, so in singleplayer re-opening the
world is enough.

## Client sync

Item components are not part of the network protocol: a client connected to a remote server
rebuilds them from its own code (`RegistryDataCollector.updateComponents`). So:

- Vanilla client: sees vanilla components. No durability bar or tooltip; the armor still wears and
  breaks on the server. The enchanting table preview may not show offers.
- Client with the mod: the server sends `mountarmortweaks:components` (table, durability) during
  the configuration phase, before the client rebuilds components, and the client uses those values
  instead of its local config. A server without the mod sends nothing and the client uses vanilla
  components.
- Singleplayer: the integrated server and the client share the same item objects (memory
  connection, components are not rebuilt), so the local config applies to both.

## Known limitations and open questions

- JEI/REI/EMI may not show the recipes on clients: the server stops syncing full recipes since
  1.21.2. The vanilla recipe book works.
- Not yet tested with Lithium or Carpet (both touch damage handling).
- Other recipe data packs: different ids coexist; the same shape means one recipe wins, with the
  same output.
