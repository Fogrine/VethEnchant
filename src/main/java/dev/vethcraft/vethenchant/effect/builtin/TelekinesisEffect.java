package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.BlockDropContext;
import dev.vethcraft.vethenchant.api.context.BlockExpContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;

public final class TelekinesisEffect implements VethEnchantEffect {

    private static final NamespacedKey KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":telekinesis");

    private final VethEnchantPlugin plugin;

    public TelekinesisEffect(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockDrop(BlockDropContext context) {
        for (Item item : new ArrayList<>(context.event().getItems())) {
            ItemStack stack = item.getItemStack();
            Map<Integer, ItemStack> overflow = context.player().getInventory().addItem(stack);
            if (overflow.isEmpty()) {
                item.remove();
                context.event().getItems().remove(item);
            } else {
                item.setItemStack(overflow.values().iterator().next());
            }
        }
    }

    @Override
    public void onBlockExp(BlockExpContext context) {
        if (!this.plugin.getConfig().getBoolean("telekinesis.collect-exp", true)) {
            return;
        }
        int exp = context.event().getExpToDrop();
        if (exp <= 0) {
            return;
        }
        context.event().setExpToDrop(0);
        context.player().giveExp(exp);
    }
}
