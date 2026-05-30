package dev.vethcraft.vethenchant.config;

import org.bukkit.configuration.file.FileConfiguration;

public record MessageConfig(
    String prefix,
    String reload,
    String noPermission,
    String limitDeny
) {

    public static MessageConfig load(FileConfiguration config) {
        return new MessageConfig(
            config.getString("messages.prefix", "<primary>VethEnchant</primary> <muted>|</muted> "),
            config.getString("messages.reload", "<primary>配置已重载。注册表/硬修改数值需要重启服务器后生效。</primary>"),
            config.getString("messages.no-permission", "<danger>你没有权限使用这个指令。</danger>"),
            config.getString("messages.enchant-limit-deny", "<warning>这件物品的附魔槽位不够啦。</warning> <muted>当前 {used}/{max}</muted>")
        );
    }
}
