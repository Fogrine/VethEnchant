package dev.vethcraft.vethenchant;

import dev.vethcraft.vethenchant.command.VethEnchantCommand;
import dev.vethcraft.vethenchant.config.MessageConfig;
import dev.vethcraft.vethenchant.config.VethEnchantConfig;
import dev.vethcraft.vethenchant.effect.EffectDispatcher;
import dev.vethcraft.vethenchant.effect.builtin.AbrasionEffect;
import dev.vethcraft.vethenchant.effect.builtin.AreaHarvestEffect;
import dev.vethcraft.vethenchant.effect.builtin.BlockBreatherEffect;
import dev.vethcraft.vethenchant.effect.builtin.FreeRunnerEffect;
import dev.vethcraft.vethenchant.effect.builtin.PveCombatEffect;
import dev.vethcraft.vethenchant.effect.builtin.ReplenishEffect;
import dev.vethcraft.vethenchant.effect.builtin.SurvivalArmorEffect;
import dev.vethcraft.vethenchant.effect.builtin.TelekinesisEffect;
import dev.vethcraft.vethenchant.effect.builtin.MiningUtilityEffect;
import dev.vethcraft.vethenchant.effect.builtin.ToolUtilityEffect;
import dev.vethcraft.vethenchant.effect.builtin.WisdomEffect;
import dev.vethcraft.vethenchant.limit.EnchantLimitService;
import dev.vethcraft.vethenchant.listener.EnchantLimitListener;
import dev.vethcraft.vethenchant.listener.CraftEngineReloadListener;
import dev.vethcraft.vethenchant.listener.CustomCropsReloadListener;
import dev.vethcraft.vethenchant.listener.TooltipListener;
import dev.vethcraft.vethenchant.packet.PacketBridge;
import dev.vethcraft.vethenchant.packet.PacketEventsBridge;
import dev.vethcraft.vethenchant.packet.UnavailablePacketBridge;
import dev.vethcraft.vethenchant.protection.PlacedBlockTracker;
import dev.vethcraft.vethenchant.protection.ProtectionService;
import dev.vethcraft.vethenchant.registry.EnchantCatalog;
import dev.vethcraft.vethenchant.tooltip.EnchantTooltipService;
import dev.vethcraft.vethenchant.util.CustomCropService;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class VethEnchantPlugin extends JavaPlugin {

    private VethEnchantConfig vethConfig;
    private MessageConfig messages;
    private EffectDispatcher dispatcher;
    private PacketBridge packetBridge;
    private EnchantTooltipService tooltipService;
    private EnchantLimitService limitService;
    private ProtectionService protectionService;
    private PlacedBlockTracker placedBlockTracker;
    private CustomCropService customCropService;

    @Override
    public void onEnable() {
        this.vethConfig = new VethEnchantConfig(this);
        this.vethConfig.reload();
        this.messages = MessageConfig.load(getConfig());

        this.protectionService = new ProtectionService(this);
        this.placedBlockTracker = new PlacedBlockTracker(this);
        this.placedBlockTracker.enable();
        this.customCropService = new CustomCropService(this);
        this.customCropService.reload();
        this.dispatcher = new EffectDispatcher(this);
        registerBuiltInEffects();
        this.dispatcher.registerListeners();

        this.limitService = new EnchantLimitService(this.vethConfig);
        getServer().getPluginManager().registerEvents(new EnchantLimitListener(this, this.limitService), this);

        this.tooltipService = new EnchantTooltipService(this);
        getServer().getPluginManager().registerEvents(new TooltipListener(this, this.tooltipService), this);
        if (getServer().getPluginManager().isPluginEnabled("CustomCrops")) {
            getServer().getPluginManager().registerEvents(new CustomCropsReloadListener(this), this);
        }
        if (getServer().getPluginManager().isPluginEnabled("CraftEngine")) {
            getServer().getPluginManager().registerEvents(new CraftEngineReloadListener(this), this);
        }

        this.packetBridge = createPacketBridge();
        this.packetBridge.enable();

        VethEnchantCommand command = new VethEnchantCommand(this, this.dispatcher);
        command.register();

        getLogger().info("VethEnchant enabled with " + this.dispatcher.effectCount() + " runtime effects.");
    }

    @Override
    public void onDisable() {
        if (this.packetBridge != null) {
            this.packetBridge.disable();
        }
        if (this.placedBlockTracker != null) {
            this.placedBlockTracker.disable();
        }
    }

    public void reloadPluginConfig() {
        this.vethConfig.reload();
        this.messages = MessageConfig.load(getConfig());
        if (this.protectionService != null) {
            this.protectionService.reload();
        }
        if (this.placedBlockTracker != null) {
            this.placedBlockTracker.reload();
        }
        if (this.customCropService != null) {
            this.customCropService.reload();
        }
        if (this.tooltipService != null) {
            this.tooltipService.reload();
        }
        if (this.packetBridge != null) {
            this.packetBridge.disable();
            this.packetBridge = createPacketBridge();
            this.packetBridge.enable();
        }
    }

    public boolean isEnchantEnabled(String id) {
        return isEnchantEnabled(new NamespacedKey(EnchantCatalog.NAMESPACE, id));
    }

    public boolean isEnchantEnabled(NamespacedKey key) {
        return this.vethConfig == null || this.vethConfig.isEnchantEnabled(key);
    }

    public VethEnchantConfig vethConfig() {
        return this.vethConfig;
    }

    public MessageConfig messages() {
        return this.messages;
    }

    public EffectDispatcher dispatcher() {
        return this.dispatcher;
    }

    public PacketBridge packetBridge() {
        return this.packetBridge;
    }

    public EnchantTooltipService tooltipService() {
        return this.tooltipService;
    }

    public EnchantLimitService limitService() {
        return this.limitService;
    }

    public ProtectionService protectionService() {
        return this.protectionService;
    }

    public PlacedBlockTracker placedBlockTracker() {
        return this.placedBlockTracker;
    }

    public CustomCropService customCropService() {
        return this.customCropService;
    }

    public TagResolver paletteResolver() {
        return TagResolver.resolver(
            Placeholder.styling("primary", color("palette.primary", "#63bbd0")),
            Placeholder.styling("warning", color("palette.warning", "#fbda41")),
            Placeholder.styling("danger", color("palette.danger", "#d42517")),
            Placeholder.styling("muted", color("palette.muted", "#8A8F98")),
            Placeholder.styling("text", color("palette.text", "#f8f4ed"))
        );
    }

    private void registerBuiltInEffects() {
        this.dispatcher.register(new AbrasionEffect());
        this.dispatcher.register(new ReplenishEffect(this));
        this.dispatcher.register(new TelekinesisEffect(this));
        this.dispatcher.register(new WisdomEffect());
        this.dispatcher.register(new FreeRunnerEffect());
        this.dispatcher.register(new BlockBreatherEffect());
        for (String id : List.of("vein_miner", "tunneling", "excavator", "timber", "harvester", "quarry")) {
            this.dispatcher.register(new AreaHarvestEffect(this, id));
        }
        for (String id : List.of(
            "green_thumb", "stonebound", "soft_touch", "deep_delver", "prospector", "mineral_luck",
            "flinting", "clayfinder", "lumber_bounty"
        )) {
            this.dispatcher.register(new MiningUtilityEffect(this, id));
        }
        for (String id : List.of("smelter", "gemfinder", "magnetism", "compact")) {
            this.dispatcher.register(new ToolUtilityEffect(this, id));
        }
        for (String id : List.of(
            "cleave", "lifesteal", "executioner", "hunter", "beast_hunter", "raider_bane",
            "nether_bane", "end_bane", "opening_strike", "giant_slayer", "purifier", "stagger",
            "soul_siphon", "ferocity", "frostbite", "poison_tip", "weakening", "guard_breaker",
            "battle_focus", "longshot", "steady_aim", "close_quarters", "pinning", "frost_arrow",
            "hunter_arrow", "tidecaller", "stormcast", "scavenger", "veteran", "pacify"
        )) {
            this.dispatcher.register(new PveCombatEffect(this, id));
        }
        for (String id : List.of(
            "aegis", "warding", "sentinel", "cleansing", "fireward", "frostguard", "bastion",
            "bulwark", "blastguard", "arrowguard", "rooted", "rebound", "second_wind",
            "lightstep", "safe_landing", "steady_feet"
        )) {
            this.dispatcher.register(new SurvivalArmorEffect(id));
        }
    }

    private PacketBridge createPacketBridge() {
        if (!getConfig().getBoolean("packet-events.enabled", getConfig().getBoolean("settings.packet-events.enabled", true))) {
            return new UnavailablePacketBridge("disabled in config");
        }
        if (getServer().getPluginManager().getPlugin("packetevents") == null) {
            return new UnavailablePacketBridge("packetevents not installed");
        }
        return new PacketEventsBridge(this, EnchantCatalog.NAMESPACE);
    }

    private TextColor color(String path, String fallback) {
        TextColor color = TextColor.fromHexString(getConfig().getString(path, fallback));
        return color == null ? TextColor.color(0xFFFFFF) : color;
    }
}
