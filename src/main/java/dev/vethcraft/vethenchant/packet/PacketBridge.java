package dev.vethcraft.vethenchant.packet;

public interface PacketBridge {

    void enable();

    void disable();

    boolean available();

    String status();
}
