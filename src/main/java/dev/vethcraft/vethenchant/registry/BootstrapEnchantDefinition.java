package dev.vethcraft.vethenchant.registry;

import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemType;

import java.util.List;

public record BootstrapEnchantDefinition(
    String id,
    Key key,
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
    List<TypedKey<Enchantment>> exclusiveWith
) {

    public String translationKey() {
        return "enchantment." + key.namespace() + "." + key.value();
    }

    public TagKey<ItemType> supportedItemTag() {
        return target.itemTag(id + "_supported");
    }

    public TagKey<ItemType> primaryItemTag() {
        return target.itemTag(id + "_primary");
    }
}
