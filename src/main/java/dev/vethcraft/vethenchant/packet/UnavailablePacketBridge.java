package dev.vethcraft.vethenchant.packet;

public final class UnavailablePacketBridge implements PacketBridge {

    private final String reason;

    public UnavailablePacketBridge(String reason) {
        this.reason = reason;
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String status() {
        return this.reason;
    }
}
