package dev.vethcraft.vethenchant.bootstrap;

import dev.vethcraft.vethenchant.registry.BootstrapEnchantDefinition;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemType;

import java.util.List;
import java.util.Set;

public final class VethEnchantBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        var lifecycle = context.getLifecycleManager();
        BootstrapConfigLoader.LoadedConfig loadedConfig = BootstrapConfigLoader.load(context.getDataDirectory());
        List<BootstrapEnchantDefinition> definitions = loadedConfig.customDefinitions();

        lifecycle.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ITEM).newHandler(event -> {
            for (BootstrapEnchantDefinition definition : definitions) {
                List<TypedKey<ItemType>> supported = definition.target().supportedItems(event.registrar());
                event.registrar().setTag(definition.supportedItemTag(), supported);
                event.registrar().setTag(definition.primaryItemTag(), supported);
            }
        }));

        lifecycle.registerEventHandler(RegistryEvents.ENCHANTMENT.entryAdd().newHandler(event -> {
            BootstrapConfigLoader.RegistryPatch patch = loadedConfig.registryPatches().get(event.key().key());
            if (patch != null) {
                patch.apply(event.builder());
            }
        }));

        lifecycle.registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
            for (BootstrapEnchantDefinition definition : definitions) {
                event.registry().register(EnchantmentKeys.create(definition.key()), builder -> builder
                    .description(Component.translatable(definition.translationKey(), definition.displayName()))
                    .supportedItems(event.getOrCreateTag(definition.supportedItemTag()))
                    .primaryItems(event.getOrCreateTag(definition.primaryItemTag()))
                    .exclusiveWith(RegistrySet.keySet(RegistryKey.ENCHANTMENT, definition.exclusiveWith()))
                    .weight(definition.weight())
                    .maxLevel(definition.maxLevel())
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(definition.minimumCostBase(), definition.minimumCostPerLevel()))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(definition.maximumCostBase(), definition.maximumCostPerLevel()))
                    .anvilCost(definition.anvilCost())
                    .activeSlots(definition.target().activeSlots())
                );
            }
        }));

        lifecycle.registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ENCHANTMENT).newHandler(event -> {
            for (BootstrapEnchantDefinition definition : definitions) {
                TypedKey<Enchantment> key = EnchantmentKeys.create(definition.key());
                Set<TagEntry<Enchantment>> entry = Set.of(TagEntry.valueEntry(key));

                addIf(event.registrar(), EnchantmentTagKeys.IN_ENCHANTING_TABLE, entry, definition.distribution().enchantingTable());
                addIf(event.registrar(), EnchantmentTagKeys.ON_RANDOM_LOOT, entry, definition.distribution().randomLoot());
                addIf(event.registrar(), EnchantmentTagKeys.TRADEABLE, entry, definition.distribution().villagerTrade());
                addIf(event.registrar(), EnchantmentTagKeys.ON_MOB_SPAWN_EQUIPMENT, entry, definition.distribution().mobEquipment());
                addIf(event.registrar(), EnchantmentTagKeys.ON_TRADED_EQUIPMENT, entry, definition.distribution().tradedEquipment());
                addIf(event.registrar(), EnchantmentTagKeys.TREASURE, entry, definition.distribution().treasure());
                addIf(event.registrar(), EnchantmentTagKeys.DOUBLE_TRADE_PRICE, entry, definition.distribution().treasure());
            }
            for (BootstrapConfigLoader.RegistryPatch patch : loadedConfig.registryPatches().values()) {
                TypedKey<Enchantment> key = EnchantmentKeys.create(patch.key());
                Set<TagEntry<Enchantment>> entry = Set.of(TagEntry.valueEntry(key));
                patchSource(event.registrar(), EnchantmentTagKeys.IN_ENCHANTING_TABLE, entry, patch.sourcePatch().enchantingTable());
                patchSource(event.registrar(), EnchantmentTagKeys.ON_RANDOM_LOOT, entry, patch.sourcePatch().loot());
                patchSource(event.registrar(), EnchantmentTagKeys.TRADEABLE, entry, patch.sourcePatch().villager());
                patchSource(event.registrar(), EnchantmentTagKeys.ON_MOB_SPAWN_EQUIPMENT, entry, patch.sourcePatch().mobEquipment());
                patchSource(event.registrar(), EnchantmentTagKeys.ON_TRADED_EQUIPMENT, entry, patch.sourcePatch().tradedEquipment());
                patchSource(event.registrar(), EnchantmentTagKeys.TREASURE, entry, patch.sourcePatch().treasure());
                patchSource(event.registrar(), EnchantmentTagKeys.DOUBLE_TRADE_PRICE, entry, patch.sourcePatch().treasure());
            }
        }));
    }

    private static void addIf(
        io.papermc.paper.tag.PreFlattenTagRegistrar<Enchantment> registrar,
        TagKey<Enchantment> tag,
        Set<TagEntry<Enchantment>> entries,
        boolean condition
    ) {
        if (condition) {
            registrar.addToTag(tag, entries);
        }
    }

    private static void patchSource(
        io.papermc.paper.tag.PreFlattenTagRegistrar<Enchantment> registrar,
        TagKey<Enchantment> tag,
        Set<TagEntry<Enchantment>> entries,
        Boolean value
    ) {
        if (Boolean.TRUE.equals(value)) {
            registrar.addToTag(tag, entries);
        } else if (Boolean.FALSE.equals(value) && registrar.hasTag(tag)) {
            Set<TagEntry<Enchantment>> patched = new java.util.LinkedHashSet<>(registrar.getTag(tag));
            Set<net.kyori.adventure.key.Key> keys = entries.stream()
                .map(TagEntry::key)
                .collect(java.util.stream.Collectors.toSet());
            patched.removeIf(entry -> !entry.isTag() && keys.contains(entry.key()));
            registrar.setTag(tag, patched);
        }
    }
}
