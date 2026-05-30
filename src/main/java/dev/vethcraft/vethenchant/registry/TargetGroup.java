package dev.vethcraft.vethenchant.registry;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;

import java.util.ArrayList;
import java.util.List;

public enum TargetGroup {
    SWORDS(List.of(ItemTypeTagKeys.SWORDS), List.of(EquipmentSlotGroup.MAINHAND)),
    AXES(List.of(ItemTypeTagKeys.AXES), List.of(EquipmentSlotGroup.MAINHAND)),
    MELEE(List.of(ItemTypeTagKeys.SWORDS, ItemTypeTagKeys.AXES), List.of(EquipmentSlotGroup.MAINHAND)),
    BOWS(List.of(ItemTypeTagKeys.ENCHANTABLE_BOW), List.of(EquipmentSlotGroup.MAINHAND)),
    CROSSBOWS(List.of(ItemTypeTagKeys.ENCHANTABLE_CROSSBOW), List.of(EquipmentSlotGroup.MAINHAND)),
    RANGED(List.of(ItemTypeTagKeys.ENCHANTABLE_BOW, ItemTypeTagKeys.ENCHANTABLE_CROSSBOW), List.of(EquipmentSlotGroup.MAINHAND)),
    TRIDENTS(List.of(ItemTypeTagKeys.ENCHANTABLE_TRIDENT, ItemTypeTagKeys.SPEARS), List.of(EquipmentSlotGroup.MAINHAND)),
    PICKAXES(List.of(ItemTypeTagKeys.PICKAXES), List.of(EquipmentSlotGroup.MAINHAND)),
    SHOVELS(List.of(ItemTypeTagKeys.SHOVELS), List.of(EquipmentSlotGroup.MAINHAND)),
    HOES(List.of(ItemTypeTagKeys.HOES), List.of(EquipmentSlotGroup.MAINHAND)),
    TOOLS(List.of(ItemTypeTagKeys.ENCHANTABLE_MINING), List.of(EquipmentSlotGroup.MAINHAND)),
    TOOLS_AND_WEAPONS(List.of(ItemTypeTagKeys.ENCHANTABLE_MINING, ItemTypeTagKeys.ENCHANTABLE_WEAPON), List.of(EquipmentSlotGroup.MAINHAND)),
    HELMETS(List.of(ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR), List.of(EquipmentSlotGroup.HEAD)),
    CHESTPLATES(List.of(ItemTypeTagKeys.ENCHANTABLE_CHEST_ARMOR), List.of(EquipmentSlotGroup.CHEST)),
    LEGGINGS(List.of(ItemTypeTagKeys.ENCHANTABLE_LEG_ARMOR), List.of(EquipmentSlotGroup.LEGS)),
    BOOTS(List.of(ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR), List.of(EquipmentSlotGroup.FEET)),
    ARMOR(List.of(ItemTypeTagKeys.ENCHANTABLE_ARMOR), List.of(EquipmentSlotGroup.ARMOR)),
    FISHING_RODS(List.of(ItemTypeTagKeys.ENCHANTABLE_FISHING), List.of(EquipmentSlotGroup.MAINHAND)),
    SHIELDS(List.of(), List.of(EquipmentSlotGroup.OFFHAND), List.of(Material.SHIELD)),
    ELYTRA(List.of(), List.of(EquipmentSlotGroup.CHEST), List.of(Material.ELYTRA)),
    ALL_DURABLE(List.of(ItemTypeTagKeys.ENCHANTABLE_DURABILITY), List.of(EquipmentSlotGroup.ANY));

    private final List<TagKey<ItemType>> vanillaTags;
    private final List<EquipmentSlotGroup> activeSlots;
    private final List<Material> extraMaterials;

    TargetGroup(List<TagKey<ItemType>> vanillaTags, List<EquipmentSlotGroup> activeSlots) {
        this(vanillaTags, activeSlots, List.of());
    }

    TargetGroup(List<TagKey<ItemType>> vanillaTags, List<EquipmentSlotGroup> activeSlots, List<Material> extraMaterials) {
        this.vanillaTags = vanillaTags;
        this.activeSlots = activeSlots;
        this.extraMaterials = extraMaterials;
    }

    public List<TypedKey<ItemType>> supportedItems(PostFlattenTagRegistrar<ItemType> registrar) {
        List<TypedKey<ItemType>> items = new ArrayList<>();
        for (TagKey<ItemType> vanillaTag : this.vanillaTags) {
            if (registrar.hasTag(vanillaTag)) {
                items.addAll(registrar.getTag(vanillaTag));
            }
        }
        items.addAll(this.extraMaterials.stream()
            .map(Material::asItemType)
            .map(ItemType::key)
            .map(key -> TypedKey.create(RegistryKey.ITEM, key))
            .toList());
        return items.stream().distinct().toList();
    }

    public List<TagKey<ItemType>> vanillaTags() {
        return this.vanillaTags;
    }

    public Iterable<EquipmentSlotGroup> activeSlots() {
        return this.activeSlots;
    }

    public TagKey<ItemType> itemTag(String suffix) {
        return TagKey.create(RegistryKey.ITEM, Key.key(EnchantCatalog.NAMESPACE, suffix));
    }
}
