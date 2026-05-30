package dev.vethcraft.vethenchant.protection;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class PlacedBlockTracker implements Listener {

    private final VethEnchantPlugin plugin;
    private final File file;
    private final Map<String, Set<BlockPos>> placedBlocks = new HashMap<>();
    private boolean enabled;
    private String bypassPermission;
    private boolean dirty;
    private BukkitTask autosaveTask;

    public PlacedBlockTracker(VethEnchantPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "placed-blocks.yml");
    }

    public void enable() {
        reload();
        load();
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        startAutosave();
    }

    public void disable() {
        if (this.autosaveTask != null) {
            this.autosaveTask.cancel();
            this.autosaveTask = null;
        }
        saveNow();
    }

    public void reload() {
        this.enabled = this.plugin.getConfig().getBoolean("placed-block-protection.enabled", true);
        this.bypassPermission = this.plugin.getConfig().getString("placed-block-protection.bypass-permission", "").trim();
        startAutosave();
    }

    public boolean shouldSkipAreaBreak(Player player, Block block) {
        if (!this.enabled || player == null || block == null) {
            return false;
        }
        if (!this.bypassPermission.isBlank() && player.hasPermission(this.bypassPermission)) {
            return false;
        }
        return isPlayerPlaced(block);
    }

    public boolean isPlayerPlaced(Block block) {
        if (block == null || block.getWorld() == null) {
            return false;
        }
        Set<BlockPos> positions = this.placedBlocks.get(block.getWorld().getName());
        return positions != null && positions.contains(BlockPos.of(block));
    }

    public void unmark(Block block) {
        if (block == null || block.getWorld() == null) {
            return;
        }
        Set<BlockPos> positions = this.placedBlocks.get(block.getWorld().getName());
        if (positions != null && positions.remove(BlockPos.of(block))) {
            this.dirty = true;
            if (positions.isEmpty()) {
                this.placedBlocks.remove(block.getWorld().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!this.enabled) {
            return;
        }
        Block block = event.getBlockPlaced();
        this.placedBlocks.computeIfAbsent(block.getWorld().getName(), ignored -> new HashSet<>()).add(BlockPos.of(block));
        this.dirty = true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        unmark(event.getBlock());
    }

    private void startAutosave() {
        if (this.autosaveTask != null) {
            this.autosaveTask.cancel();
            this.autosaveTask = null;
        }
        int seconds = Math.max(10, this.plugin.getConfig().getInt("placed-block-protection.autosave-interval-seconds", 60));
        this.autosaveTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::saveIfDirty, seconds * 20L, seconds * 20L);
    }

    private void load() {
        this.placedBlocks.clear();
        if (!this.file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        if (worlds == null) {
            return;
        }
        for (String worldName : worlds.getKeys(false)) {
            Set<BlockPos> positions = new HashSet<>();
            for (String raw : worlds.getStringList(worldName)) {
                BlockPos.parse(raw).ifPresent(positions::add);
            }
            if (!positions.isEmpty()) {
                this.placedBlocks.put(worldName, positions);
            }
        }
    }

    private void saveIfDirty() {
        if (this.dirty) {
            saveNow();
        }
    }

    public void saveNow() {
        if (!this.dirty && this.file.exists()) {
            return;
        }
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Set<BlockPos>> entry : this.placedBlocks.entrySet()) {
            config.set("worlds." + entry.getKey(), entry.getValue().stream().map(BlockPos::serialize).sorted().toList());
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(this.file);
            this.dirty = false;
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save placed block protection data.", exception);
        }
    }

    private record BlockPos(int x, int y, int z) {
        static BlockPos of(Block block) {
            Location location = block.getLocation();
            World world = block.getWorld();
            return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        static java.util.Optional<BlockPos> parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return java.util.Optional.empty();
            }
            String[] parts = raw.split(",", 3);
            if (parts.length != 3) {
                return java.util.Optional.empty();
            }
            try {
                return java.util.Optional.of(new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException exception) {
                return java.util.Optional.empty();
            }
        }

        String serialize() {
            return this.x + "," + this.y + "," + this.z;
        }
    }
}
