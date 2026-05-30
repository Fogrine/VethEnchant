package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.BlockBreakContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.NamespacedKey;

public final class WisdomEffect implements VethEnchantEffect {

    private static final NamespacedKey KEY = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":wisdom");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(BlockBreakContext context) {
        int xp = context.event().getExpToDrop();
        if (xp > 0) {
            context.event().setExpToDrop((int) Math.ceil(xp * (1.0D + 0.15D * context.level())));
        }
    }
}
