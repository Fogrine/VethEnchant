package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import dev.vethcraft.vethenchant.util.CustomCropData;
import dev.vethcraft.vethenchant.util.CropUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public final class ReplenishEffect implements VethEnchantEffect {

    private static final NamespacedKey KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":replenish");
    private static final NamespacedKey SEED_SAVER_KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":seed_saver");

    private final VethEnchantPlugin plugin;

    public ReplenishEffect(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onFarm(FarmContext context) {
        CustomCropData customCrop = context.customCrop();
        if (customCrop != null) {
            if (!shouldPreserveSeed(context) && !this.plugin.customCropService().consumeSeed(context.player(), customCrop)) {
                return;
            }
            Bukkit.getScheduler().runTaskLater(this.plugin, () ->
                this.plugin.customCropService().replant(context.crop().getLocation(), customCrop), 1L);
            return;
        }

        ItemStack seed = CropUtil.seedFor(context.cropType());
        boolean preserveSeed = shouldPreserveSeed(context);
        if (seed == null || (!preserveSeed && !context.player().getInventory().containsAtLeast(seed, 1))) {
            return;
        }
        if (!preserveSeed) {
            context.player().getInventory().removeItem(seed);
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> CropUtil.replant(context.crop(), context.cropType()), 1L);
    }

    private static boolean shouldPreserveSeed(FarmContext context) {
        int level = ItemEnchantments.level(context.item(), SEED_SAVER_KEY);
        return level > 0 && ThreadLocalRandom.current().nextDouble() <= Math.min(0.55D, 0.18D + 0.16D * level);
    }
}
