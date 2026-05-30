package dev.vethcraft.vethenchant.api.event;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class VethEnchantTriggerEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final NamespacedKey enchantment;
    private final int level;
    private final String trigger;

    public VethEnchantTriggerEvent(Player player, NamespacedKey enchantment, int level, String trigger) {
        this.player = player;
        this.enchantment = enchantment;
        this.level = level;
        this.trigger = trigger;
    }

    public Player player() {
        return this.player;
    }

    public NamespacedKey enchantment() {
        return this.enchantment;
    }

    public int level() {
        return this.level;
    }

    public String trigger() {
        return this.trigger;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
