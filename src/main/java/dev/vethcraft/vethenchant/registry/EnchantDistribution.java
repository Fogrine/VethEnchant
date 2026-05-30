package dev.vethcraft.vethenchant.registry;

public record EnchantDistribution(
    boolean enchantingTable,
    boolean randomLoot,
    boolean villagerTrade,
    boolean mobEquipment,
    boolean tradedEquipment,
    boolean treasure
) {

    public static EnchantDistribution common() {
        return new EnchantDistribution(true, true, true, true, true, false);
    }

    public static EnchantDistribution lootOnly() {
        return new EnchantDistribution(false, true, false, false, false, true);
    }
}
