package dev.vethcraft.vethenchant.api;

import dev.vethcraft.vethenchant.api.context.AttackContext;
import dev.vethcraft.vethenchant.api.context.BlockBreakContext;
import dev.vethcraft.vethenchant.api.context.BlockDropContext;
import dev.vethcraft.vethenchant.api.context.BlockExpContext;
import dev.vethcraft.vethenchant.api.context.DamageContext;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import org.bukkit.NamespacedKey;

public interface VethEnchantEffect {

    NamespacedKey key();

    default void onAttack(AttackContext context) {
    }

    default void onDamaged(DamageContext context) {
    }

    default void onBlockBreak(BlockBreakContext context) {
    }

    default void onBlockDrop(BlockDropContext context) {
    }

    default void onBlockExp(BlockExpContext context) {
    }

    default void onFarm(FarmContext context) {
    }
}
