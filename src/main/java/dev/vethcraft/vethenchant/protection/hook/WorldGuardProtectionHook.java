package dev.vethcraft.vethenchant.protection.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.vethcraft.vethenchant.protection.ProtectionAction;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class WorldGuardProtectionHook {

    private WorldGuardProtectionHook() {
    }

    public static boolean canAffect(Player player, Location location, ProtectionAction action) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (hasBypass(localPlayer, location)) {
            return true;
        }

        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldedit.util.Location adaptedLocation = BukkitAdapter.adapt(location);
        return switch (action) {
            case BLOCK_BREAK -> query.testBuild(adaptedLocation, localPlayer, com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK);
            case BLOCK_PLACE, BLOCK_MODIFY -> query.testBuild(adaptedLocation, localPlayer, com.sk89q.worldguard.protection.flags.Flags.BLOCK_PLACE);
            case EXPLOSION -> query.testBuild(adaptedLocation, localPlayer, com.sk89q.worldguard.protection.flags.Flags.OTHER_EXPLOSION);
            case ENTITY_DAMAGE -> {
                StateFlag custom = optionalStateFlag("damage-entities");
                yield custom == null || query.testState(adaptedLocation, localPlayer, custom);
            }
        };
    }

    public static boolean canDamageEntity(Player player, LivingEntity target) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (hasBypass(localPlayer, target.getLocation())) {
            return true;
        }

        StateFlag flag = entityDamageFlag(target);
        if (flag == null) {
            return true;
        }
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
            .testState(BukkitAdapter.adapt(target.getLocation()), localPlayer, flag);
    }

    private static boolean hasBypass(LocalPlayer localPlayer, Location location) {
        return WorldGuard.getInstance().getPlatform().getSessionManager()
            .hasBypass(localPlayer, BukkitAdapter.adapt(location.getWorld()));
    }

    private static StateFlag entityDamageFlag(LivingEntity target) {
        StateFlag custom = optionalStateFlag("damage-entities");
        if (custom != null) {
            return custom;
        }
        if (target instanceof Player) {
            return com.sk89q.worldguard.protection.flags.Flags.PVP;
        }
        if (target instanceof Animals) {
            return com.sk89q.worldguard.protection.flags.Flags.DAMAGE_ANIMALS;
        }
        return null;
    }

    private static StateFlag optionalStateFlag(String id) {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        if (registry.get(id) instanceof StateFlag stateFlag) {
            return stateFlag;
        }
        return null;
    }
}
