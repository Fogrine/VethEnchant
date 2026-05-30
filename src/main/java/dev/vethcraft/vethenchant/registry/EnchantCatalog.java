package dev.vethcraft.vethenchant.registry;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;

import java.util.List;

public final class EnchantCatalog {

    public static final String NAMESPACE = "vethenchant";

    public static final BootstrapEnchantDefinition ABRASION = definition("abrasion", "磨蚀", TargetGroup.MELEE, 3, 5, 8, 9, 28, 9, 4, EnchantDistribution.common());
    public static final BootstrapEnchantDefinition REPLENISH = definition("replenish", "催生", TargetGroup.HOES, 1, 2, 18, 0, 50, 0, 8, EnchantDistribution.lootOnly());
    public static final BootstrapEnchantDefinition TELEKINESIS = definition("telekinesis", "吸星", TargetGroup.TOOLS_AND_WEAPONS, 1, 6, 12, 0, 40, 0, 4, EnchantDistribution.common());
    public static final BootstrapEnchantDefinition WISDOM = definition("wisdom", "智识", TargetGroup.TOOLS_AND_WEAPONS, 2, 8, 6, 10, 24, 12, 3, EnchantDistribution.common());
    public static final BootstrapEnchantDefinition FREERUNNER = definition("freerunner", "轻盈", TargetGroup.BOOTS, 3, 8, 6, 8, 24, 8, 3, EnchantDistribution.common());
    public static final BootstrapEnchantDefinition BLOCK_BREATHER = definition("block_breather", "无氧", TargetGroup.HELMETS, 2, 10, 4, 8, 20, 8, 2, EnchantDistribution.common());

    private static final List<BootstrapEnchantDefinition> DEFINITIONS = List.of(
        ABRASION,
        REPLENISH,
        TELEKINESIS,
        WISDOM,
        FREERUNNER,
        BLOCK_BREATHER,
        definition("vein_miner", "矿脉", TargetGroup.PICKAXES, 3, 5, EnchantDistribution.common()),
        definition("tunneling", "掘进", TargetGroup.PICKAXES, 3, 4, EnchantDistribution.common()),
        definition("excavator", "掘土", TargetGroup.SHOVELS, 3, 5, EnchantDistribution.common()),
        definition("timber", "伐木", TargetGroup.AXES, 3, 5, EnchantDistribution.common()),
        definition("harvester", "丰收", TargetGroup.HOES, 3, 6, EnchantDistribution.common()),
        definition("smelter", "熔炼", TargetGroup.PICKAXES, 1, 4, EnchantDistribution.lootOnly()),
        definition("gemfinder", "寻晶", TargetGroup.PICKAXES, 3, 3, EnchantDistribution.common()),
        definition("stonebound", "坚岩", TargetGroup.PICKAXES, 3, 6, EnchantDistribution.common()),
        definition("quarry", "采场", TargetGroup.PICKAXES, 2, 2, EnchantDistribution.lootOnly()),
        definition("magnetism", "磁引", TargetGroup.TOOLS_AND_WEAPONS, 2, 5, EnchantDistribution.common()),
        definition("compact", "归整", TargetGroup.TOOLS, 1, 4, EnchantDistribution.lootOnly()),
        definition("green_thumb", "青芽", TargetGroup.HOES, 3, 6, EnchantDistribution.common()),
        definition("deep_delver", "深掘", TargetGroup.PICKAXES, 3, 4, EnchantDistribution.common()),
        definition("prospector", "探矿", TargetGroup.PICKAXES, 3, 5, EnchantDistribution.common()),
        definition("mineral_luck", "矿运", TargetGroup.PICKAXES, 3, 3, EnchantDistribution.lootOnly()),
        definition("soft_touch", "轻采", TargetGroup.TOOLS, 2, 5, EnchantDistribution.common()),
        definition("flinting", "燧取", TargetGroup.SHOVELS, 3, 7, EnchantDistribution.common()),
        definition("clayfinder", "寻陶", TargetGroup.SHOVELS, 3, 6, EnchantDistribution.common()),
        definition("lumber_bounty", "木馈", TargetGroup.AXES, 3, 5, EnchantDistribution.common()),
        definition("cleave", "顺劈", TargetGroup.MELEE, 3, 5, EnchantDistribution.common()),
        definition("lifesteal", "汲取", TargetGroup.MELEE, 3, 3, EnchantDistribution.common()),
        definition("executioner", "斩决", TargetGroup.MELEE, 3, 4, EnchantDistribution.common()),
        definition("hunter", "狩猎", TargetGroup.MELEE, 3, 6, EnchantDistribution.common()),
        definition("beast_hunter", "兽猎", TargetGroup.MELEE, 3, 6, EnchantDistribution.common()),
        definition("raider_bane", "破袭", TargetGroup.MELEE, 3, 5, EnchantDistribution.common()),
        definition("nether_bane", "狱猎", TargetGroup.MELEE, 3, 4, EnchantDistribution.common()),
        definition("end_bane", "末猎", TargetGroup.MELEE, 3, 4, EnchantDistribution.common()),
        definition("opening_strike", "破势", TargetGroup.MELEE, 3, 5, EnchantDistribution.common()),
        definition("giant_slayer", "巨猎", TargetGroup.MELEE, 3, 4, EnchantDistribution.common()),
        definition("purifier", "净刃", TargetGroup.MELEE, 2, 5, EnchantDistribution.common()),
        definition("stagger", "踉跄", TargetGroup.MELEE, 2, 4, EnchantDistribution.common()),
        definition("soul_siphon", "魂汲", TargetGroup.MELEE, 2, 3, EnchantDistribution.lootOnly()),
        definition("ferocity", "凶猛", TargetGroup.MELEE, 3, 4, EnchantDistribution.common()),
        definition("frostbite", "霜咬", TargetGroup.MELEE, 2, 4, EnchantDistribution.common()),
        definition("poison_tip", "淬毒", TargetGroup.MELEE, 2, 4, EnchantDistribution.common()),
        definition("weakening", "虚弱", TargetGroup.MELEE, 2, 4, EnchantDistribution.common()),
        definition("guard_breaker", "破防", TargetGroup.MELEE, 3, 3, EnchantDistribution.lootOnly()),
        definition("battle_focus", "战意", TargetGroup.MELEE, 3, 5, EnchantDistribution.common()),
        definition("longshot", "远矢", TargetGroup.BOWS, 3, 5, EnchantDistribution.common()),
        definition("steady_aim", "稳弦", TargetGroup.BOWS, 3, 5, EnchantDistribution.common()),
        definition("close_quarters", "近射", TargetGroup.RANGED, 3, 5, EnchantDistribution.common()),
        definition("pinning", "钉足", TargetGroup.RANGED, 2, 4, EnchantDistribution.common()),
        definition("ricochet", "弹射", TargetGroup.CROSSBOWS, 2, 3, EnchantDistribution.lootOnly()),
        definition("frost_arrow", "霜箭", TargetGroup.RANGED, 2, 4, EnchantDistribution.common()),
        definition("quickdraw", "疾射", TargetGroup.BOWS, 2, 5, EnchantDistribution.common()),
        definition("scatterbolt", "散弩", TargetGroup.CROSSBOWS, 2, 3, EnchantDistribution.lootOnly()),
        definition("hunter_arrow", "猎矢", TargetGroup.RANGED, 3, 5, EnchantDistribution.common()),
        definition("vitality", "活力", TargetGroup.CHESTPLATES, 3, 5, EnchantDistribution.common()),
        definition("aegis", "庇护", TargetGroup.ARMOR, 3, 5, EnchantDistribution.common()),
        definition("warding", "守御", TargetGroup.ARMOR, 3, 5, EnchantDistribution.common()),
        definition("sentinel", "哨卫", TargetGroup.ARMOR, 2, 6, EnchantDistribution.common()),
        definition("cleansing", "清心", TargetGroup.ARMOR, 2, 5, EnchantDistribution.common()),
        definition("fireward", "御火", TargetGroup.ARMOR, 3, 6, EnchantDistribution.common()),
        definition("frostguard", "御寒", TargetGroup.ARMOR, 2, 5, EnchantDistribution.common()),
        definition("bastion", "坚垒", TargetGroup.CHESTPLATES, 3, 4, EnchantDistribution.common()),
        definition("bulwark", "盾壁", TargetGroup.SHIELDS, 3, 5, EnchantDistribution.common()),
        definition("rebound", "回震", TargetGroup.CHESTPLATES, 3, 4, EnchantDistribution.common()),
        definition("second_wind", "回息", TargetGroup.CHESTPLATES, 2, 3, EnchantDistribution.lootOnly()),
        definition("nourishing", "滋养", TargetGroup.HELMETS, 2, 5, EnchantDistribution.common()),
        definition("lightstep", "轻步", TargetGroup.BOOTS, 3, 6, EnchantDistribution.common()),
        definition("sprinter", "疾行", TargetGroup.BOOTS, 3, 5, EnchantDistribution.common()),
        definition("blastguard", "震护", TargetGroup.ARMOR, 3, 5, EnchantDistribution.common()),
        definition("arrowguard", "箭护", TargetGroup.ARMOR, 3, 6, EnchantDistribution.common()),
        definition("rooted", "扎根", TargetGroup.BOOTS, 2, 5, EnchantDistribution.common()),
        definition("aquatic", "亲水", TargetGroup.HELMETS, 3, 5, EnchantDistribution.common()),
        definition("night_owl", "夜行", TargetGroup.HELMETS, 1, 4, EnchantDistribution.lootOnly()),
        definition("steady_feet", "稳足", TargetGroup.BOOTS, 2, 5, EnchantDistribution.common()),
        definition("glider", "滑翔", TargetGroup.ELYTRA, 3, 4, EnchantDistribution.common()),
        definition("angler_luck", "渔运", TargetGroup.FISHING_RODS, 3, 5, EnchantDistribution.common()),
        definition("quick_bite", "快咬", TargetGroup.FISHING_RODS, 3, 5, EnchantDistribution.common()),
        definition("treasure_hook", "宝钩", TargetGroup.FISHING_RODS, 2, 3, EnchantDistribution.lootOnly()),
        definition("gentle_hook", "柔钩", TargetGroup.FISHING_RODS, 2, 5, EnchantDistribution.common()),
        definition("tidecaller", "唤潮", TargetGroup.TRIDENTS, 3, 4, EnchantDistribution.common()),
        definition("stormcast", "掷雷", TargetGroup.TRIDENTS, 2, 3, EnchantDistribution.lootOnly()),
        definition("returning_current", "回流", TargetGroup.TRIDENTS, 2, 5, EnchantDistribution.common()),
        definition("windglide", "乘风", TargetGroup.ELYTRA, 3, 4, EnchantDistribution.common()),
        definition("safe_landing", "稳降", TargetGroup.ELYTRA, 2, 5, EnchantDistribution.common()),
        definition("scavenger", "拾荒", TargetGroup.MELEE, 3, 5, EnchantDistribution.common()),
        definition("veteran", "老练", TargetGroup.TOOLS_AND_WEAPONS, 3, 5, EnchantDistribution.common()),
        definition("pacify", "安抚", TargetGroup.MELEE, 2, 4, EnchantDistribution.common())
    );

    private EnchantCatalog() {
    }

    public static List<BootstrapEnchantDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Key key(String id) {
        return Key.key(NAMESPACE, id);
    }

    public static TypedKey<Enchantment> typedKey(String id) {
        return TypedKey.create(RegistryKey.ENCHANTMENT, key(id));
    }

    public static TypedKey<Enchantment> vanilla(String id) {
        return EnchantmentKeys.create(Key.key(Key.MINECRAFT_NAMESPACE, id));
    }

    private static BootstrapEnchantDefinition definition(
        String id,
        String displayName,
        TargetGroup target,
        int maxLevel,
        int weight,
        EnchantDistribution distribution,
        TypedKey<Enchantment>... exclusiveWith
    ) {
        return definition(
            id,
            displayName,
            target,
            maxLevel,
            weight,
            5 + Math.max(0, 8 - weight),
            8,
            25 + Math.max(0, 8 - weight) * 2,
            8,
            Math.max(2, Math.min(8, maxLevel + 1)),
            distribution,
            exclusiveWith
        );
    }

    private static BootstrapEnchantDefinition definition(
        String id,
        String displayName,
        TargetGroup target,
        int maxLevel,
        int weight,
        int minimumCostBase,
        int minimumCostPerLevel,
        int maximumCostBase,
        int maximumCostPerLevel,
        int anvilCost,
        EnchantDistribution distribution,
        TypedKey<Enchantment>... exclusiveWith
    ) {
        return new BootstrapEnchantDefinition(
            id,
            key(id),
            displayName,
            target,
            maxLevel,
            weight,
            minimumCostBase,
            minimumCostPerLevel,
            maximumCostBase,
            maximumCostPerLevel,
            anvilCost,
            distribution,
            List.of(exclusiveWith)
        );
    }
}
