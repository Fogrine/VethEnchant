package dev.vethcraft.vethenchant.effect.builtin;

import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.DamageContext;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SurvivalArmorEffect implements VethEnchantEffect {

    private static final long SECOND_WIND_COOLDOWN_MILLIS = 45_000L;
    private static final Map<UUID, Long> SECOND_WIND_LAST_TRIGGER = new ConcurrentHashMap<>();
    private static final Set<EntityDamageEvent.DamageCause> FIRE = EnumSet.of(
        EntityDamageEvent.DamageCause.FIRE,
        EntityDamageEvent.DamageCause.FIRE_TICK,
        EntityDamageEvent.DamageCause.LAVA,
        EntityDamageEvent.DamageCause.CAMPFIRE,
        EntityDamageEvent.DamageCause.HOT_FLOOR
    );

    private final NamespacedKey key;

    public SurvivalArmorEffect(String id) {
        this.key = NamespacedKey.fromString(EnchantCatalog.NAMESPACE + ":" + id);
    }

    @Override
    public NamespacedKey key() {
        return this.key;
    }

    @Override
    public void onDamaged(DamageContext context) {
        String id = this.key.getKey();
        switch (id) {
            case "aegis" -> reduce(context, 0.06D * context.level());
            case "warding" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    reduce(context, 0.05D * context.level());
                }
            }
            case "sentinel" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                    || context.event().getCause() == EntityDamageEvent.DamageCause.MAGIC) {
                    reduce(context, 0.07D * context.level());
                }
            }
            case "cleansing" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.POISON
                    || context.event().getCause() == EntityDamageEvent.DamageCause.WITHER
                    || context.event().getCause() == EntityDamageEvent.DamageCause.MAGIC) {
                    reduce(context, 0.08D * context.level());
                }
            }
            case "fireward" -> {
                if (FIRE.contains(context.event().getCause())) {
                    reduce(context, 0.10D * context.level());
                    context.player().setFireTicks(Math.max(0, context.player().getFireTicks() - 25 * context.level()));
                }
            }
            case "frostguard" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.FREEZE) {
                    reduce(context, 0.14D * context.level());
                    context.player().setFreezeTicks(Math.max(0, context.player().getFreezeTicks() - 30 * context.level()));
                }
            }
            case "blastguard" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    reduce(context, 0.08D * context.level());
                }
            }
            case "arrowguard" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    reduce(context, 0.07D * context.level());
                }
            }
            case "rebound" -> {
                if (ThreadLocalRandom.current().nextDouble() <= 0.08D * context.level()) {
                    reduce(context, 0.08D * context.level());
                }
            }
            case "second_wind" -> secondWind(context);
            case "lightstep", "safe_landing" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.FALL) {
                    reduce(context, 0.18D * context.level());
                }
            }
            case "steady_feet" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.FALL) {
                    reduce(context, 0.10D * context.level());
                }
            }
            case "rooted" -> {
                if (context.player().isSneaking()
                    && (context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.PROJECTILE)) {
                    reduce(context, 0.05D * context.level());
                }
            }
            case "bastion" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                    reduce(context, 0.09D * context.level());
                }
            }
            case "bulwark" -> {
                if (context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    || context.event().getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                    || context.event().getCause() == EntityDamageEvent.DamageCause.MAGIC) {
                    reduce(context, 0.08D * context.level());
                }
            }
            default -> {
            }
        }
    }

    private static void reduce(DamageContext context, double ratio) {
        double clamped = Math.max(0.0D, Math.min(0.65D, ratio));
        context.event().setDamage(Math.max(0.0D, context.event().getDamage() * (1.0D - clamped)));
    }

    private static void secondWind(DamageContext context) {
        var maxHealth = context.player().getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0D : maxHealth.getValue();
        if (context.player().getHealth() - context.event().getFinalDamage() > max * 0.35D) {
            return;
        }
        UUID playerId = context.player().getUniqueId();
        long now = System.currentTimeMillis();
        long lastTrigger = SECOND_WIND_LAST_TRIGGER.getOrDefault(playerId, 0L);
        if (now - lastTrigger < SECOND_WIND_COOLDOWN_MILLIS) {
            return;
        }
        SECOND_WIND_LAST_TRIGGER.put(playerId, now);
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60 + 20 * context.level(), 0));
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40 + 10 * context.level(), 0));
    }
}
