package dev.vethcraft.vethenchant.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.vethcraft.vethenchant.config.ConfigHealthReport;
import dev.vethcraft.vethenchant.config.EnchantDefinition;
import dev.vethcraft.vethenchant.VethEnchantPlugin;
import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class VethEnchantCommand {

    private static final int LIST_PAGE_SIZE = 12;
    private static final int WARNING_PAGE_SIZE = 8;

    private final VethEnchantPlugin plugin;
    private final EffectDispatcher dispatcher;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public VethEnchantCommand(VethEnchantPlugin plugin, EffectDispatcher dispatcher) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
    }

    public void register() {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
            Commands.literal("vethenchant")
                .requires(source -> source.getSender().hasPermission("vethenchant.command"))
                .executes(context -> {
                    sendInfo(context.getSource().getSender());
                    return 1;
                })
                .then(Commands.literal("status")
                    .requires(source -> source.getSender().hasPermission("vethenchant.admin"))
                    .executes(context -> {
                        sendStatus(context.getSource().getSender());
                        return 1;
                    }))
                .then(Commands.literal("list")
                    .requires(source -> source.getSender().hasPermission("vethenchant.admin"))
                    .executes(context -> {
                        sendList(context.getSource().getSender(), "all");
                        return 1;
                    })
                    .then(Commands.argument("filter", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            suggest(builder, "all", "custom", "vanilla", "enabled", "disabled");
                            for (String rarity : this.plugin.vethConfig().rarities().keySet()) {
                                suggest(builder, rarity);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            sendList(context.getSource().getSender(), StringArgumentType.getString(context, "filter"));
                            return 1;
                        })))
                .then(Commands.literal("inspect")
                    .requires(source -> source.getSender().hasPermission("vethenchant.admin"))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (EnchantDefinition definition : this.plugin.vethConfig().enchants().all()) {
                                suggest(builder, definition.key().asString(), definition.key().getKey());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            inspect(context.getSource().getSender(), StringArgumentType.getString(context, "id"));
                            return 1;
                        })))
                .then(Commands.literal("warnings")
                    .requires(source -> source.getSender().hasPermission("vethenchant.admin"))
                    .executes(context -> {
                        sendWarnings(context.getSource().getSender(), 1);
                        return 1;
                    })
                    .then(Commands.argument("page", StringArgumentType.word())
                        .executes(context -> {
                            sendWarnings(context.getSource().getSender(), parsePage(StringArgumentType.getString(context, "page")));
                            return 1;
                        })))
                .then(Commands.literal("givebook")
                    .requires(source -> source.getSender().hasPermission("vethenchant.admin"))
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                suggest(builder, player.getName());
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                for (EnchantDefinition definition : this.plugin.vethConfig().enchants().all()) {
                                    suggest(builder, definition.key().asString(), definition.key().getKey());
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                giveBook(
                                    context.getSource().getSender(),
                                    StringArgumentType.getString(context, "player"),
                                    StringArgumentType.getString(context, "id"),
                                    1,
                                    1
                                );
                                return 1;
                            })
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                .executes(context -> {
                                    giveBook(
                                        context.getSource().getSender(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "id"),
                                        IntegerArgumentType.getInteger(context, "level"),
                                        1
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                    .executes(context -> {
                                        giveBook(
                                            context.getSource().getSender(),
                                            StringArgumentType.getString(context, "player"),
                                            StringArgumentType.getString(context, "id"),
                                            IntegerArgumentType.getInteger(context, "level"),
                                            IntegerArgumentType.getInteger(context, "amount")
                                        );
                                        return 1;
                                    }))))))
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("vethenchant.admin"))
                    .executes(context -> {
                        this.plugin.reloadPluginConfig();
                        send(context.getSource().getSender(), this.plugin.messages().reload());
                        return 1;
                    }))
                .build(),
            "VethEnchant main command",
            List.of("ve")
        ));
    }

    private void sendInfo(CommandSender sender) {
        send(sender, "<aqua>VethEnchant</aqua> <gray>v" + this.plugin.getPluginMeta().getVersion() + "</gray>");
        send(sender, "<gray>Effects:</gray> <white>" + this.dispatcher.effectCount() + "</white>");
        send(sender, "<gray>Packet bridge:</gray> <white>" + this.plugin.packetBridge().status() + "</white>");
        send(sender, "<gray>Use:</gray> <white>/ve status</white><gray>, </gray><white>/ve list</white><gray>, </gray><white>/ve inspect <id></white><gray>, </gray><white>/ve givebook</white>");
    }

    private void sendStatus(CommandSender sender) {
        ConfigHealthReport report = this.plugin.vethConfig().healthReport();
        send(sender, "<primary>状态</primary> <muted>附魔 " + report.enabledEnchantments() + " 启用 / " + report.totalEnchantments() + " 总数</muted>");
        send(sender, "<muted>运行效果 </muted><primary>" + this.dispatcher.effectCount() + "</primary><muted> | PacketEvents </muted><primary>" + this.plugin.packetBridge().status() + "</primary>");
        send(sender, "<muted>槽位限制 </muted><primary>" + enabledText(this.plugin.vethConfig().limits().enabled())
            + "</primary><muted> | 注册表缺失 </muted><primary>" + report.missingRegistryEntries()
            + "</primary><muted> | 警告 </muted><primary>" + report.warningCount() + "</primary>");
        sendMap(sender, "类型", report.byType());
        sendMap(sender, "稀有度", report.byRarity());
        if (!report.healthy()) {
            send(sender, "<warning>发现配置提醒，可用 </warning><primary>/ve warnings</primary><warning> 查看。</warning>");
        }
    }

    private void sendList(CommandSender sender, String rawFilter) {
        String filter = normalizeFilter(rawFilter);
        List<EnchantDefinition> definitions = this.plugin.vethConfig().enchants().all().stream()
            .filter(definition -> matches(definition, filter))
            .sorted(Comparator
                .comparing(EnchantDefinition::type)
                .thenComparing(EnchantDefinition::rarity)
                .thenComparing(definition -> definition.key().asString()))
            .limit(LIST_PAGE_SIZE)
            .toList();

        long total = this.plugin.vethConfig().enchants().all().stream()
            .filter(definition -> matches(definition, filter))
            .count();

        send(sender, "<primary>附魔列表</primary> <muted>筛选 " + filter + "，显示 " + definitions.size() + "/" + total + "</muted>");
        for (EnchantDefinition definition : definitions) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(definition.key());
            String registryState = enchantment == null ? "<danger>未注册</danger>" : "<primary>已注册</primary>";
            send(sender, "<muted>- </muted><primary>" + definition.key().asString() + "</primary><muted> " + definition.name()
                + " | " + definition.type() + " | " + definition.rarity() + " | " + enabledText(definition.enabled())
                + " | </muted>" + registryState);
        }
        if (total > LIST_PAGE_SIZE) {
            send(sender, "<muted>列表较长，可用 </muted><primary>/ve list custom</primary><muted> 或稀有度筛选。</muted>");
        }
    }

    private void inspect(CommandSender sender, String rawId) {
        NamespacedKey key = parseKey(rawId);
        if (key == null) {
            send(sender, "<danger>附魔 ID 格式无效：</danger><muted>" + rawId + "</muted>");
            return;
        }
        Optional<EnchantDefinition> optional = this.plugin.vethConfig().enchants().find(key);
        if (optional.isEmpty()) {
            send(sender, "<danger>没有找到附魔配置：</danger><muted>" + rawId + "</muted>");
            return;
        }

        EnchantDefinition definition = optional.get();
        Enchantment enchantment = Registry.ENCHANTMENT.get(definition.key());
        send(sender, "<primary>" + definition.name() + "</primary> <muted>" + definition.key().asString() + "</muted>");
        send(sender, "<muted>类型 </muted><primary>" + definition.type() + "</primary><muted> | 稀有度 </muted><primary>" + definition.rarity()
            + "</primary><muted> | 状态 </muted><primary>" + enabledText(definition.enabled()) + "</primary>");
        if (enchantment == null) {
            send(sender, "<warning>当前注册表未找到这个附魔。新增或硬改 registry 后需要重启服务器。</warning>");
        } else {
            send(sender, "<muted>注册表 </muted><primary>等级 " + enchantment.getStartLevel() + "-" + enchantment.getMaxLevel()
                + "</primary><muted> | 权重 </muted><primary>" + enchantment.getWeight()
                + "</primary><muted> | 铁砧消耗 </muted><primary>" + enchantment.getAnvilCost() + "</primary>");
        }
        send(sender, "<muted>槽位组 </muted><primary>" + join(definition.groups()) + "</primary><muted> | 优先级 </muted><primary>" + definition.priority() + "</primary>");
        for (String line : definition.description()) {
            send(sender, "<muted>说明 </muted><text>" + line + "</text>");
        }
    }

    private void giveBook(CommandSender sender, String playerName, String rawId, int requestedLevel, int amount) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            send(sender, "<danger>玩家不在线：</danger><muted>" + playerName + "</muted>");
            return;
        }

        NamespacedKey key = parseKey(rawId);
        if (key == null) {
            send(sender, "<danger>附魔 ID 格式无效：</danger><muted>" + rawId + "</muted>");
            return;
        }

        Enchantment enchantment = Registry.ENCHANTMENT.get(key);
        if (enchantment == null) {
            send(sender, "<warning>注册表里还没有这个附魔：</warning><muted>" + key.asString() + "</muted><warning>。新增或硬修改后请重启服务器。</warning>");
            return;
        }

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, Math.max(1, Math.min(64, amount)));
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        int level = Math.max(enchantment.getStartLevel(), Math.min(requestedLevel, enchantment.getMaxLevel()));
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        this.plugin.tooltipService().normalize(book);

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(book);
        for (ItemStack item : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        send(sender, "<primary>已给予 </primary><text>" + player.getName() + "</text><primary> 附魔书：</primary><text>" + key.asString() + " " + level + " x" + amount + "</text>");
        if (!overflow.isEmpty()) {
            send(sender, "<warning>玩家背包已满，剩余附魔书掉落在玩家脚下。</warning>");
        }
    }

    private void sendWarnings(CommandSender sender, int page) {
        List<String> warnings = this.plugin.vethConfig().healthReport().warnings();
        if (warnings.isEmpty()) {
            send(sender, "<primary>配置检查通过，目前没有警告。</primary>");
            return;
        }
        int pages = Math.max(1, (int) Math.ceil(warnings.size() / (double) WARNING_PAGE_SIZE));
        int current = Math.max(1, Math.min(page, pages));
        int from = (current - 1) * WARNING_PAGE_SIZE;
        int to = Math.min(warnings.size(), from + WARNING_PAGE_SIZE);
        send(sender, "<warning>配置警告</warning> <muted>" + current + "/" + pages + "，共 " + warnings.size() + " 条</muted>");
        for (String warning : warnings.subList(from, to)) {
            send(sender, "<muted>- </muted><warning>" + warning + "</warning>");
        }
    }

    private void sendMap(CommandSender sender, String title, Map<String, Integer> values) {
        if (values.isEmpty()) {
            return;
        }
        String text = values.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", "));
        send(sender, "<muted>" + title + " </muted><primary>" + text + "</primary>");
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(this.miniMessage.deserialize(this.plugin.messages().prefix() + message, this.plugin.paletteResolver()));
    }

    private boolean matches(EnchantDefinition definition, String filter) {
        return switch (filter) {
            case "all" -> true;
            case "enabled" -> definition.enabled();
            case "disabled" -> !definition.enabled();
            case "custom", "vanilla" -> definition.type().equalsIgnoreCase(filter);
            default -> definition.rarity().equalsIgnoreCase(filter);
        };
    }

    private NamespacedKey parseKey(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        if (!value.contains(":")) {
            NamespacedKey custom = NamespacedKey.fromString("vethenchant:" + value);
            if (custom != null && this.plugin.vethConfig().enchants().find(custom).isPresent()) {
                return custom;
            }
            return NamespacedKey.fromString("minecraft:" + value);
        }
        return NamespacedKey.fromString(value);
    }

    private static String normalizeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "all";
        }
        return raw.toLowerCase(Locale.ROOT);
    }

    private static int parsePage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static String enabledText(boolean enabled) {
        return enabled ? "启用" : "关闭";
    }

    private static String join(List<String> values) {
        return values.isEmpty() ? "无" : String.join(", ", values);
    }

    private static void suggest(com.mojang.brigadier.suggestion.SuggestionsBuilder builder, String... values) {
        String remaining = builder.getRemainingLowerCase();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(value);
            }
        }
    }
}
