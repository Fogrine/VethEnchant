package dev.vethcraft.vethenchant.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.vethcraft.vethenchant.VethEnchantPlugin;
import org.bukkit.entity.Player;

public final class PacketEventsBridge implements PacketBridge {

    private final VethEnchantPlugin plugin;
    private final String namespace;
    private PacketListenerCommon listener;

    public PacketEventsBridge(VethEnchantPlugin plugin, String namespace) {
        this.plugin = plugin;
        this.namespace = namespace;
    }

    @Override
    public void enable() {
        if (this.listener != null) {
            return;
        }
        this.listener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                // Extension point: inspect client interactions for enchantment-specific packet logic.
            }
        });
        this.plugin.getLogger().info("PacketEvents bridge enabled for namespace " + this.namespace + ".");
    }

    @Override
    public void disable() {
        if (this.listener == null) {
            return;
        }
        PacketEvents.getAPI().getEventManager().unregisterListener(this.listener);
        this.listener = null;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String status() {
        return "packetevents";
    }
}
