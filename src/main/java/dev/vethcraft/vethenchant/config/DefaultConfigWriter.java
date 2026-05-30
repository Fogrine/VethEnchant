package dev.vethcraft.vethenchant.config;

import dev.vethcraft.vethenchant.registry.EnchantCatalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class DefaultConfigWriter {

    private final Path dataDirectory;

    public DefaultConfigWriter(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void writeMissingFiles() throws IOException {
        Files.createDirectories(this.dataDirectory);
        writeIfMissing("rarity.yml", rarityYaml());
        writeIfMissing("limits.yml", limitsYaml());
        writeIfMissing("mechanisms/enchanting-table.yml", mechanismYaml("enchanting-table"));
        writeIfMissing("mechanisms/loot.yml", mechanismYaml("loot"));
        writeIfMissing("mechanisms/fishing.yml", mechanismYaml("fishing"));

        for (DefaultEnchant enchant : vanillaDefaults()) {
            writeIfMissing("vanilla-enchant/" + enchant.fileName() + ".yml", enchantYaml(enchant, "minecraft"));
        }
        for (DefaultEnchant enchant : customDefaults()) {
            writeIfMissing("custom-enchant/" + enchant.fileName() + ".yml", enchantYaml(enchant, EnchantCatalog.NAMESPACE));
        }
    }

    private void writeIfMissing(String relativePath, String content) throws IOException {
        Path path = this.dataDirectory.resolve(relativePath);
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private static String rarityYaml() {
        return """
            common:
              name: "普通"
              color: "#f8f4ed"
              weight: 1000
              tooltip-style: "server:tooltip/common"
              enchant-format: "<color:#f8f4ed>{name}</color> <white>{level}</white> <white>{icons}<white>"
            uncommon:
              name: "罕见"
              color: "#66c18c"
              weight: 500
              tooltip-style: "server:tooltip/uncommon"
              enchant-format: "<color:#66c18c>{name}</color> <white>{level}</white> <white>{icons}<white>"
            rare:
              name: "精良"
              color: "#63bbd0"
              weight: 250
              tooltip-style: "server:tooltip/rare"
              enchant-format: "<color:#63bbd0>{name}</color> <white>{level}</white> <white>{icons}<white>"
            epic:
              name: "史诗"
              color: "#eb507e"
              weight: 50
              tooltip-style: "server:tooltip/epic"
              enchant-format: "<color:#eb507e>{name}</color> <white>{level}</white> <white>{icons}<white>"
            legendary:
              name: "传奇"
              color: "#fba414"
              weight: 10
              tooltip-style: "server:tooltip/legendary"
              enchant-format: "<color:#fba414>{name}</color> <white>{level}</white> <white>{icons}<white>"
            splendid:
              name: "稀世"
              color: "#fbda41"
              weight: 5
              tooltip-style: "server:tooltip/splendid"
              enchant-format: "<color:#fbda41>{name}</color> <white>{level}</white> <white>{icons}<white>"
            curse:
              name: "诅咒"
              color: "#d42517"
              weight: 25
              tooltip-style: "server:tooltip/curse"
              enchant-format: "<color:#d42517>{name}</color> <white>{level}</white> <white>{icons}<white>"
            artifact:
              name: "皮肤"
              color: "#ec9bad"
              weight: 1
              tooltip-style: "server:tooltip/artifact"
              enchant-format: "<color:#ec9bad>{name}</color> <white>{level}</white> <white>{icons}<white>"
              inaccessible: true
            """;
    }

    private static String limitsYaml() {
        return """
            enabled: true
            bypass-permission: "vethenchant.limit.bypass"

            legacy-items:
              # allow-existing: Do not scan or delete old over-limit items. Only block adding more.
              # strict: Generated loot/fishing/enchanting output is trimmed to the configured limits.
              mode: "allow-existing"

            display:
              show-slots-in-lore: true
              format: "<muted>附魔槽位 </muted><primary>{used}</primary><muted>/</muted><primary>{max}</primary>"

            book-slots:
              default:
                max-slots: 6
              enchanting-table:
                max-slots: 6
              loot:
                max-slots: 6
              fishing:
                max-slots: 6
              villager:
                max-slots: 6
              anvil:
                max-slots: 6

            item-slots:
              default:
                max-slots: 6
              sword:
                max-slots: 7
              axe:
                max-slots: 7
              pickaxe:
                max-slots: 7
              shovel:
                max-slots: 5
              hoe:
                max-slots: 5
              bow:
                max-slots: 6
                group-limits:
                  damage: 1
                  area: 1
              crossbow:
                max-slots: 6
                group-limits:
                  damage: 1
                  area: 1
              trident:
                max-slots: 6
              mace:
                max-slots: 6
              helmet:
                max-slots: 6
              chestplate:
                max-slots: 6
              leggings:
                max-slots: 6
              boots:
                max-slots: 6
              elytra:
                max-slots: 4
              fishing_rod:
                max-slots: 4
              shears:
                max-slots: 3

            source-limits:
              enchanting-table:
                enabled: true
                max-enchants-added: 3
                max-custom-enchants: -1
                max-rare-or-above: -1
                overflow-policy: "trim-lowest-priority"
              loot:
                enabled: true
                enchanted-book:
                  max-slots: 3
                  max-custom-enchants: -1
                  max-rare-or-above: -1
                equipment:
                  use-item-slot-limit: true
                overflow-policy: "trim-lowest-priority"
              fishing:
                enabled: true
                enchanted-book:
                  max-slots: 2
                  max-custom-enchants: -1
                  max-rare-or-above: -1
                equipment:
                  use-item-slot-limit: true
                overflow-policy: "trim-lowest-priority"
              villager:
                enabled: true
                enchanted-book:
                  max-slots: 1
                  allow-custom-enchants: false
              anvil:
                enabled: false
                remove-prior-work-penalty: true
                max-repair-cost: 0
                overflow-policy: "allow"
            """;
    }

    private static String mechanismYaml(String name) {
        return """
            enabled: true
            # This file is reserved for future %s tuning. Runtime limits are in limits.yml.
            """.formatted(name);
    }

    private static String enchantYaml(DefaultEnchant enchant, String defaultNamespace) {
        String namespace = enchant.id().contains(":") ? enchant.id().split(":", 2)[0] : defaultNamespace;
        String id = enchant.id().contains(":") ? enchant.id() : namespace + ":" + enchant.id();
        return """
            id: "%s"
            type: "%s"
            enabled: %s

            display:
              name: "%s"
              rarity: "%s"
              icons:
            %s
              description:
                - "%s"

            registry:
              override: true
              restart-required: true

            sources:
              enchanting-table: true
              loot: true
              fishing: true
              villager: true
              mob-equipment: true
              treasure: false

            limits:
              groups:
            %s
              priority: %d

            effects:
              restart-required: true
              hard-modify: true
            """.formatted(
            id,
            enchant.type(),
            Boolean.toString(enchant.enabled()),
            enchant.name(),
            enchant.rarity(),
            iconLines(iconTags(enchant)),
            enchant.description(),
            groupLines(enchant.groups()),
            enchant.priority()
        );
    }

    private static String iconLines(List<String> icons) {
        if (icons.isEmpty()) {
            return "    []";
        }
        StringBuilder builder = new StringBuilder();
        for (String icon : icons) {
            builder.append("    - \"").append(icon).append("\"\n");
        }
        return builder.toString().stripTrailing();
    }

    private static String groupLines(List<String> groups) {
        if (groups.isEmpty()) {
            return "    []";
        }
        StringBuilder builder = new StringBuilder();
        for (String group : groups) {
            builder.append("    - \"").append(group).append("\"\n");
        }
        return builder.toString().stripTrailing();
    }

    private static List<String> iconTags(DefaultEnchant enchant) {
        return iconIds(enchant.id()).stream()
            .map(DefaultConfigWriter::iconTag)
            .toList();
    }

    private static String iconTag(String id) {
        return "<image:server:gui/menu/enchantments/items/" + id + ">";
    }

    private static List<String> iconIds(String rawId) {
        String id = rawId.contains(":") ? rawId.split(":", 2)[1] : rawId;
        return switch (id) {
            case "protection", "fire_protection", "blast_protection", "projectile_protection", "thorns",
                "aegis", "warding", "sentinel", "cleansing", "fireward", "frostguard", "blastguard",
                "arrowguard", "rooted" -> armorIcons();
            case "feather_falling", "depth_strider", "frost_walker", "soul_speed", "freerunner",
                "lightstep", "sprinter", "steady_feet" -> List.of("boots");
            case "respiration", "aqua_affinity", "block_breather", "nourishing", "aquatic", "night_owl" -> List.of("helmet");
            case "swift_sneak" -> List.of("leggings");
            case "binding_curse" -> List.of("helmet", "chestplate", "leggings", "boots", "elytra");
            case "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting",
                "abrasion", "cleave", "lifesteal", "executioner", "hunter", "beast_hunter", "raider_bane",
                "nether_bane", "end_bane", "opening_strike", "giant_slayer", "purifier", "stagger",
                "soul_siphon", "ferocity", "frostbite", "poison_tip", "weakening", "guard_breaker",
                "battle_focus", "scavenger", "pacify" -> List.of("sword", "axe");
            case "sweeping_edge" -> List.of("sword");
            case "efficiency", "silk_touch", "fortune", "compact", "soft_touch" -> toolIcons();
            case "unbreaking", "mending", "vanishing_curse" -> durableIcons();
            case "power", "punch", "flame", "infinity", "longshot", "steady_aim", "quickdraw", "close_quarters" -> List.of("bow", "cross_bow");
            case "luck_of_the_sea", "lure", "angler_luck", "quick_bite", "treasure_hook", "gentle_hook" -> List.of("fishing_rod");
            case "loyalty", "impaling", "riptide", "channeling", "tidecaller", "stormcast",
                "returning_current" -> List.of("trident", "spear");
            case "multishot", "quick_charge", "piercing", "ricochet", "scatterbolt" -> List.of("cross_bow");
            case "density", "breach", "wind_burst" -> List.of("mace");
            case "lunge" -> List.of("spear");
            case "replenish", "harvester", "green_thumb" -> List.of("hoe");
            case "telekinesis", "wisdom", "magnetism", "veteran" -> toolsAndWeaponsIcons();
            case "vein_miner", "tunneling", "smelter", "gemfinder", "stonebound", "quarry",
                "deep_delver", "prospector", "mineral_luck" -> List.of("pickaxe");
            case "excavator", "flinting", "clayfinder" -> List.of("shovel");
            case "timber", "lumber_bounty" -> List.of("axe");
            case "pinning", "frost_arrow", "hunter_arrow" -> List.of("bow", "cross_bow");
            case "vitality", "rebound", "second_wind", "bastion" -> List.of("chestplate");
            case "bulwark" -> List.of("shield");
            case "glider", "windglide", "safe_landing" -> List.of("elytra");
            default -> List.of();
        };
    }

    private static List<String> armorIcons() {
        return List.of("helmet", "chestplate", "leggings", "boots");
    }

    private static List<String> toolIcons() {
        return List.of("pickaxe", "axe", "shovel", "hoe", "shear");
    }

    private static List<String> toolsAndWeaponsIcons() {
        return List.of("sword", "axe", "pickaxe", "shovel", "hoe", "shear", "mace", "trident", "spear");
    }

    private static List<String> durableIcons() {
        return List.of(
            "sword", "axe", "pickaxe", "shovel", "hoe", "shear", "mace", "bow", "cross_bow",
            "trident", "spear", "helmet", "chestplate", "leggings", "boots", "elytra",
            "fishing_rod", "shield"
        );
    }

    private static List<DefaultEnchant> vanillaDefaults() {
        return List.of(
            vanilla("protection", "保护", "common", "降低多数伤害。", 90, "protection"),
            vanilla("fire_protection", "火焰保护", "common", "降低火焰和熔岩伤害。", 80, "protection"),
            vanilla("feather_falling", "摔落保护", "common", "降低摔落伤害。", 75, "protection", "mobility"),
            vanilla("blast_protection", "爆炸保护", "uncommon", "降低爆炸伤害。", 80, "protection"),
            vanilla("projectile_protection", "弹射物保护", "common", "降低远程弹射伤害。", 80, "protection"),
            vanilla("respiration", "水下呼吸", "uncommon", "延长水下呼吸。", 70, "survival"),
            vanilla("aqua_affinity", "水下速掘", "uncommon", "加快水下挖掘。", 65, "utility"),
            vanilla("thorns", "荆棘", "rare", "概率反弹近战伤害。", 70, "damage", "survival"),
            vanilla("depth_strider", "深海探索者", "uncommon", "提升水下移速。", 70, "mobility"),
            vanilla("frost_walker", "冰霜行者", "rare", "水面行走生成薄冰。", 70, "mobility"),
            vanilla("binding_curse", "绑定诅咒", "curse", "穿上后无法取下。", 20, "curse"),
            vanilla("soul_speed", "灵魂疾行", "rare", "提升灵魂方块移速。", 70, "mobility"),
            vanilla("swift_sneak", "迅捷潜行", "rare", "提升潜行移速。", 70, "mobility"),
            vanilla("sharpness", "锋利", "common", "提升近战伤害。", 100, "damage", "melee"),
            vanilla("smite", "亡灵杀手", "common", "提升对亡灵伤害。", 95, "damage", "melee"),
            vanilla("bane_of_arthropods", "节肢杀手", "common", "提升对节肢伤害。", 95, "damage", "melee"),
            vanilla("knockback", "击退", "uncommon", "提升近战击退。", 65, "control"),
            vanilla("fire_aspect", "火焰附加", "uncommon", "命中附加燃烧。", 70, "damage", "fire"),
            vanilla("looting", "抢夺", "rare", "提升生物掉落。", 90, "economy"),
            vanilla("sweeping_edge", "横扫之刃", "uncommon", "提升横扫伤害。", 70, "damage", "area"),
            vanilla("efficiency", "效率", "common", "提升挖掘速度。", 85, "utility"),
            vanilla("silk_touch", "精准采集", "rare", "方块原样掉落。", 90, "economy"),
            vanilla("unbreaking", "耐久", "common", "降低耐久消耗。", 95, "sustain"),
            vanilla("fortune", "时运", "rare", "提升方块掉落。", 90, "economy"),
            vanilla("power", "力量", "common", "提升弓箭伤害。", 100, "damage", "ranged"),
            vanilla("punch", "冲击", "uncommon", "提升箭矢击退。", 65, "control"),
            vanilla("flame", "火矢", "uncommon", "箭矢点燃目标。", 70, "damage", "fire"),
            vanilla("infinity", "无限", "rare", "普通箭不消耗。", 90, "sustain"),
            vanilla("luck_of_the_sea", "海之眷顾", "uncommon", "提升宝藏咬钩率。", 70, "economy"),
            vanilla("lure", "饵钓", "common", "缩短上钩时间。", 65, "utility"),
            vanilla("loyalty", "忠诚", "uncommon", "三叉戟自动返回。", 70, "utility"),
            vanilla("impaling", "穿刺", "uncommon", "提升水生目标伤害。", 90, "damage"),
            vanilla("riptide", "激流", "rare", "雨水中投掷冲刺。", 80, "mobility"),
            vanilla("channeling", "引雷", "rare", "雷暴命中召雷。", 75, "damage"),
            vanilla("multishot", "多重射击", "rare", "弩一次射出三发。", 80, "damage", "area"),
            vanilla("quick_charge", "快速装填", "uncommon", "缩短弩装填时间。", 70, "utility"),
            vanilla("piercing", "穿透", "uncommon", "弩箭穿透目标。", 75, "damage", "area"),
            vanilla("mending", "经验修补", "epic", "经验修复耐久。", 100, "sustain"),
            vanilla("vanishing_curse", "消失诅咒", "curse", "死亡后物品消失。", 20, "curse"),
            vanilla("density", "致密", "rare", "提升重锤坠击伤害。", 90, "damage"),
            vanilla("breach", "破甲", "rare", "重锤攻击削弱护甲。", 90, "damage"),
            vanilla("wind_burst", "风爆", "epic", "重锤命中触发风爆。", 85, "mobility", "area"),
            vanilla("lunge", "突刺", "rare", "提升冲刺攻击表现。", 80, "mobility", "damage")
        );
    }

    private static List<DefaultEnchant> customDefaults() {
        return List.of(
            custom("abrasion", "磨蚀", "uncommon", "概率磨损目标护甲。", 70, "damage", "utility"),
            custom("replenish", "催生", "rare", "收获成熟作物后自动补种。", 75, "farm", "utility"),
            custom("telekinesis", "吸星", "rare", "方块掉落自动进背包。", 70, "utility", "economy"),
            custom("wisdom", "智识", "rare", "提升方块经验。", 70, "economy"),
            custom("freerunner", "轻盈", "uncommon", "概率减免摔落。", 65, "mobility", "survival"),
            custom("block_breather", "无氧", "uncommon", "概率抵消窒息。", 65, "survival"),
            custom("vein_miner", "矿脉", "rare", "连锁采集相连矿石。", 85, "area", "economy"),
            custom("tunneling", "掘进", "rare", "挖掘时破坏小范围方块。", 80, "area"),
            custom("excavator", "掘土", "uncommon", "铲类工具小范围掘土。", 75, "area"),
            custom("timber", "伐木", "rare", "连锁砍伐树干。", 80, "area", "economy"),
            custom("harvester", "丰收", "uncommon", "范围收割成熟作物。", 75, "farm", "area"),
            custom("smelter", "熔炼", "rare", "矿物掉落自动熔炼。", 70, "utility", "economy"),
            custom("gemfinder", "寻晶", "rare", "挖矿时小概率额外宝石。", 65, "economy"),
            custom("stonebound", "坚岩", "uncommon", "挖掘石类时更耐用。", 60, "sustain"),
            custom("quarry", "采场", "epic", "更大的采矿范围，默认较稀有。", 90, "area"),
            custom("magnetism", "磁引", "uncommon", "吸引附近掉落物。", 60, "utility"),
            custom("compact", "归整", "rare", "采集后尝试压缩材料。", 55, "utility", "economy"),
            custom("green_thumb", "青芽", "uncommon", "作物收获时概率加速附近作物。", 60, "farm"),
            custom("prospector", "探矿", "uncommon", "挖矿时额外提升经验。", 68, "economy"),
            custom("mineral_luck", "矿运", "rare", "挖矿时小概率掉落额外矿物。", 72, "economy"),
            custom("flinting", "燧取", "uncommon", "碎石类方块更容易出燧石。", 62, "economy"),
            custom("clayfinder", "寻陶", "uncommon", "黏土和沙地更容易出黏土球。", 60, "economy"),
            custom("lumber_bounty", "木馈", "uncommon", "砍树时小概率获得额外掉落。", 64, "economy"),
            custom("deep_delver", "深掘", "rare", "低处挖矿获得少量额外经验。", 65, "economy"),
            custom("soft_touch", "轻采", "uncommon", "采集时降低工具损耗。", 55, "sustain"),
            custom("cleave", "顺劈", "rare", "近战命中溅射附近怪物。", 90, "damage", "area"),
            custom("lifesteal", "汲取", "rare", "攻击怪物时少量回复生命。", 85, "damage", "sustain"),
            custom("executioner", "斩决", "rare", "目标低血量时造成额外伤害。", 88, "damage"),
            custom("hunter", "狩猎", "uncommon", "对普通怪物伤害提高。", 82, "damage"),
            custom("beast_hunter", "兽猎", "uncommon", "对生物系怪物伤害提高。", 82, "damage"),
            custom("raider_bane", "破袭", "uncommon", "对袭击者伤害提高。", 78, "damage"),
            custom("nether_bane", "狱猎", "uncommon", "对下界怪物伤害提高。", 76, "damage"),
            custom("end_bane", "末猎", "uncommon", "对末地怪物伤害提高。", 76, "damage"),
            custom("opening_strike", "破势", "uncommon", "开场命中更容易打出高伤。", 80, "damage"),
            custom("giant_slayer", "巨猎", "rare", "对高血量目标伤害提高。", 84, "damage"),
            custom("purifier", "净刃", "uncommon", "对亡灵目标更强。", 78, "damage"),
            custom("stagger", "踉跄", "uncommon", "命中后可能削弱目标。", 72, "control"),
            custom("soul_siphon", "魂汲", "rare", "命中怪物时小概率回血。", 80, "damage", "sustain"),
            custom("ferocity", "凶猛", "rare", "概率造成一次轻额外伤害。", 84, "damage"),
            custom("frostbite", "霜咬", "uncommon", "命中后短暂减速目标。", 70, "control"),
            custom("poison_tip", "淬毒", "uncommon", "命中后短暂中毒。", 72, "damage"),
            custom("weakening", "虚弱", "uncommon", "命中后短暂虚弱目标。", 68, "control"),
            custom("guard_breaker", "破防", "epic", "对高护甲目标稍强。", 88, "damage"),
            custom("battle_focus", "战意", "uncommon", "连续战斗获得轻微增伤。", 78, "damage"),
            custom("longshot", "远矢", "uncommon", "远距离箭矢伤害提高。", 80, "damage", "ranged"),
            custom("steady_aim", "稳弦", "uncommon", "弓箭伤害更稳定。", 72, "damage", "ranged"),
            custom("close_quarters", "近射", "uncommon", "贴脸射击更有力。", 74, "damage", "ranged"),
            custom("pinning", "钉足", "rare", "远程命中概率减速。", 74, "control", "ranged"),
            disabledCustom("ricochet", "弹射", "epic", "弩箭概率弹向附近怪物。", 85, "damage", "area"),
            custom("frost_arrow", "霜箭", "uncommon", "箭矢短暂减速目标。", 70, "control", "ranged"),
            disabledCustom("quickdraw", "疾射", "uncommon", "短时间内提升弓箭节奏。", 66, "utility", "ranged"),
            disabledCustom("scatterbolt", "散弩", "epic", "弩命中时轻微溅射。", 84, "damage", "area"),
            custom("hunter_arrow", "猎矢", "uncommon", "对怪物的箭矢伤害提高。", 78, "damage", "ranged"),
            disabledCustom("vitality", "活力", "uncommon", "受到治疗时效果略高。", 65, "survival"),
            custom("aegis", "庇护", "rare", "受到伤害时概率减免。", 80, "protection"),
            custom("warding", "守御", "uncommon", "降低部分怪物伤害。", 76, "protection"),
            custom("sentinel", "哨卫", "rare", "降低多种常见伤害。", 82, "protection"),
            custom("cleansing", "清心", "rare", "降低负面效果伤害。", 78, "protection"),
            custom("fireward", "御火", "uncommon", "降低燃烧持续影响。", 68, "protection"),
            custom("frostguard", "御寒", "uncommon", "降低冰冻和减速影响。", 62, "protection"),
            custom("bastion", "坚垒", "rare", "正面近战防护更稳。", 80, "protection"),
            custom("bulwark", "盾壁", "rare", "盾牌与正面防护更强。", 82, "protection"),
            custom("blastguard", "震护", "rare", "降低爆炸伤害。", 80, "protection"),
            custom("arrowguard", "箭护", "rare", "降低远程箭矢伤害。", 80, "protection"),
            custom("rooted", "扎根", "uncommon", "下蹲时防护更稳。", 64, "protection"),
            custom("rebound", "回震", "rare", "受近战攻击时轻微反击。", 74, "damage", "protection"),
            custom("second_wind", "回息", "epic", "低血量时获得短暂恢复。", 82, "survival"),
            disabledCustom("nourishing", "滋养", "uncommon", "饥饿消耗略微降低。", 58, "survival"),
            custom("lightstep", "轻步", "uncommon", "移动更轻巧，降低部分摔落。", 64, "mobility"),
            disabledCustom("sprinter", "疾行", "rare", "短跑时获得轻微速度。", 66, "mobility"),
            disabledCustom("aquatic", "亲水", "uncommon", "水下行动更舒适。", 62, "survival", "mobility"),
            disabledCustom("night_owl", "夜行", "rare", "夜晚探索获得清晰视野。", 55, "utility"),
            custom("steady_feet", "稳足", "uncommon", "降低击退影响。", 63, "protection"),
            disabledCustom("glider", "滑翔", "rare", "鞘翅飞行更省耐久。", 62, "mobility", "sustain"),
            disabledCustom("angler_luck", "渔运", "uncommon", "钓鱼获得宝物概率略高。", 68, "economy"),
            disabledCustom("quick_bite", "快咬", "uncommon", "缩短钓鱼等待时间。", 64, "utility"),
            disabledCustom("treasure_hook", "宝钩", "rare", "钓鱼时更容易出现稀有收获。", 70, "economy"),
            disabledCustom("gentle_hook", "柔钩", "common", "钓鱼竿更耐用。", 50, "sustain"),
            custom("tidecaller", "唤潮", "rare", "三叉戟对水中目标更强。", 78, "damage"),
            custom("stormcast", "掷雷", "epic", "雷雨中投掷三叉戟更强。", 82, "damage"),
            disabledCustom("returning_current", "回流", "uncommon", "三叉戟返回更稳定。", 60, "utility"),
            disabledCustom("windglide", "乘风", "rare", "鞘翅飞行时更顺滑。", 64, "mobility"),
            custom("safe_landing", "稳降", "uncommon", "落地伤害降低。", 66, "survival"),
            custom("scavenger", "拾荒", "uncommon", "击杀怪物时小概率额外掉落。", 76, "economy"),
            custom("veteran", "老练", "rare", "获得经验略微提高。", 70, "economy"),
            custom("pacify", "安抚", "uncommon", "攻击后概率削弱怪物攻击。", 68, "control")
        );
    }

    private static DefaultEnchant vanilla(String id, String name, String rarity, String description, int priority, String... groups) {
        return new DefaultEnchant(id, id, "vanilla", true, name, rarity, description, List.of(groups), priority);
    }

    private static DefaultEnchant custom(String id, String name, String rarity, String description, int priority, String... groups) {
        return new DefaultEnchant(id, id, "custom", true, name, rarity, description, List.of(groups), priority);
    }

    private static DefaultEnchant disabledCustom(String id, String name, String rarity, String description, int priority, String... groups) {
        return new DefaultEnchant(id, id, "custom", false, name, rarity, description + "（暂未开放运行效果）", List.of(groups), priority);
    }

    private record DefaultEnchant(
        String fileName,
        String id,
        String type,
        boolean enabled,
        String name,
        String rarity,
        String description,
        List<String> groups,
        int priority
    ) {
    }
}
