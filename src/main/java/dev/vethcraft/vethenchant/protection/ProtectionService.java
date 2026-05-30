package dev.vethcraft.vethenchant.protection;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.protection.hook.ResidenceProtectionHook;
import dev.vethcraft.vethenchant.protection.hook.WorldGuardProtectionHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ProtectionService {

    private final VethEnchantPlugin plugin;
    private String bypassPermission;
    private boolean worldGuardEnabled;
    private boolean residenceEnabled;

    public ProtectionService(VethEnchantPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.bypassPermission = this.plugin.getConfig().getString("protection.bypass-permission", "vethenchant.protection.bypass");
        this.worldGuardEnabled = this.plugin.getConfig().getBoolean("protection.worldguard", true);
        this.residenceEnabled = this.plugin.getConfig().getBoolean("protection.residence", true);
    }

    public boolean canBreakBlock(Player player, Block block) {
        return canAffect(player, block.getLocation(), ProtectionAction.BLOCK_BREAK);
    }

    public boolean canPlaceBlock(Player player, Location location) {
        return canAffect(player, location, ProtectionAction.BLOCK_PLACE);
    }

    public boolean canModifyBlock(Player player, Block block) {
        return canAffect(player, block.getLocation(), ProtectionAction.BLOCK_MODIFY);
    }

    public boolean canAffectEntity(Player player, LivingEntity target, ProtectionAction action) {
        if (player == null || target == null) {
            return false;
        }
        if (player.hasPermission(this.bypassPermission)) {
            return true;
        }
        return canAffectWorldGuardEntity(player, target, action) && canAffectResidence(player, target.getLocation(), action);
    }

    public boolean canAffect(Player player, Location location, ProtectionAction action) {
        if (player == null || location == null) {
            return false;
        }
        if (player.hasPermission(this.bypassPermission)) {
            return true;
        }
        return canAffectWorldGuard(player, location, action) && canAffectResidence(player, location, action);
    }

    public List<Block> filterBlocks(Player player, Collection<Block> blocks, ProtectionAction action) {
        List<Block> allowed = new ArrayList<>();
        for (Block block : blocks) {
            if (canAffect(player, block.getLocation(), action)) {
                allowed.add(block);
            }
        }
        return allowed;
    }

    public List<Entity> filterEntities(Player player, Collection<? extends Entity> entities, ProtectionAction action) {
        List<Entity> allowed = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity
                ? canAffectEntity(player, livingEntity, action)
                : canAffect(player, entity.getLocation(), action)) {
                allowed.add(entity);
            }
        }
        return allowed;
    }

    private boolean canAffectWorldGuard(Player player, Location location, ProtectionAction action) {
        if (!this.worldGuardEnabled || !isPluginEnabled("WorldGuard")) {
            return true;
        }

        return WorldGuardProtectionHook.canAffect(player, location, action);
    }

    private boolean canAffectWorldGuardEntity(Player player, LivingEntity target, ProtectionAction action) {
        if (!this.worldGuardEnabled || !isPluginEnabled("WorldGuard")) {
            return true;
        }
        if (action == ProtectionAction.ENTITY_DAMAGE) {
            return WorldGuardProtectionHook.canDamageEntity(player, target);
        }
        return WorldGuardProtectionHook.canAffect(player, target.getLocation(), action);
    }

    private boolean canAffectResidence(Player player, Location location, ProtectionAction action) {
        if (!this.residenceEnabled || !isPluginEnabled("Residence")) {
            return true;
        }
        return ResidenceProtectionHook.canAffect(player, location, action);
    }

    private boolean isPluginEnabled(String name) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
        return dependency != null && dependency.isEnabled();
    }
}
