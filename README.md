# VethEnchant

VethEnchant 是 **VethCraft 糖糖工艺服务器专用插件**，用于提供服务器自定义附魔、附魔展示、附魔来源控制和附魔运行效果。

插件基于 Paper 原生附魔注册表开发，会在 bootstrap 阶段注册自定义附魔，并在运行时通过事件系统处理采矿、战斗、防御、农业和掉落类效果。

## 主要功能

- Paper 原生自定义附魔注册
- 自定义附魔书与附魔展示 Lore
- 附魔稀有度、来源和槽位限制
- 采矿类效果：矿脉、熔炼、归整、寻晶等
- 农业类效果：补种、丰收、青芽等
- 战斗类效果：顺劈、汲取、破防、狩猎等
- 防御类效果：庇护、御火、回息、箭护等
- WorldGuard / Residence 区域保护兼容
- CraftEngine / CustomCrops 相关兼容

## 架构说明

- 注册表定义：`EnchantCatalog`
- 运行效果接口：`VethEnchantEffect`
- 事件分发：`EffectDispatcher`
- 附魔配置：`plugins/VethEnchant/custom-enchant/*.yml`
- 原版附魔显示配置：`plugins/VethEnchant/vanilla-enchant/*.yml`

## 运行环境

- Minecraft / Paper API：`26.1.2`
- Java：`25`
- 服务端环境：VethCraft 糖糖工艺

## 注意

本插件按照 VethCraft 糖糖工艺的经济、附魔、资源世界和保护区规则设计，默认不保证适配其它服务器。

正式服运行配置、数据库、玩家数据、密钥和构建产物不会放入仓库。
