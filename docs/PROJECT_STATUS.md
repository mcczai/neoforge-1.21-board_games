# Card Duel · 项目状态总览与框架梳理

> 本文件是项目进度的**唯一总索引**：已完成 / 未测试 / 未完成 / 计划 四类状态 + 代码框架梳理 + 关键决策记录。
> 详细设计见同目录 `CARD_DESIGN.md`（卡池设计+技能草案+交互规划），测试细节见 `P1_TEST.md`（测试清单+B1-B20 风险）。
> 最后更新：卡面视觉规范定稿（§10：框/布局/tooltip/卡背/空槽）；卡池平衡版 v2（53 张）；回合上限与双端同步优化设计定稿（`CARD_DESIGN.md` §9.5）。

---

## 一、项目概况

| 项 | 值 |
|---|---|
| modid / 包名 | `cardduel` / `net.mcczai.cardduel` |
| 平台 | Minecraft 1.21.1 / NeoForge 21.1.220 |
| 工作区 / 分支 | `D:\cyd\board_games` / `bug_version` |
| origin | github.com/mcczai/board_games |
| 定位 | 玩家自定义卡牌对战 mod（卡包数据驱动，玩家可自行添加卡牌） |
| 文档目录 | `docs/`（本文件 + `CARD_DESIGN.md` + `P1_TEST.md`）；根目录保留 `README.md`/`issues.md`/`LICENSE` |

**构建与提交约束（本机环境铁律）**：
- 编译：`.\gradlew.bat compileJava --console=plain`（workdir `D:\cyd\board_games`），必须带 `sandbox_permissions: danger-full-access`（Gradle 写 `C:\Users\user\.gradle`，用户已批准）
- 提交身份：一律 `git -c user.name="DeepSeek Agent" -c user.email="deepseek-agent@noreply.invalid" commit`，不改仓库 git 配置（P0 提交 `3b2eabd` 为 mcczai 身份，用户认可保留）
- 工作流：每阶段先出**逐文件改动清单** → 用户确认 → 才写代码；bug 报告视为修复指令可直接修

---

## 二、代码框架梳理

### 2.1 模块划分（按包）

| 包 | 职责 | 关键类 |
|---|---|---|
| 根 `CardduelMod` | 注册入口：方块/物品/BE/数据组件/创造栏/菜单/attachment 注册 + 网络注册 + 双配置 + 默认卡包注册 | `CardduelMod`（`MODID`/`DEFAULT_PACK`/`registerDefaultExtraCardPack`） |
| `API/` | 对外 API：卡索引查询 | `CdAPI`（`getCommonCardIndex`/`getAllCommonCardIndex` 服务端；客户端版同构） |
| `API/item/` | 卡牌类型/派系枚举、卡数据读写 | `CardTabType`（trap/mana/equip/summon）、`CardTribe`（ender/nature/monster/ocean，**待 P2 更新为 6 派系**）、`nbt/CardDataAccessor`（卡数据组件读写默认方法） |
| `items/` | 卡牌物品与构建 | `ICard`（卡牌接口）、`AbstractCardItem`（按类型填充创造栏）、`builder/CardItemBuilder`（**从注册表建卡**，重置数值用）、`component/CardDataComponent`（hp/mp/atk/type/skill/tribe + `CARD_ID` + `IN_DUEL`） |
| `resources/` | **卡包数据驱动核心**（服务端） | `CommonCardPackLoader`/`CardAssetManager`/`CommonCardIndex`/POJO（`CardIndexPOJO`/`CardDataPOJO`/`PackInfoPOJO`）、`DefaultAssets`（`COIN_CARD_ID`/`EMPTY_CARD_ID`） |
| `client/resource/` | 卡包数据驱动（客户端镜像） | `ClientCardPackLoader`/`ClientAssetManager`/纹理加载（FilePack/ZipPack）/语言/序列化 |
| `client/duel/` | 对局客户端状态与交互 | `ClientDuelState`/`ClientDuelHand`/`ClientDuelTrap`（同步缓存）、`DuelInteraction`（选中状态）、`HudClickManager`（点击命中+分支路由）、`DuelCameraManager`（俯视相机）、`DuelSeatLock`（座位锁定） |
| `client/hud/` | 对局 HUD | `BattleBoardHud`（双方状态条/回合/提示/按钮）、`DuelHandHud`（手牌渲染） |
| `client/gui/` | 界面 | `DuelSetupScreen`（开局设置）、`CardBagScreen`（卡包袋） |
| `block/` + `block/entity/` | 牌桌 | `DuelTableBlock`（`DOUBLE` 双桌状态）、`DuelTableBlockEntity`（**对局数据宿主**：hostData/guestData、phase、manaCap/hpCap、NBT 三通道） |
| `duel/` | **对局核心（服务端权威）** | `DuelEngine`（入座/开局/回合/出牌/攻击/四类结算/秘密/内建技能）、`DuelPlayerData`、`DuelPhase`（IDLE/SETUP/WAITING/MULLIGAN/PLAYING/FINISHED）、`DuelSeat`、`SkillHooks`（6 个空钩子，P2） |
| `network/` | 网络 | `DuelNet`（playToServer 注册+处理器）+ payload 10 个（见 2.3） |
| `config/` | 配置 | `CommonConfig`（`DefaultPackDebug`）、`DuelConfig`（SERVER：`trapZoneLimit` 1-8 默认 3；**P2 增 `maxTurnLimit` 默认 50 建议 10-200**） |
| `init/` | 注册表 | `ModBlocks`/`ModItem`/`ModBlockEntities`/`ModDataComponents`/`ModAttachments`（`DUEL_SEAT`）/`ModMenuType` |
| 其他 | 命令/事件 | `command/DuelCommands`（`/duel endturn|status|leave`）、`event/DuelPlayerEvents`（掉线判负）、`skill/SkillHooks` |

### 2.2 核心数据流

1. **卡牌创建**：卡包资源（jar 内 `default_card_pack` + config 自定义包）→ `Common/ClientCardPackLoader` → 卡索引（name/data/stack_size/tooltip/type/texture）→ `CardItemBuilder` 写入数据组件 → 进卡包袋/手牌
2. **对局状态**：全部存 `DuelTableBlockEntity`（双方 `DuelPlayerData`：hand≤8/deck≤27/discard/trapZone/board[7]/equipped[7]/summonTurn/attackTurn/hp/mp/mpMax/turnCount/fatigue/deckReady/mulliganDone/totemActive）
3. **同步三通道**：
   - BE `getUpdateTag`（`savePublic`：不含手牌与陷阱内容；装备为公开信息随同步）
   - `ClientboundDuelSyncPayload`（公开视图：hp/mp/牌库/疲劳/**trapCount/totemActive**）
   - `ClientboundDuelHandPayload` + `ClientboundDuelTrapPayload`（**私有定向发本人**）
4. **操作闭环**：客户端点击（`HudClickManager` 按 `selectedHandKind()` 分支）→ `Serverbound*Payload` → 服务端按座位 attachment 定位牌桌 → `DuelEngine` 结算 → `syncToPlayers`/`broadcast`

### 2.3 四类结算路径（P1-5，已实现）

| 类型 | 结算 | 内建技能（P1 子集） |
|---|---|---|
| 召唤 summon | 占己方战场槽（召唤失调/每回合一次攻击） | 无（白板，P2 接 SkillHooks） |
| 魔法 mana | 立即结算 → 进弃牌堆（不占槽） | `heal_N`/`harm_N`/`fire_N`/`lava_N`/`draw_N`/`mana_N`/`golden_heal`/`pearl_strike`/`totem_protect` |
| 陷阱 trap | `secret_*`→秘密区（上限 `DuelConfig.trapZoneLimit`）；`anvil_N`→点选敌方召唤物即伤；`*_thorns`→占槽反伤 | `secret_mine/arrow/wither/sculk/tnt` + `anvil_N` + `cactus_thorns`/`magma_burn` |
| 装备 equip | 附着己方召唤物（每槽 1 件，替换重置旧装备）；atk=攻击加成；hp=**耐久**（任意伤害-1，归零重置进弃牌堆）；召唤物死亡→卡与装备均 `freshCard()` 重置进弃牌堆 | 机制内建：`equip_guard` 减伤/`equip_ranged` 免反击/`equip_elytra` 冲锋+免陷阱/`equip_trident` 风怒/`equip_taunt` 嘲讽 |

未知 skill 一律按召唤卡站场兜底（默认包羊卡兼容）。

---

## 三、已完成

| 阶段 | 提交 | 内容 |
|---|---|---|
| P0 | `3b2eabd`（已在 origin） | issues 清理、namespace 正则统一 `[a-z0-9_.-]`、`ModBlockEntities.build` 参数修复 |
| P1-1a | `e2499ac` | 入座/设置界面/座位 attachment/牌桌 NBT/卡包提交 |
| 修复×4 | `9ec069f` `cb022db` `217250e` `1c84fbe` | 离座 NPE（`removeData`）/单桌拦截+破桌清座/设置界面取消按钮/防刷屏 |
| P1-1b | `fc9b870` | 回合引擎/疲劳/胜负/掉线判负/`/duel` 命令/SkillHooks 预留 |
| P1-2 | `c94fbfc` | 俯视相机（AT）/座位锁定/HUD 手牌与状态条/结束回合按钮/桌面渲染几何修复 |
| P1-3+4 | `f3d4a0d` | 出牌/攻击（互伤+打脸+召唤失调+回合一次）/真实换牌/后手硬币 |
| 测试文档 | `b6e717a` | P1_TEST.md 初版（B1-B15） |
| P1-5a | `c87af13` | **四类结算区分** + 秘密区配置 + 内建技能最小集 + CARD_DESIGN.md |
| P1-5b | `834e423` | 装备规则 v2：耐久按受击次数-1、损坏/死亡重置、装备机制内建 5 个、嘲讽规则 |
| P1-5c | `717bf96` | 装备规则 v3：**任意伤害**磨损耐久（统一入 `applyCardDamage`）+ 技能数据驱动架构草案 |
| 文档整理 | 本提交 | docs/ 目录 + PROJECT_STATUS.md |

**P1 目标已全部完成**（goal 已 complete），全部提交编译通过；P1-1a/P1-1b 用户已实测通过。

---

## 四、未测试（代码已完成，待实测）

- **P1-2 / P1-3 / P1-4 / P1-5**：全部未实测。阻塞原因：① 用户缺第二客户端 ② 默认卡包只有 1 张羊（trap/skill=0，只能测站场兜底路径），四类结算需要 53 张卡池数据
- 重点风险：B1（屏幕→桌面映射 Z 符号）、B2（AT 方法名崩溃）、B16-B20（四类结算边界）；全表见 `P1_TEST.md`
- 测试环境：双客户端（`gradlew runServer` + 两客户端，或局域网联机）；测试顺序见 `P1_TEST.md` 第 5 节

---

## 五、未完成（P2 清单）

1. **53 张卡包 JSON + 贴图**（`CARD_DESIGN.md` 第 5 节卡表，平衡版 v2）——解锁四类结算实测的关键
2. **数据驱动技能系统**（`CARD_DESIGN.md` 第 9 节草案：技能 JSON/SkillRegistry/SkillDispatcher/effect 扩展点）——用户已确认 P2 实施，P2 开局第一项
3. **拖拽施法交互**（`CARD_DESIGN.md` 第 8 节：长按拖动+箭头指示，炉石式目标选择）
4. `CardDataPOJO`/`CardIndexPOJO` 加 `rarity` 字段 + 工具提示稀有度/彩色边框
5. `CardTribe` 枚举更新为 6 派系（nature/nether/end/void/redstone/steve）+ 语言文件
6. 召唤卡技能（战吼/亡语/嘲讽/冲锋/风怒/剧毒/潜行）接入 `SkillHooks`
7. 装备"永久增加攻击力、血量"类特殊说明 + 装备提供派系
8. 编译 deprecation 警告清理（`@EventBusSubscriber.bus()` 等 5 条，沿用项目既有风格）
9. `/duel fillbag` 调试命令（一键填满卡包，测试辅助）
10. **卡背系统（用户定稿方向，见 `CARD_DESIGN.md` §10.3）**：多款卡背 + 玩家卡包袋选择（偏好存储 + UI 页签 + 牌库/对手手牌背/秘密区渲染接入）；陷阱卡背渲染与秘密触发扩展并入此项
11. **同步包瘦身（设计已定稿，见 `CARD_DESIGN.md` §9.5）**：阶段一必做——`savePublic` 公开同步视图 deck/discard 内容改为仅数量（存档 NBT 保留全量内容），单次同步 ~8KB→~1KB；阶段二可选——同步时机脏标记化/按 tick 节流
12. **膨胀硬上限（印卡前置条件，P2 必做）**：①**回合上限已定稿**——`DuelConfig.maxTurnLimit` 默认 50 可配置（SERVER，建议范围 10-200），配置注释警告+客户端「第 X/50 回合」倒计提醒，达上限按血量判胜/平局；②弃牌堆上限（如 60 张，超出销毁最旧）；设计见 `CARD_DESIGN.md` §9.5

---

## 六、计划路线

1. **近期（可选）**：制作 53 张卡包 JSON（`CARD_DESIGN.md` 卡表 → `cards/index`+`cards/data`+占位贴图）→ 用户双客户端实测 P1 全流程 → 按 B1-B20 修 bug
2. **P2 开局**：技能数据驱动系统（第 9 节三阶段）→ rarity/派系字段 → 卡包内容落地 → 拖拽交互
3. **推送**：用户已指示推送（回合上限+同步优化设计定稿后）；本提交一并推送，之后 origin 与本地同步

---

## 七、关键决策记录（用户确认，长期有效）

- **工作流**：「后续的所有改动在写完后先给我确认再进行更改」——逐文件清单 → 确认 → 实现
- **git 身份**：DeepSeek Agent / deepseek-agent@noreply.invalid；`3b2eabd` 保持 mcczai 身份
- **对战规则**：炉石式 cost；开局设法力封顶+生命上限；手牌 8/起手 4 换 2；7 战场槽；疲劳递增；随机先手+后手硬币；HUD 结束回合；固定座位+全俯视
- **四类结算**：秘密区上限默认 3 可配 1-8；装备每召唤物 1 件、替换旧装备重置进弃牌堆
- **装备**：HP 字段即耐久（不新增字段）；召唤物**受到任意伤害**耐久-1（guard 减到 0 不磨损）；损坏→装备重置+召唤物恢复原有属性；召唤物死亡→卡与装备均重置数值进弃牌堆
- **技能系统**：独立模块、任意类型卡牌可调用、玩家可加新机制——数据驱动方向，P2 实施（第 9 节）
- **回合上限**：默认 50、可配置（SERVER `DuelConfig.maxTurnLimit`，建议 10-200）；配置注释必须提醒玩家过高回合数会引发对局状态/存档膨胀风险；达上限按血量判胜、相等平局；HUD 显示「第 X/50 回合」+ 剩余 ≤5 回合警告色
- **双端同步**：deck/discard 内容移出公开同步（存档保留全量、同步仅数量）；手牌/秘密维持定向私发不变；同步时机脏标记化为可选优化
- **拖拽施法**：先记录，P2 实现
- **卡池**：53 张设计稿平衡版 v2（`CARD_DESIGN.md` §5+§5.5），标准对局 10 法力/30 血/50 回合，含快攻/陷阱/成长/控制四流派与解牌矩阵，待实测微调
- **卡面视觉（用户定稿，`CARD_DESIGN.md` §10）**：6 派系×4 稀有度框（派系材质：末地石/黑曜石/原木苔藓/深板岩/红石矿石/工作台熔炉）；**稀有度表现定稿：不加外圈，下 1/3 数值区矿物色包边+四角矿物宝石（铁/金/钻石/下界合金阶梯）**；卡面=上 2/3 立绘+下 1/3 数值、零文字；卡名/描述走 tooltip（仅 2D 渲染）；多卡背玩家自选；空槽不渲染贴图

---

## 八、文档索引

| 文档 | 位置 | 内容 |
|---|---|---|
| 项目状态总览（本文件） | `docs/PROJECT_STATUS.md` | 四类状态+框架+决策 |
| 卡牌设计 | `docs/CARD_DESIGN.md` | 53 张卡池（平衡版 v2）、四类结算已实现清单、技能系统草案（§9）、拖拽交互（§8） |
| 测试清单 | `docs/P1_TEST.md` | P1-1a~P1-5 用例、B1-B20 风险、回归顺序 |
| 问题清单（原始） | `issues.md`（根目录） | 项目历史问题记录 |
| 项目自述 | `README.md`（根目录） | mod 简介 |
