# VethEnchant

VethEnchant is a lightweight Paper-native enchantment plugin for Paper/Leaf 26.1.2.

The plugin registers custom enchantments into Paper's real enchantment registry during bootstrap. Runtime behavior is intentionally modular:

- registry definitions live in `EnchantCatalog`
- effect logic implements `VethEnchantEffect`
- Bukkit/Paper events are dispatched by `EffectDispatcher`
- PacketEvents integration is isolated behind `PacketBridge`

Vanilla enchantment config is split per enchantment under `plugins/VethEnchant/vanilla-enchant/<id>.yml`.
Each file can tune two independent layers:

- `display`: MiniMessage name, rarity, and short tooltip description.
- `registry`: Paper bootstrap registry values such as max level, weight, cost, anvil cost, conflicts, and source flags. These need a server restart.

VethEnchant does not override vanilla enchantment runtime formulas. Vanilla mechanics stay owned by Paper/Minecraft; this keeps the plugin lighter and safer for production.

This first skeleton includes a few safe sample enchantments so the framework can compile and be tested before adding the full curated enchantment pool.
