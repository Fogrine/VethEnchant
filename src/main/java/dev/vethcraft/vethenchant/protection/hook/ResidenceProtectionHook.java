package dev.vethcraft.vethenchant.protection.hook;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.permissions.PermissionManager.ResPerm;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import dev.vethcraft.vethenchant.protection.ProtectionAction;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ResidenceProtectionHook {

    private ResidenceProtectionHook() {
    }

    public static boolean canAffect(Player player, Location location, ProtectionAction action) {
        if (Residence.getInstance() == null) {
            return true;
        }

        FlagPermissions permissions = FlagPermissions.getPerms(location, player);
        return switch (action) {
            case BLOCK_BREAK -> canDestroy(player, permissions);
            case BLOCK_PLACE, BLOCK_MODIFY -> canPlace(player, permissions);
            case ENTITY_DAMAGE -> canDamageEntity(player, permissions);
            case EXPLOSION -> permissions.has(Flags.explode, true);
        };
    }

    private static boolean canDestroy(Player player, FlagPermissions permissions) {
        return permissions.playerHas(player, Flags.destroy, permissions.playerHas(player, Flags.build, true))
            || ResPerm.bypass_destroy.hasPermission(player, 10000L);
    }

    private static boolean canPlace(Player player, FlagPermissions permissions) {
        return permissions.playerHas(player, Flags.place, permissions.playerHas(player, Flags.build, true))
            || ResPerm.bypass_build.hasPermission(player, 10000L);
    }

    private static boolean canDamageEntity(Player player, FlagPermissions permissions) {
        return permissions.playerHas(player, Flags.damage, true)
            || permissions.playerHas(player, Flags.animalkilling, true)
            || permissions.playerHas(player, Flags.mobkilling, true);
    }
}
