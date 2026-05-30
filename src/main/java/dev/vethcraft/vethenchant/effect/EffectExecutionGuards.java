package dev.vethcraft.vethenchant.effect;

public final class EffectExecutionGuards {

    private static final ThreadLocal<Boolean> APPLYING_CLEAVE_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> APPLYING_AREA_BREAK = ThreadLocal.withInitial(() -> false);

    private EffectExecutionGuards() {
    }

    public static boolean isApplyingCleaveDamage() {
        return APPLYING_CLEAVE_DAMAGE.get();
    }

    public static boolean isApplyingAreaBreak() {
        return APPLYING_AREA_BREAK.get();
    }

    public static void runCleaveDamage(Runnable runnable) {
        boolean previous = APPLYING_CLEAVE_DAMAGE.get();
        APPLYING_CLEAVE_DAMAGE.set(true);
        try {
            runnable.run();
        } finally {
            APPLYING_CLEAVE_DAMAGE.set(previous);
        }
    }

    public static void runAreaBreak(Runnable runnable) {
        boolean previous = APPLYING_AREA_BREAK.get();
        APPLYING_AREA_BREAK.set(true);
        try {
            runnable.run();
        } finally {
            APPLYING_AREA_BREAK.set(previous);
        }
    }
}
