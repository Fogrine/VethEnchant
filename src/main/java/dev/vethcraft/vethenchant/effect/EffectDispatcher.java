package dev.vethcraft.vethenchant.effect;

import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.api.VethEnchantEffect;
import dev.vethcraft.vethenchant.api.context.AttackContext;
import dev.vethcraft.vethenchant.api.context.BlockBreakContext;
import dev.vethcraft.vethenchant.api.context.BlockDropContext;
import dev.vethcraft.vethenchant.api.context.BlockExpContext;
import dev.vethcraft.vethenchant.api.context.DamageContext;
import dev.vethcraft.vethenchant.api.context.FarmContext;
import dev.vethcraft.vethenchant.api.event.VethEnchantTriggerEvent;
import dev.vethcraft.vethenchant.listener.CombatListener;
import dev.vethcraft.vethenchant.listener.CustomCropListener;
import dev.vethcraft.vethenchant.listener.PlayerStateListener;
import dev.vethcraft.vethenchant.listener.ToolListener;
import dev.vethcraft.vethenchant.util.ItemEnchantments;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class EffectDispatcher {

    private final VethEnchantPlugin plugin;
    private final Map<NamespacedKey, VethEnchantEffect> effects = new LinkedHashMap<>();

    public EffectDispatcher(VethEnchantPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(VethEnchantEffect effect) {
        this.effects.put(effect.key(), effect);
    }

    public void registerListeners() {
        this.plugin.getServer().getPluginManager().registerEvents(new CombatListener(this), this.plugin);
        this.plugin.getServer().getPluginManager().registerEvents(new ToolListener(this, this.plugin.protectionService()), this.plugin);
        if (this.plugin.getServer().getPluginManager().isPluginEnabled("CustomCrops")) {
            this.plugin.getServer().getPluginManager().registerEvents(new CustomCropListener(this.plugin, this), this.plugin);
        }
        this.plugin.getServer().getPluginManager().registerEvents(new PlayerStateListener(this), this.plugin);
    }

    public int effectCount() {
        return this.effects.size();
    }

    public Collection<VethEnchantEffect> effects() {
        return this.effects.values();
    }

    public Optional<VethEnchantEffect> effect(NamespacedKey key) {
        return Optional.ofNullable(this.effects.get(key));
    }

    public void attack(AttackContext context) {
        forEachEffect(context.item(), context.attacker(), "attack", (effect, level) ->
            effect.onAttack(new AttackContext(context.event(), context.attacker(), context.target(), context.item(), level)));
    }

    public void damaged(DamageContext context) {
        forEachEffect(context.item(), context.player(), "damaged", (effect, level) ->
            effect.onDamaged(new DamageContext(context.event(), context.player(), context.item(), level)));
    }

    public void blockBreak(BlockBreakContext context) {
        forEachEffect(context.item(), context.player(), "block_break", (effect, level) ->
            effect.onBlockBreak(new BlockBreakContext(context.event(), context.player(), context.item(), level)));
    }

    public void blockDrop(BlockDropContext context) {
        forEachEffectOrdered(context.item(), context.player(), "block_drop", (effect, level) ->
            effect.onBlockDrop(new BlockDropContext(context.event(), context.player(), context.item(), level)));
    }

    public void blockExp(BlockExpContext context) {
        forEachEffect(context.item(), context.player(), "block_exp", (effect, level) ->
            effect.onBlockExp(new BlockExpContext(context.event(), context.player(), context.item(), level)));
    }

    public void farm(FarmContext context) {
        forEachEffect(context.item(), context.player(), "farm", (effect, level) ->
            effect.onFarm(new FarmContext(context.event(), context.player(), context.item(), context.crop(), context.cropType(), context.customCrop(), level)));
    }

    private void forEachEffect(ItemStack item, Player player, String trigger, BiConsumer<VethEnchantEffect, Integer> consumer) {
        for (Map.Entry<Enchantment, Integer> entry : ItemEnchantments.enchantments(item).entrySet()) {
            dispatchEffect(player, trigger, consumer, entry);
        }
    }

    private void forEachEffectOrdered(ItemStack item, Player player, String trigger, BiConsumer<VethEnchantEffect, Integer> consumer) {
        List<Map.Entry<Enchantment, Integer>> entries = ItemEnchantments.enchantments(item).entrySet().stream()
            .sorted(Comparator.comparingInt(this::dropOrder).thenComparing(entry -> keyString(entry.getKey())))
            .toList();
        for (Map.Entry<Enchantment, Integer> entry : entries) {
            dispatchEffect(player, trigger, consumer, entry);
        }
    }

    private void dispatchEffect(Player player, String trigger, BiConsumer<VethEnchantEffect, Integer> consumer, Map.Entry<Enchantment, Integer> entry) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(entry.getKey());
        if (key == null || !this.effects.containsKey(key)) {
            return;
        }
        if (!this.plugin.isEnchantEnabled(key)) {
            return;
        }
        this.plugin.getServer().getPluginManager().callEvent(new VethEnchantTriggerEvent(player, key, entry.getValue(), trigger));
        consumer.accept(this.effects.get(key), entry.getValue());
    }

    private int dropOrder(Map.Entry<Enchantment, Integer> entry) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(entry.getKey());
        if (key == null) {
            return 50;
        }
        return switch (key.getKey()) {
            case "smelter" -> 10;
            case "gemfinder" -> 20;
            case "compact" -> 30;
            case "telekinesis", "magnetism" -> 90;
            default -> 50;
        };
    }

    private String keyString(Enchantment enchantment) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        return key == null ? enchantment.getKey().asString() : key.asString();
    }
}
