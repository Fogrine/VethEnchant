package dev.vethcraft.vethenchant.listener;

import dev.vethcraft.vethenchant.api.context.BlockBreakContext;
import dev.vethcraft.vethenchant.api.context.BlockDropContext;
import dev.vethcraft.vethenchant.api.context.BlockExpContext;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import dev.vethcraft.vethenchant.effect.EffectExecutionGuards;
import dev.vethcraft.vethenchant.protection.ProtectionService;
import dev.vethcraft.vethenchant.util.CropUtil;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public final class ToolListener implements Listener {

    private final EffectDispatcher dispatcher;
    private final ProtectionService protectionService;

    public ToolListener(EffectDispatcher dispatcher, ProtectionService protectionService) {
        this.dispatcher = dispatcher;
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (EffectExecutionGuards.isApplyingAreaBreak()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        int level = ItemEnchantments.highestRegisteredLevel(item, this.dispatcher);

        if (!this.protectionService.canBreakBlock(player, event.getBlock())) {
            return;
        }

        this.dispatcher.blockExp(new BlockExpContext(event, player, item, level));
        this.dispatcher.blockBreak(new BlockBreakContext(event, player, item, level));

        Block block = event.getBlock();
        if (CropUtil.isMatureCrop(block) && this.protectionService.canModifyBlock(player, block)) {
            this.dispatcher.farm(FarmContext.vanilla(event, player, item, block, level));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!this.protectionService.canBreakBlock(player, event.getBlock())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        this.dispatcher.blockDrop(new BlockDropContext(event, player, item, ItemEnchantments.highestRegisteredLevel(item, this.dispatcher)));
    }
}
