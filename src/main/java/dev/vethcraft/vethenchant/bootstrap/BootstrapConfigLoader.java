package dev.vethcraft.vethenchant.bootstrap;

import dev.vethcraft.vethenchant.registry.BootstrapEnchantDefinition;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import dev.vethcraft.vethenchant.registry.EnchantDistribution;
import dev.vethcraft.vethenchant.registry.TargetGroup;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BootstrapConfigLoader {

    private BootstrapConfigLoader() {
    }

    public static LoadedConfig load(Path dataDirectory) {
        return new LoadedConfig(loadCustomDefinitions(dataDirectory), loadRegistryPatches(dataDirectory));
    }

    private static List<BootstrapEnchantDefinition> loadCustomDefinitions(Path dataDirectory) {
        Path folder = dataDirectory.resolve("custom-enchant");
        if (!Files.isDirectory(folder)) {
            return EnchantCatalog.definitions();
        }

        Map<String, BootstrapEnchantDefinition> definitions = new LinkedHashMap<>();
        for (BootstrapEnchantDefinition definition : EnchantCatalog.definitions()) {
            definitions.put(definition.id(), definition);
        }

        File[] files = folder.toFile().listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) {
            return List.copyOf(definitions.values());
        }

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.getBoolean("enabled", true)) {
                continue;
            }
            BootstrapEnchantDefinition fallback = definitions.get(file.getName().replaceFirst("\\.ya?ml$", ""));
            BootstrapEnchantDefinition parsed = parse(config, file, fallback);
            if (parsed != null) {
                definitions.put(parsed.id(), parsed);
            }
        }
        return List.copyOf(definitions.values());
    }

    private static BootstrapEnchantDefinition parse(FileConfiguration config, File file, BootstrapEnchantDefinition fallback) {
        String rawId = config.getString("id", fallback == null ? file.getName().replaceFirst("\\.ya?ml$", "") : fallback.key().asString());
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        Key key = Key.key(rawId.contains(":") ? rawId : EnchantCatalog.NAMESPACE + ":" + rawId);
        if (!EnchantCatalog.NAMESPACE.equals(key.namespace())) {
            return null;
        }
        String id = key.value();
        ConfigurationSection registry = config.getConfigurationSection("registry");
        TargetGroup target = parseTarget(config, fallback == null ? TargetGroup.ALL_DURABLE : fallback.target());
        return new BootstrapEnchantDefinition(
            id,
            key,
            config.getString("display.name", fallback == null ? id : fallback.displayName()),
            target,
            intValue(registry, "max-level", fallback == null ? 1 : fallback.maxLevel()),
            intValue(registry, "weight", fallback == null ? 1 : fallback.weight()),
            intValue(registry, "min-cost.base", fallback == null ? 1 : fallback.minimumCostBase()),
            intValue(registry, "min-cost.per-level", fallback == null ? 0 : fallback.minimumCostPerLevel()),
            intValue(registry, "max-cost.base", fallback == null ? 30 : fallback.maximumCostBase()),
            intValue(registry, "max-cost.per-level", fallback == null ? 0 : fallback.maximumCostPerLevel()),
            intValue(registry, "anvil-cost", fallback == null ? 1 : fallback.anvilCost()),
            distribution(config.getConfigurationSection("sources"), fallback == null ? EnchantDistribution.common() : fallback.distribution()),
            parseExclusiveWith(config.getStringList("conflicts"))
        );
    }

    private static Map<Key, RegistryPatch> loadRegistryPatches(Path dataDirectory) {
        Map<Key, RegistryPatch> patches = new LinkedHashMap<>();
        loadPatchFolder(patches, dataDirectory.resolve("vanilla-enchant"), "minecraft");
        loadPatchFolder(patches, dataDirectory.resolve("custom-enchant"), EnchantCatalog.NAMESPACE);
        return Map.copyOf(patches);
    }

    private static void loadPatchFolder(Map<Key, RegistryPatch> patches, Path folder, String defaultNamespace) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        File[] files = folder.toFile().listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String rawId = config.getString("id", file.getName().replaceFirst("\\.ya?ml$", ""));
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            Key key = Key.key(rawId.contains(":") ? rawId : defaultNamespace + ":" + rawId);
            RegistryPatch patch = RegistryPatch.parse(key, config);
            if (patch.hasChanges()) {
                patches.put(key, patch);
            }
        }
    }

    private static TargetGroup parseTarget(FileConfiguration config, TargetGroup fallback) {
        List<String> targets = config.getStringList("targets");
        if (targets.isEmpty()) {
            return fallback;
        }
        String first = targets.getFirst().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (first) {
            case "SWORD", "SWORDS" -> TargetGroup.SWORDS;
            case "AXE", "AXES" -> TargetGroup.AXES;
            case "MELEE" -> TargetGroup.MELEE;
            case "BOW", "BOWS" -> TargetGroup.BOWS;
            case "CROSSBOW", "CROSSBOWS" -> TargetGroup.CROSSBOWS;
            case "RANGED" -> TargetGroup.RANGED;
            case "TRIDENT", "TRIDENTS" -> TargetGroup.TRIDENTS;
            case "PICKAXE", "PICKAXES" -> TargetGroup.PICKAXES;
            case "SHOVEL", "SHOVELS" -> TargetGroup.SHOVELS;
            case "HOE", "HOES" -> TargetGroup.HOES;
            case "TOOL", "TOOLS" -> TargetGroup.TOOLS;
            case "TOOLS_AND_WEAPONS" -> TargetGroup.TOOLS_AND_WEAPONS;
            case "HELMET", "HELMETS" -> TargetGroup.HELMETS;
            case "CHESTPLATE", "CHESTPLATES" -> TargetGroup.CHESTPLATES;
            case "LEGGINGS" -> TargetGroup.LEGGINGS;
            case "BOOT", "BOOTS" -> TargetGroup.BOOTS;
            case "ARMOR" -> TargetGroup.ARMOR;
            case "FISHING_ROD", "FISHING_RODS" -> TargetGroup.FISHING_RODS;
            case "SHIELD", "SHIELDS" -> TargetGroup.SHIELDS;
            case "ELYTRA" -> TargetGroup.ELYTRA;
            default -> fallback;
        };
    }

    private static EnchantDistribution distribution(ConfigurationSection sources, EnchantDistribution fallback) {
        if (sources == null) {
            return fallback;
        }
        return new EnchantDistribution(
            sources.getBoolean("enchanting-table", fallback.enchantingTable()),
            sources.getBoolean("loot", fallback.randomLoot()),
            sources.getBoolean("villager", fallback.villagerTrade()),
            sources.getBoolean("mob-equipment", fallback.mobEquipment()),
            sources.getBoolean("traded-equipment", fallback.tradedEquipment()),
            sources.getBoolean("treasure", fallback.treasure())
        );
    }

    private static List<TypedKey<Enchantment>> parseExclusiveWith(List<String> conflicts) {
        List<TypedKey<Enchantment>> result = new ArrayList<>();
        for (String conflict : conflicts) {
            if (conflict == null || conflict.isBlank()) {
                continue;
            }
            String value = conflict.contains(":") ? conflict : "minecraft:" + conflict;
            result.add(EnchantmentKeys.create(Key.key(value)));
        }
        return List.copyOf(result);
    }

    private static int intValue(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    public record LoadedConfig(
        List<BootstrapEnchantDefinition> customDefinitions,
        Map<Key, RegistryPatch> registryPatches
    ) {
    }

    public record RegistryPatch(
        Key key,
        String displayName,
        Integer maxLevel,
        Integer weight,
        Integer minCostBase,
        Integer minCostPerLevel,
        Integer maxCostBase,
        Integer maxCostPerLevel,
        Integer anvilCost,
        List<TypedKey<Enchantment>> exclusiveWith,
        SourcePatch sourcePatch
    ) {
        static RegistryPatch parse(Key key, FileConfiguration config) {
            ConfigurationSection registry = config.getConfigurationSection("registry");
            List<String> conflicts = config.getStringList("conflicts");
            return new RegistryPatch(
                key,
                config.getString("display.name", null),
                integer(registry, "max-level"),
                integer(registry, "weight"),
                integer(registry, "min-cost.base"),
                integer(registry, "min-cost.per-level"),
                integer(registry, "max-cost.base"),
                integer(registry, "max-cost.per-level"),
                integer(registry, "anvil-cost"),
                conflicts.isEmpty() ? null : parseExclusiveWith(conflicts),
                SourcePatch.parse(config.getConfigurationSection("sources"))
            );
        }

        boolean hasChanges() {
            return this.displayName != null
                || this.maxLevel != null
                || this.weight != null
                || this.minCostBase != null
                || this.minCostPerLevel != null
                || this.maxCostBase != null
                || this.maxCostPerLevel != null
                || this.anvilCost != null
                || this.exclusiveWith != null
                || this.sourcePatch.hasChanges();
        }

        public void apply(EnchantmentRegistryEntry.Builder builder) {
            if (this.displayName != null) {
                builder.description(Component.text(this.displayName));
            }
            if (this.maxLevel != null) {
                builder.maxLevel(clamp(this.maxLevel, 1, 255));
            }
            if (this.weight != null) {
                builder.weight(clamp(this.weight, 1, 1024));
            }
            if (this.minCostBase != null || this.minCostPerLevel != null) {
                builder.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                    this.minCostBase == null ? builder.minimumCost().baseCost() : this.minCostBase,
                    this.minCostPerLevel == null ? builder.minimumCost().additionalPerLevelCost() : this.minCostPerLevel
                ));
            }
            if (this.maxCostBase != null || this.maxCostPerLevel != null) {
                builder.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                    this.maxCostBase == null ? builder.maximumCost().baseCost() : this.maxCostBase,
                    this.maxCostPerLevel == null ? builder.maximumCost().additionalPerLevelCost() : this.maxCostPerLevel
                ));
            }
            if (this.anvilCost != null) {
                builder.anvilCost(Math.max(0, this.anvilCost));
            }
            if (this.exclusiveWith != null) {
                builder.exclusiveWith(RegistrySet.keySet(RegistryKey.ENCHANTMENT, this.exclusiveWith));
            }
        }

        private static Integer integer(ConfigurationSection section, String path) {
            if (section == null || !section.contains(path)) {
                return null;
            }
            return section.getInt(path);
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    public record SourcePatch(
        Boolean enchantingTable,
        Boolean loot,
        Boolean villager,
        Boolean mobEquipment,
        Boolean tradedEquipment,
        Boolean treasure
    ) {
        static SourcePatch parse(ConfigurationSection section) {
            if (section == null) {
                return empty();
            }
            return new SourcePatch(
                bool(section, "enchanting-table"),
                bool(section, "loot"),
                bool(section, "villager"),
                bool(section, "mob-equipment"),
                bool(section, "traded-equipment"),
                bool(section, "treasure")
            );
        }

        static SourcePatch empty() {
            return new SourcePatch(null, null, null, null, null, null);
        }

        boolean hasChanges() {
            return this.enchantingTable != null
                || this.loot != null
                || this.villager != null
                || this.mobEquipment != null
                || this.tradedEquipment != null
                || this.treasure != null;
        }

        private static Boolean bool(ConfigurationSection section, String path) {
            return section.contains(path) ? section.getBoolean(path) : null;
        }
    }
}
