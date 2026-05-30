package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.AttackContext;
import dev.vethcraft.vethenchant.effect.EffectExecutionGuards;
import dev.vethcraft.vethenchant.protection.ProtectionAction;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public final class PveCombatEffect implements VethEnchantEffect {

    private final VethEnchantPlugin plugin;
    private final NamespacedKey key;

    public PveCombatEffect(VethEnchantPlugin plugin, String id) {
        this.plugin = plugin;
        this.key = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":" + id);
    }

    @Override
    public NamespacedKey key() {
        return this.key;
    }

    @Override
    public void onAttack(AttackContext context) {
        if (EffectExecutionGuards.isApplyingCleaveDamage()) {
            return;
        }
        if (context.target() instanceof Player) {
            return;
        }
        if (!this.plugin.protectionService().canAffectEntity(context.attacker(), context.target(), ProtectionAction.ENTITY_DAMAGE)) {
            return;
        }
        String id = this.key.getKey();
        switch (id) {
            case "cleave" -> cleave(context);
            case "lifesteal" -> heal(context.attacker(), context.event().getFinalDamage() * (0.04D + 0.02D * context.level()));
            case "executioner" -> execute(context);
            case "hunter", "hunter_arrow", "beast_hunter" -> {
                if (context.target() instanceof Monster) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.08D * context.level()));
                }
            }
            case "raider_bane" -> {
                if (context.target() instanceof Raider) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.10D * context.level()));
                }
            }
            case "nether_bane" -> {
                String typeName = context.target().getType().name();
                if (typeName.contains("BLAZE") || typeName.contains("GHAST") || typeName.contains("MAGMA_CUBE")
                    || typeName.contains("PIGLIN") || typeName.contains("HOGLIN") || typeName.contains("WITHER")) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.09D * context.level()));
                }
            }
            case "end_bane" -> {
                String typeName = context.target().getType().name();
                if (typeName.contains("ENDERMAN") || typeName.contains("SHULKER") || typeName.contains("ENDER_DRAGON")) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.10D * context.level()));
                }
            }
            case "opening_strike" -> {
                if (context.target().getHealth() >= maxHealth(context.target()) * 0.75D) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.06D * context.level()));
                }
            }
            case "giant_slayer" -> {
                if (maxHealth(context.target()) >= 40.0D) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.08D * context.level()));
                }
            }
            case "purifier" -> {
                if (isUndead(context.target())) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.07D * context.level()));
                    context.target().setFireTicks(Math.max(context.target().getFireTicks(), 35 + 15 * context.level()));
                }
            }
            case "stagger" -> {
                if (ThreadLocalRandom.current().nextDouble() <= 0.14D * context.level()) {
                    context.target().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40 + context.level() * 15, 0));
                    context.target().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 35 + context.level() * 10, 0));
                }
            }
            case "soul_siphon" -> {
                if (context.target() instanceof Monster && ThreadLocalRandom.current().nextDouble() <= 0.08D * context.level()) {
                    context.attacker().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 45 + 15 * context.level(), 0));
                }
            }
            case "ferocity" -> {
                if (ThreadLocalRandom.current().nextDouble() <= 0.08D * context.level()) {
                    context.event().setDamage(context.event().getDamage() + 1.0D + context.level());
                }
            }
            case "frostbite", "frost_arrow" -> context.target().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35 + context.level() * 15, 0));
            case "poison_tip" -> context.target().addPotionEffect(new PotionEffect(PotionEffectType.POISON, 35 + context.level() * 15, 0));
            case "weakening", "pacify" -> context.target().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 50 + context.level() * 20, 0));
            case "guard_breaker" -> context.event().setDamage(context.event().getDamage() * (1.0D + armoredBonus(context.target(), context.level())));
            case "battle_focus" -> {
                if (context.attacker().getHealth() <= maxHealth(context.attacker()) * 0.5D) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.06D * context.level()));
                }
            }
            case "longshot" -> context.event().setDamage(context.event().getDamage() * (1.0D + Math.min(0.24D, context.attacker().getLocation().distance(context.target().getLocation()) * 0.01D * context.level())));
            case "close_quarters" -> {
                if (context.attacker().getLocation().distanceSquared(context.target().getLocation()) <= 9.0D) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.05D * context.level()));
                }
            }
            case "steady_aim" -> {
                if (context.attacker().isSneaking()) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.06D * context.level()));
                }
            }
            case "pinning" -> {
                if (ThreadLocalRandom.current().nextDouble() <= 0.18D * context.level()) {
                    context.target().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 45 + context.level() * 15, 1));
                }
            }
            case "tidecaller" -> {
                if (context.target().isInWater()) {
                    context.event().setDamage(context.event().getDamage() * (1.0D + 0.10D * context.level()));
                }
            }
            case "scavenger", "veteran" -> context.event().setDamage(context.event().getDamage() * (1.0D + 0.04D * context.level()));
            default -> {
            }
        }
    }

    private void cleave(AttackContext context) {
        double radius = 2.0D + 0.35D * context.level();
        double splash = context.event().getDamage() * (0.18D + 0.06D * context.level());
        int affected = 0;
        for (LivingEntity nearby : context.target().getLocation().getNearbyLivingEntities(radius)) {
            if (nearby.equals(context.target()) || nearby.equals(context.attacker()) || nearby instanceof Player) {
                continue;
            }
            if (!this.plugin.protectionService().canAffectEntity(context.attacker(), nearby, ProtectionAction.ENTITY_DAMAGE)) {
                continue;
            }
            EffectExecutionGuards.runCleaveDamage(() -> nearby.damage(splash, context.attacker()));
            affected++;
            if (affected >= 2 + context.level()) {
                return;
            }
        }
    }

    private void execute(AttackContext context) {
        double maxHealth = maxHealth(context.target());
        if (context.target().getHealth() <= maxHealth * (0.18D + 0.04D * context.level())) {
            context.event().setDamage(context.event().getDamage() * (1.0D + 0.12D * context.level()));
        }
    }

    private static boolean isUndead(LivingEntity target) {
        return target instanceof Zombie || target instanceof WitherSkeleton || target instanceof Wither || target instanceof Raider
            || target.getType().name().contains("SKELETON") || target.getType().name().contains("ZOMBIE");
    }

    private static double maxHealth(LivingEntity target) {
        var maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20.0D : maxHealth.getValue();
    }

    private static double armoredBonus(LivingEntity target, int level) {
        var armor = target.getAttribute(Attribute.ARMOR);
        if (armor == null || armor.getValue() <= 0.0D) {
            return 0.04D * level;
        }
        return Math.min(0.18D, armor.getValue() * 0.006D * level);
    }

    private static void heal(Player player, double amount) {
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0D : maxHealth.getValue();
        player.setHealth(Math.min(max, player.getHealth() + Math.max(0.0D, amount)));
    }
}
