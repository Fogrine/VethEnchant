package dev.vethcraft.vethenchant.util;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.customcrops.api.BukkitCustomCropsAPI;
import net.momirealms.customcrops.api.CustomCropsAPI;
import net.momirealms.customcrops.api.core.block.BreakReason;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CustomCropService {

    private static final Pattern STAGE_PATTERN = Pattern.compile("^(.+)_stage_(\\d+)$");

    private final VethEnchantPlugin plugin;
    private boolean enabled;
    private Path cropFolder;
    private Map<String, CustomCropData> cropsById = Map.of();
    private Map<String, CustomCropData> cropsBySeedId = Map.of();
    private Map<String, CustomCropData> cropsByStageId = Map.of();
    private final ThreadLocal<Integer> suppressedFarmDepth = ThreadLocal.withInitial(() -> 0);

    public CustomCropService(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.enabled = this.plugin.getConfig().getBoolean("custom-crops.enabled", true);
        String path = this.plugin.getConfig().getString("custom-crops.craftengine-crops-path", "");
        this.cropFolder = path == null || path.isBlank() ? null : Path.of(path);
        load();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Optional<CustomCropData> find(Block block) {
        if (!this.enabled || block == null) {
            return Optional.empty();
        }
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null) {
            return Optional.empty();
        }
        String stageId = normalizeKey(state.owner().value().id().toString());
        return Optional.ofNullable(this.cropsByStageId.get(stageId));
    }

    public boolean isCustomCropBlock(Block block) {
        return find(block).isPresent();
    }

    public boolean isMature(Block block) {
        if (!this.enabled || block == null) {
            return false;
        }
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null) {
            return false;
        }
        String stageId = normalizeKey(state.owner().value().id().toString());
        CustomCropData data = this.cropsByStageId.get(stageId);
        return data != null && data.isMatureStage(stageId);
    }

    public Optional<CustomCropData> findBySeedItem(ItemStack itemStack) {
        if (!this.enabled || itemStack == null || itemStack.getType().isAir()) {
            return Optional.empty();
        }
        Key itemId = CraftEngineItems.getCustomItemId(itemStack);
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.cropsBySeedId.get(normalizeKey(itemId.toString())));
    }

    public boolean consumeSeed(Player player, CustomCropData crop) {
        if (player == null || crop == null) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            Key itemId = CraftEngineItems.getCustomItemId(stack);
            if (itemId == null || !crop.seedId().equals(normalizeKey(itemId.toString()))) {
                continue;
            }
            if (stack.getAmount() <= 1) {
                player.getInventory().clear(slot);
            } else {
                stack.setAmount(stack.getAmount() - 1);
                player.getInventory().setItem(slot, stack);
            }
            return true;
        }
        return false;
    }

    public boolean replant(Location location, CustomCropData crop) {
        if (location == null || crop == null || location.getWorld() == null) {
            return false;
        }
        if (!this.enabled) {
            return false;
        }
        CustomCropsAPI api = BukkitCustomCropsAPI.get();
        if (api != null && api.placeCrop(location, crop.cropId(), 0)) {
            return true;
        }
        return CraftEngineBlocks.place(location, Key.of(crop.firstStageId()), false);
    }

    public boolean addGrowthPoint(Location location, int point) {
        if (!this.enabled || location == null || location.getWorld() == null || point <= 0) {
            return false;
        }
        CustomCropsAPI api = BukkitCustomCropsAPI.get();
        if (api == null) {
            return false;
        }
        api.addPointToCrop(location, point);
        return true;
    }

    public boolean breakWithoutFarmDispatch(Player player, Block block) {
        if (player == null || block == null || !this.enabled) {
            return false;
        }
        CustomCropData before = find(block).orElse(null);
        if (before == null) {
            return false;
        }
        CustomCropsAPI api = BukkitCustomCropsAPI.get();
        if (api == null) {
            return false;
        }

        this.suppressedFarmDepth.set(this.suppressedFarmDepth.get() + 1);
        try {
            api.simulatePlayerBreakCrop(player, EquipmentSlot.HAND, block.getLocation(), BreakReason.CUSTOM);
        } finally {
            int nextDepth = Math.max(0, this.suppressedFarmDepth.get() - 1);
            if (nextDepth == 0) {
                this.suppressedFarmDepth.remove();
            } else {
                this.suppressedFarmDepth.set(nextDepth);
            }
        }
        return find(block).map(after -> !after.equals(before)).orElse(true);
    }

    public boolean isFarmDispatchSuppressed() {
        return this.suppressedFarmDepth.get() > 0;
    }

    public Optional<CustomCropData> cropById(String cropId) {
        if (cropId == null || cropId.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeKey(cropId);
        CustomCropData byId = this.cropsById.get(normalized);
        if (byId != null) {
            return Optional.of(byId);
        }
        String shortId = shortId(normalized);
        byId = this.cropsById.get(shortId);
        if (byId != null) {
            return Optional.of(byId);
        }
        return Optional.ofNullable(this.cropsById.get(shortId.replace("_", "")));
    }

    public Optional<CustomCropData> cropByStageId(String stageId) {
        if (stageId == null || stageId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.cropsByStageId.get(normalizeKey(stageId)));
    }

    private void load() {
        Map<String, CustomCropData> byId = new LinkedHashMap<>();
        Map<String, CustomCropData> bySeedId = new HashMap<>();
        Map<String, CustomCropData> byStageId = new HashMap<>();

        if (!this.enabled || this.cropFolder == null || !Files.isDirectory(this.cropFolder)) {
            this.cropsById = Map.copyOf(byId);
            this.cropsBySeedId = Map.copyOf(bySeedId);
            this.cropsByStageId = Map.copyOf(byStageId);
            return;
        }

        File[] files = this.cropFolder.toFile().listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null || files.length == 0) {
            this.cropsById = Map.copyOf(byId);
            this.cropsBySeedId = Map.copyOf(bySeedId);
            this.cropsByStageId = Map.copyOf(byStageId);
            return;
        }

        List<String> warnings = new ArrayList<>();
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String cropId = normalizeKey(file.getName().replaceFirst("\\.ya?ml$", ""));
            ConfigurationSection blocks = config.getConfigurationSection("blocks");
            if (blocks == null || blocks.getKeys(false).isEmpty()) {
                warnings.add("作物文件缺少 blocks 段：" + file.getName());
                continue;
            }

            List<StageEntry> stages = new ArrayList<>();
            for (String blockId : blocks.getKeys(false)) {
                String normalizedBlockId = normalizeKey(blockId);
                Matcher matcher = STAGE_PATTERN.matcher(normalizedBlockId);
                if (!matcher.matches()) {
                    continue;
                }
                int stage = parseInt(matcher.group(2), -1);
                if (stage < 0) {
                    continue;
                }
                stages.add(new StageEntry(normalizedBlockId, stage));
            }

            if (stages.isEmpty()) {
                warnings.add("作物文件没有找到数字阶段：" + file.getName());
                continue;
            }

            stages.sort((left, right) -> Integer.compare(left.stage(), right.stage()));
            String firstStageId = stages.getFirst().id();
            String matureStageId = stages.getLast().id();
            int firstStage = stages.getFirst().stage();
            int matureStage = stages.getLast().stage();

            String seedId = seedId(blocks, config, firstStageId, cropId);
            if (seedId == null || seedId.isBlank()) {
                warnings.add("作物文件无法找到种子物品：" + file.getName());
                continue;
            }

            String shortCropId = shortId(cropId);
            CustomCropData data = new CustomCropData(cropId, shortCropId, seedId, firstStageId, matureStageId, firstStage, matureStage);
            byId.put(cropId, data);
            byId.put(shortCropId, data);
            bySeedId.put(seedId, data);
            for (StageEntry stage : stages) {
                byStageId.put(stage.id(), data);
            }
        }

        this.cropsById = Map.copyOf(byId);
        this.cropsBySeedId = Map.copyOf(bySeedId);
        this.cropsByStageId = Map.copyOf(byStageId);

        if (!warnings.isEmpty()) {
            for (String warning : warnings) {
                this.plugin.getLogger().warning("[CustomCrops] " + warning);
            }
        }
        this.plugin.getLogger().info("[CustomCrops] 已加载 " + this.cropsById.size() + " 个作物定义。");
    }

    private static String seedId(ConfigurationSection blocks, FileConfiguration config, String firstStageId, String cropId) {
        String seedId = null;
        ConfigurationSection firstStage = blocks.getConfigurationSection(firstStageId);
        if (firstStage != null) {
            seedId = firstStage.getString("settings.overrides.item", null);
        }
        if (seedId != null && !seedId.isBlank()) {
            return normalizeKey(seedId.trim());
        }
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                if (key.endsWith("_seeds") || key.endsWith("_seed")) {
                    return normalizeKey(key);
                }
            }
        }
        return normalizeKey("customcrops:" + shortId(cropId) + "_seeds");
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static String shortId(String value) {
        String normalized = normalizeKey(value);
        int index = normalized.indexOf(':');
        return index < 0 ? normalized : normalized.substring(index + 1);
    }

    private record StageEntry(String id, int stage) {
    }
}
