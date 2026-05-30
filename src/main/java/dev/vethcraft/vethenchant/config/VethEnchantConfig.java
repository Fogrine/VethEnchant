package dev.vethcraft.vethenchant.config;

import dev.vethcraft.vethenchant.tooltip.RarityDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VethEnchantConfig {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");

    private final JavaPlugin plugin;
    private EnchantDefinitionRegistry enchants = EnchantDefinitionRegistry.empty();
    private Map<String, RarityDefinition> rarities = Map.of();
    private LimitsConfig limits = LimitsConfig.defaults();
    private ConfigHealthReport healthReport = ConfigHealthReport.empty();

    public VethEnchantConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ensureFiles();
        this.plugin.reloadConfig();
        List<String> warnings = new ArrayList<>();
        this.rarities = loadRarities(warnings);
        this.enchants = loadEnchantments(warnings);
        this.limits = LimitsConfig.load(file("limits.yml"));
        this.healthReport = ConfigHealthReport.build(this.enchants, this.rarities, warnings);
        if (this.plugin.getConfig().getBoolean("settings.debug", false) && !this.healthReport.healthy()) {
            this.plugin.getLogger().warning("VethEnchant config loaded with " + this.healthReport.warningCount() + " warning(s). Use /ve warnings for details.");
        }
    }

    public EnchantDefinitionRegistry enchants() {
        return this.enchants;
    }

    public Map<String, RarityDefinition> rarities() {
        return this.rarities;
    }

    public LimitsConfig limits() {
        return this.limits;
    }

    public ConfigHealthReport healthReport() {
        return this.healthReport;
    }

    public boolean isEnchantEnabled(NamespacedKey key) {
        return this.enchants.isEnabled(key);
    }

    public boolean isRareOrAbove(NamespacedKey key) {
        int rarityWeight = rarity(this.enchants.rarity(key)).weight();
        int rareWeight = rarity("rare").weight();
        return rarityWeight <= rareWeight;
    }

    public RarityDefinition rarity(String id) {
        String normalized = normalizeRarity(id);
        return this.rarities.getOrDefault(normalized, this.rarities.get("common"));
    }

    private void ensureFiles() {
        this.plugin.saveDefaultConfig();
        try {
            new DefaultConfigWriter(this.plugin.getDataFolder().toPath()).writeMissingFiles();
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to create default VethEnchant config files.", exception);
        }
    }

    private Map<String, RarityDefinition> loadRarities(List<String> warnings) {
        Map<String, RarityDefinition> result = defaultRarities();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file("rarity.yml"));
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                warnings.add("rarity.yml 中的 " + id + " 不是有效配置段，已跳过。");
                continue;
            }
            String normalized = normalizeRarity(id);
            RarityDefinition fallback = result.getOrDefault(normalized, result.get("common"));
            String color = color(section.getString("color", null), fallback);
            String tooltipStyleRaw = section.getString("tooltip-style", keyString(fallback.tooltipStyle()));
            NamespacedKey tooltipStyle = parseKey(tooltipStyleRaw);
            if (tooltipStyleRaw != null && !tooltipStyleRaw.isBlank() && tooltipStyle == null) {
                warnings.add("rarity.yml 中 " + id + " 的 tooltip-style 无效：" + tooltipStyleRaw);
            }
            result.put(normalized, new RarityDefinition(
                normalized,
                section.getString("name", section.getString("display", fallback.displayName())),
                section.getInt("weight", fallback.weight()),
                tooltipStyle,
                section.getString("enchant-format", "<color:" + color + ">{name}</color> <text>{level}</text>"),
                section.getString("description-format", "<muted>  {description}</muted>")
            ));
        }
        return Map.copyOf(result);
    }

    private EnchantDefinitionRegistry loadEnchantments(List<String> warnings) {
        Map<NamespacedKey, EnchantDefinition> result = new LinkedHashMap<>();
        loadEnchantFolder(result, "vanilla-enchant", "minecraft", "vanilla", warnings);
        loadEnchantFolder(result, "custom-enchant", "vethenchant", "custom", warnings);
        return new EnchantDefinitionRegistry(result);
    }

    private void loadEnchantFolder(Map<NamespacedKey, EnchantDefinition> result, String folderName, String defaultNamespace, String defaultType, List<String> warnings) {
        Path folder = this.plugin.getDataFolder().toPath().resolve(folderName);
        if (!Files.isDirectory(folder)) {
            warnings.add("附魔配置文件夹不存在：" + folderName);
            return;
        }
        File[] files = folder.toFile().listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) {
            warnings.add("无法读取附魔配置文件夹：" + folderName);
            return;
        }
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            NamespacedKey key = parseEnchantKey(config, file, defaultNamespace);
            if (key == null) {
                warnings.add("附魔配置 ID 无效，已跳过：" + folderName + "/" + file.getName());
                continue;
            }
            ConfigurationSection display = config.getConfigurationSection("display");
            ConfigurationSection limits = config.getConfigurationSection("limits");
            String fileName = file.getName().replaceFirst("\\.ya?ml$", "");
            String name = string(display, "name", config.getString("basic.name", fallbackName(fileName)));
            String rarity = normalizeRarity(string(display, "rarity", config.getString("rarity", "common")));
            if (!this.rarities.containsKey(rarity)) {
                warnings.add("附魔 " + key.asString() + " 使用了未定义稀有度：" + rarity);
            }
            List<String> description = display == null ? List.of() : display.getStringList("description");
            if (description.isEmpty()) {
                String general = config.getString("display.description.general", "");
                description = general.isBlank() ? List.of("<primary>暂无详细说明。</primary>") : List.of("<primary>" + general + "</primary>");
            }
            List<String> icons = list(display, "icons");
            if (result.containsKey(key)) {
                warnings.add("附魔 " + key.asString() + " 被多个文件定义，后加载的配置会覆盖前一个。");
            }
            result.put(key, new EnchantDefinition(
                key,
                config.getString("type", defaultType).toLowerCase(Locale.ROOT),
                config.getBoolean("enabled", config.getBoolean("basic.enable", true)),
                name,
                rarity,
                List.copyOf(description),
                List.copyOf(icons),
                limits == null ? List.of() : limits.getStringList("groups").stream()
                    .map(group -> group.toLowerCase(Locale.ROOT))
                    .toList(),
                limits == null ? 0 : limits.getInt("priority", 0)
            ));
        }
    }

    private NamespacedKey parseEnchantKey(FileConfiguration config, File file, String defaultNamespace) {
        String raw = config.getString("id", config.getString("basic.id", file.getName().replaceFirst("\\.ya?ml$", "")));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.toLowerCase(Locale.ROOT).trim();
        if (!value.contains(":")) {
            value = defaultNamespace + ":" + value;
        }
        return NamespacedKey.fromString(value);
    }

    private Map<String, RarityDefinition> defaultRarities() {
        Map<String, RarityDefinition> result = new LinkedHashMap<>();
        addDefaultRarity(result, "common", "普通", "#f8f4ed", 1000);
        addDefaultRarity(result, "uncommon", "罕见", "#66c18c", 500);
        addDefaultRarity(result, "rare", "精良", "#63bbd0", 250);
        addDefaultRarity(result, "epic", "史诗", "#eb507e", 50);
        addDefaultRarity(result, "legendary", "传奇", "#fba414", 10);
        addDefaultRarity(result, "splendid", "稀世", "#fbda41", 5);
        addDefaultRarity(result, "curse", "诅咒", "#d42517", 25);
        addDefaultRarity(result, "artifact", "皮肤", "#ec9bad", 1);
        return result;
    }

    private void addDefaultRarity(Map<String, RarityDefinition> rarities, String id, String name, String color, int weight) {
        rarities.put(id, new RarityDefinition(
            id,
            name,
            weight,
            parseKey("server:tooltip/" + id),
            "<color:" + color + ">{name}</color> <text>{level}</text>",
            "<muted>  {description}</muted>"
        ));
    }

    private File file(String name) {
        return new File(this.plugin.getDataFolder(), name);
    }

    private static String string(ConfigurationSection section, String path, String fallback) {
        return section == null ? fallback : section.getString(path, fallback);
    }

    private static List<String> list(ConfigurationSection section, String path) {
        if (section == null || !section.contains(path)) {
            return List.of();
        }
        if (section.isList(path)) {
            return section.getStringList(path);
        }
        String value = section.getString(path, "");
        return value.isBlank() ? List.of() : List.of(value);
    }

    private static NamespacedKey parseKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
    }

    private static String keyString(NamespacedKey key) {
        return key == null ? "" : key.asString();
    }

    private static String color(String raw, RarityDefinition fallback) {
        if (raw == null || raw.isBlank()) {
            String format = fallback.enchantFormat();
            Matcher matcher = HEX_PATTERN.matcher(format);
            return matcher.find() ? matcher.group() : "#ffffff";
        }
        Matcher matcher = HEX_PATTERN.matcher(raw);
        return matcher.find() ? matcher.group() : raw;
    }

    public static String normalizeRarity(String id) {
        if (id == null || id.isBlank()) {
            return "common";
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "普通", "comom" -> "common";
            case "罕见" -> "uncommon";
            case "精良" -> "rare";
            case "史诗" -> "epic";
            case "传奇", "ledenry" -> "legendary";
            case "稀世" -> "splendid";
            case "诅咒" -> "curse";
            case "皮肤" -> "artifact";
            default -> id.toLowerCase(Locale.ROOT);
        };
    }

    private static String fallbackName(String key) {
        return key.replace('_', ' ');
    }
}
