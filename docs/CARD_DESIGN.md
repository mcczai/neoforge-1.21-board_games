# Card Duel · 卡牌设计说明（平衡版 v3）

> 状态：**设计说明（平衡版 v3）**。四类结算区分 + P1 内建技能最小集已实现（2025，见第 7 节）；卡包 JSON/贴图仍待 P2。
> 平衡基准：**10 法力上限 / 30 生命 / 50 回合上限**（标准对局参数）。
> **卡牌列表已独立**：54 张卡表与平衡分析见同目录 `CARDS.md`；本文件只保留设计原则/数值规范/机制说明。
> 关联：`P1_TEST.md`（测试清单，含 P1-5 四类结算用例）。

---

## 0. 目的与硬约束

**目的**：为 P1 对战核心提供可测试的卡池。P1 阶段所有卡牌（含魔法/陷阱/装备）按召唤卡数值结算；P2 技能系统实现后按类型差异化。

**硬约束**（来自现有代码，设计已满足）：
1. 卡包不允许重复卡（`CardBagMenu.canInsertCard`）→ 本卡池 **54 张**（`CARDS.md`），可供双方各组一副 26 张左右的牌组（牌库上限 27）
2. 数据校验要求 `ATK ≥ 1、HP ≥ 1、MP ≥ 1` → 纯防御/纯法术卡也设有 ≥1 的攻击（"P1 临时数值"）
3. 卡牌 JSON 目前无"稀有度"字段 → P2 实现卡包时需在 `CardDataPOJO`/`CardIndexPOJO` 增加 `rarity` 字段（见第 6 节）

---

## 1. 设计原则

1. **MC 原作用映射**：每张卡的效果必须对应原版物品/生物/建筑的机制（表中有"MC 依据"列）
2. **四类卡牌**：
   - 召唤（summon）＝ 生物，靠攻/血站场战斗
   - 魔法（mana）＝ 药水/消耗品/法术，即时效果（直伤/回复/抽牌/临时法力）
   - 陷阱（trap）＝ 红石机关与环境危害，以"秘密"（对手回合触发）与反制为主
   - 装备（equip）＝ 剑/盾/弓/护甲/鞘翅，P2 附着到召唤物上增强
3. **派系**：自然、下界、末地、虚空、红石、史蒂夫（详见第 2 节）
4. **稀有度**：普通/稀有/罕见/传说。强度随稀有度上升，但保留战术弹性（稀有卡可凭特效胜过低稀有度白板高攻，反之高攻白板也能赢花哨罕见卡）
5. **技能**：关键字参考炉石等卡牌游戏（战吼/亡语/嘲讽/冲锋/风怒/秘密/剧毒/图腾），全部以"P2 技能 id"形式预留；`SkillHooks` 已就位
6. **平衡基准**：标准对局 10 法力上限 / 30 生命 / 50 回合上限。要求：①正常对局（含治疗与拖延）50 回合内必分胜负（疲劳兜底）；②每种主流构筑都有对应的克制解；③体现构筑联动（快攻、陷阱猜谜、成长组合、控制疲劳），见 §5.5

---

## 2. 派系定义

| 派系  | id       | 承载内容                | 典型卡                       |
|-----|----------|---------------------|---------------------------|
| 自然  | nature   | 主世界生态（含海洋、敌对刷怪）与作物  | 羊、铁傀儡、苦力怕、仙人掌、三叉戟         |
| 下界  | nether   | 下界生物、下界材料与灼烧主题      | 烈焰人、熔岩桶、岩浆块、凋零玫瑰、下界合金剑    |
| 末地  | end      | 末地生物与瞬移/悬浮主题        | 末影人、潜影贝、末影珍珠、鞘翅           |
| 虚空  | void     | 深暗/夜空/深渊（监守者、幻翼、幽匿） | 幻翼、监守者、幽匿尖啸体              |
| 红石  | redstone | 红石机关与生电陷阱           | 压力板地雷、发射器陷阱、铁砧坠落、TNT、红石脉冲 |
| 史蒂夫 | steve    | 玩家道具：药水、剑盾、图腾       | 治疗药水、木剑、盾牌、不死图腾           |

> 原 `CardTribe` 枚举（ender/nature/monster/ocean）将被本表 6 派系取代（P2 更新枚举与语言文件）。

---

## 3. 稀有度定义

| 稀有度 | id        | 数量（本卡池） | 定位                       |
|-----|-----------|---------|--------------------------|
| 普通  | common    | 19      | 基础数值卡，构筑骨干               |
| 稀有  | rare      | 18      | 一个明确的特效                  |
| 罕见  | epic      | 13      | 强力特效或组合核心                |
| 传说  | legendary | 4       | 高费终结者：监守者、末影龙、不死图腾、下界合金剑 |

---

## 4. 数值规范

- **HP** ≈ 生物血量（1 心 = 1 HP）；**ATK** ≈ 生物近战伤害（1 心 = 1 ATK）
- **费用模型（v2）**：白板 MP ≈ ceil((ATK + HP - 1) / 2)——1 费 3 点、2 费 5 点、3 费 7 点、4 费 9 点、5 费 11 点、6 费 13 点、8 费 17 点、9 费 19 点；特效按强度 -1 点或 +1 费（嘲讽/冲锋/风怒 +1 费档；亡语/战吼按效果 1-2 费价值）
- **直伤/治疗定价**（30 血基准）：2 费 = 3 伤或 3 治疗（指定目标）；随机目标与限制条件打 8 折（2 费 3 伤随机）；AOE 按命中数量折扣（4 费敌方全场 2 伤）
- **药水类**：回复/伤害量直接折算数值；抽牌 2 张 ≈ 3 费；临时法力 ≈ 0 费（附带抽 1 则 1 费）
- **装备卡**：atk = 攻击加成；**hp = 耐久值**（每次受攻击 -1，归零损坏，同时是该装备提供的额外生命层）；耐久 2 = 标准，减伤/嘲讽类装备耐久 3-6
- **魔法/陷阱卡**：atk/hp 为 P1 占位数值（陷阱 thorns 类 hp 会参与站场战斗）

---

## 5. 卡牌列表（已独立）

> **54 张完整卡表（平衡版 v3：名字活力化 + 平衡复查）已独立至 [`CARDS.md`](./CARDS.md)**：
> 召唤 21 / 魔法 13 / 陷阱 10 / 装备 10；含平衡验证（三重保险、解牌矩阵、四流派）、P1 兼容性说明与修订记录。
> 本节只保留概览，卡表细节一律以 CARDS.md 为准。

## 6. P2 实现卡包时需要的数据结构改动

1. `CardDataPOJO` / `CardIndexPOJO` 增加 `rarity` 字段（common/rare/epic/legendary），工具提示显示稀有度与彩色边框
2. `CardTribe` 枚举更新为 6 派系（nature/nether/end/void/redstone/steve）+ 语言文件派系名
3. 卡面贴图 54 张：美术未定前可全部复用 `sheep.png` 或按派系使用占位纯色（P2 再补）
4. 技能 id 注册：`SkillRegistry`（P2）按 `CARDS.md` 卡表"技能 id"列实现完整技能；P1 内建子集（第 7 节）将被其取代
5. 陷阱"秘密"机制已实现（P1：秘密区 + 5 种内建触发）；P2 扩展触发条件与卡背渲染

## 7. 已实现（P1 四类结算区分 + 内建技能最小集）

**结算路径**（`DuelEngine.playCard` 按 type+skill 分流，未知技能一律按召唤卡站场兜底）：
- **召唤**：占己方战场槽（召唤失调/每回合一次攻击）
- **魔法**：打出立即结算内建效果 → 进弃牌堆（不占槽）
- **陷阱**：`secret_*` → 秘密区（上限 `DuelConfig.trapZoneLimit`，默认 3，可配 1-8，SERVER 配置）；`anvil_N` → 立即对点选敌方召唤物 N 伤；`*_thorns` → 占槽站场但不可主动攻击，受击反伤 1
- **装备**（用户 2025 规则修订）：附着己方召唤物，**每召唤物 1 件，新装备替换旧装备**。装备提供**属性**（atk=攻击加成）、**机制/技能**（内建子集见下）与**耐久**：
  - 耐久 = 装备卡 hp 字段，**每次"受到攻击"（主攻击/反击/反伤）-1**；魔法/陷阱伤害不磨损耐久
  - 耐久同时是额外生命层（召唤物存活判定 = 本体 HP + 装备耐久，随磨损递减）
  - 耐久归零 → 装备**重置后**进弃牌堆，召唤物恢复原有属性（本体 HP 已 ≤0 则立即死亡）
  - 召唤物死亡 → 装备与召唤物均**重置数值**后进弃牌堆（`DuelEngine.freshCard` 从注册表重建全新卡）
  - 装备特殊机制（内建）：`equip_guard` 受击伤害 -1 / `equip_ranged` 不受反击 / `equip_elytra` 冲锋+免疫陷阱伤害 / `equip_trident` 风怒（每回合两次攻击）/ `equip_taunt` 嘲讽（对方必须先攻击嘲讽单位）
  - 装备提供**派系**与"永久增加攻击力、血量"类特殊说明留 P2

**P1 内建技能子集**（skill id → 效果）：
- mana 类：`heal_N` 回 N 血（封顶 hpCap）/ `harm_N` 对方玩家 N 伤 / `fire_N` 随机敌方召唤物 N 伤（无召唤物兜底打玩家）/ `lava_N` 随机敌方单位 N 伤 / `draw_N` 抽 N 张 / `mana_N` 本回合 +N 法力 / `golden_heal` 回 5+抽 1 / `pearl_strike` 随机敌方召唤物受本卡 ATK 伤 / `totem_protect` 免死一次（抵挡致命伤害，生命置 5）
- trap 类：`secret_mine` 对方出任意牌→其玩家 2 伤 / `secret_arrow` 对方召唤物上场→该召唤物 3 伤 / `secret_wither` 对方攻击→攻击方 2 伤 / `secret_sculk` 对方回合开始→跳过抽牌 / `secret_tnt` 对方场上≥3 召唤物→双方全场 3 伤 / `anvil_N` 点选敌方召唤物 N 伤（代码保留，卡池已改用 secret_arrow）/ `cactus_thorns` 站场反伤 1
- equip 类：atk=攻击加成、hp=耐久（每受击-1）；特殊机制内建子集 `equip_guard`/`equip_ranged`/`equip_elytra`/`equip_trident`/`equip_taunt`；`equip_atk_1/2/3`、`equip_netherite` 为纯数值（netherite 的"永久"语义留 P2）
- summon 类：技能全部留 P2（白板站场）

**尚未实现（P2）**：战吼/亡语/嘲讽/冲锋/风怒/剧毒/图腾等召唤卡技能；装备独立槽位与特殊装备效果；秘密触发扩展；卡包 JSON 与贴图；拖拽施法交互（第 8 节）。

## 8. P2 交互规划（用户已确认，先记录后实现）

**炉石式拖拽施法目标选择**（用户 2025 提出）：
- 长按拖动手牌 / 玩家头像 / 召唤物，移动到想要施法的对象上释放
- 被拖动的来源与目标之间绘制一条**指示箭头**（贯穿整个拖动过程）
- 用于：魔法牌选目标（替代当前"点己方半场随机/点敌方槽"）、装备牌选宿主、攻击选目标、打脸拖到对方头像
- 实现要点（P2）：鼠标按下/拖动/释放三段事件 → 拖拽指示层渲染（`RenderGuiEvent`）→ 释放时命中检测（复用 `HudClickManager.hitTable` 的屏幕→桌面映射）→ 服务端沿用现有 payload（`ServerboundPlayCardPayload`/`ServerboundAttackPayload`）
- 当前 P1 的点击两段式交互（选中→点目标）保持不变，拖拽为增强交互

---

## 9. 技能系统（数据驱动）方向与难度预估

> 用户 2025 需求：技能做成独立模块，可被**任意类型卡牌**调用；玩家能像添加卡牌一样**添加新机制**。
> 结论：**框架能力足够（项目已有完整的卡包数据驱动基础设施可复用），但属于 P2 级重构，现不实施**。理由：P1 内建技能已闭环且尚未实测，立即重构会叠加测试盲区；本草案作为 P2 开局第一项。
> 用户 2025 补充设计（本节 9.1 已按此重写）：技能 = `trigger` + **有序 effects 列表**；效果由**基础效果接口**（effect id + 参数）组成，一个技能可按顺序串联多个效果。

**难度预估**：中-高。10-15 个文件，`DuelEngine` 大半重构，建议 3 个阶段交付（技能定义→执行器→迁移）。

### 9.1 架构草案（含用户确认的技能格式）

1. **技能资源格式**（与卡包同模式，放资源包 `cards/skills/*.json`；用户示例规范化后如下）：
   ```json
   {
     "id": "egskill_1",
     "trigger": "on_play_mana",
     "effects": [
       { "effect": "add_atk",  "target": "self", "amount": 1 },
       { "effect": "taunt",   "target": "self" }
     ]
   }
   ```
   即：该技能触发时**先**"增加 1 点攻击力"、**再**获得"嘲讽"。规范化说明（与用户原始示例的差异，已按此定稿）：
   - `effects` 用 **数组** 而非对象——JSON 对象不保证顺序，而技能要求"按序依次触发"，数组是唯一保序写法
   - 效果条目统一为对象：`{ "effect": "<效果类型id>", "<参数名>": <参数值> }`
   - 默认 `target` 为 `"self"`（技能承载者自身：召唤物/装备宿主/玩家），可省略；`deal_damage` 等必须显式指定目标
   - `trigger` 枚举：on_summon / on_death / on_attack / on_attacked / on_damaged / on_turn_start / on_turn_end / on_draw / on_play_mana / on_play_card / on_opponent_play_card / on_opponent_summon / on_opponent_attack / on_equip / on_unequip …

2. **基础效果接口清单 v1**（P2 实施时的内建 effect 集合；玩家组合它们即可造新技能）：

   | effect id | 类别 | 参数 | 语义 |
   |---|---|---|---|
   | `add_atk` | 数值 | `amount`, `target` | +攻击力 |
   | `add_hp` | 数值 | `amount`, `target` | +生命（治疗，封顶） |
   | `deal_damage` | 数值 | `amount`, `target` | 造成伤害（target：`enemy_player`/`random_enemy_summon`/`random_enemy_unit`/`selected_enemy_summon`/`attacker`/`self`） |
   | `draw` | 数值 | `amount` | 抽牌（触发者玩家） |
   | `add_mana` | 数值 | `amount` | 本回合 +法力 |
   | `block_damage` | 状态 | `once` | 抵挡下一次伤害（通用化的"不死图腾"） |
   | `add_durability` | 数值 | `amount`, `target` | 调整装备耐久（可选） |
   | `taunt` | 规则 | — | 嘲讽：对方必须先攻击此单位 |
   | `charge` | 规则 | — | 冲锋：上场即可攻击 |
   | `windfury` | 规则 | — | 风怒：每回合攻击两次 |
   | `guard` | 规则 | — | 受击伤害 -1 |
   | `ranged` | 规则 | — | 攻击不受反击 |
   | `trap_immune` | 规则 | — | 免疫陷阱伤害 |
   | `thorns` | 规则 | `amount` | 受击反伤 N（含"无法主动攻击"副作用，P2 拆分为独立规则项再议） |

   - **数值型**：纯数据驱动，玩家加 JSON 即可组合（对应 `SkillHooks` 的一次性效果）
   - **规则型**：引擎提供规则钩子接口（`canAttack/canTarget/damageModify/isImmune`），钩子即上表内建 effect；玩家可组合已有规则，**新规则需 Java 扩展**
   - **统一模型红利**：秘密陷阱也归一到此模型——`secret_mine` = `trigger: on_opponent_play_card` + `effects: [deal_damage enemy_player 2]`，与魔法/装备/召唤技能共用一套机制

3. **SkillRegistry**：加载 + 校验 + 按 trigger 索引（复用 `CommonCardPackLoader`/`CardAssetManager`/POJO 模式）

4. **SkillDispatcher**：引擎各触发点调用 `dispatcher.dispatch(trigger, context)`，遍历相关卡牌（场上/手牌/装备/秘密区）的 skill id → 按 `effects` 顺序执行

5. **effect 扩展点**：`DeferredRegister<EffectType>`（NeoForge 注册），其他 mod/开发者可注册全新 effect 类型（供高级自定义机制）

### 9.2 迁移清单（P2 实施时）

- 现有硬编码内建技能全部转为技能 JSON，映射示例：
  - `heal_3` = on_play_mana → `[add_hp self 3]`；`harm_3` = on_play_mana → `[deal_damage enemy_player 3]`
  - `mana_1` = on_play_mana → `[add_mana 1, draw 1]`（P1 硬编码只有 +1 法力，P2 定义补抽牌）
  - `golden_heal` = on_play_mana → `[add_hp self 5, draw 1]`（多效果串联示例）
  - `totem_protect` = on_play_mana → `[block_damage once]`
  - `secret_mine/arrow/wither/sculk/tnt` = on_opponent_* → 对应效果；新秘密 `secret_freeze` = on_opponent_attack → `[freeze attacker]`（P2 新规则 effect）、`secret_draw` = on_opponent_play_mana → `[draw 1]`
  - `cactus_thorns` = on_attacked → `[deal_damage attacker 1]`；新秘密 `secret_magma` = on_opponent_turn_start（对方场上有召唤物）→ `[deal_damage all_enemy_summons 2]`、`secret_dispenser` = on_opponent_summon → `[deal_damage all_enemy_summons 1]`
  - 平衡版新增技能：`buff_all_atk_1` = on_play_mana → `[add_atk all_allies 1]`、`buff_atk_3` = `[add_atk selected_ally 3]`、`heal_all_2` = `[add_hp all_allies 2]`、`aoe_enemy_2` = `[deal_damage all_enemy_summons 2]`、`silverfish_growth` = on_turn_start → `[add_atk self 1, add_hp self 1]`、`pillager_shot` = on_summon → `[deal_damage enemy_player 1]`、`dragon_breath` = on_summon → `[deal_damage all_enemy_summons 2]`
  - 装备机制 5 个 = 规则型 effect（guard/ranged/charge+免疫/windfury/taunt）
- `DuelEngine` 触发点接 dispatcher：playCard / attack / applyCardDamage / startTurn / endTurn / drawCard / death / summon（约 10 处）
- 卡数据 `skill` 字段语义不变（存 skill id），技能定义独立于卡牌 JSON
- 校验：引用不存在的 skill id / effect id / 参数越界 → 加载时警告 + 对局中兜底白板

### 9.3 装备规则修订记录（用户 2025 确认）

- 装备 HP 字段**即耐久**，不新增字段
- 耐久磨损条件：召唤物**受到任意伤害**（攻击/反击/反伤/魔法/陷阱）→ 耐久 -1；guard 减伤减到 0 视为未受伤不磨损
- 耐久归零 → 装备重置进弃牌堆，召唤物恢复原有属性；召唤物死亡 → 卡与装备均重置数值进弃牌堆（`freshCard` 注册表重建）

### 9.4 存储与性能评估（技能数据化对牌桌 NBT 的影响）

**结论：零增长。** 技能定义不进牌桌 NBT——技能 JSON 属卡包静态资源，加载进内存注册表（同卡包索引）；卡上 `skill` 字段仅存 id 字符串，数据化只是换字符串。

- 单卡 NBT（`ItemStack.saveOptional`：id/count + card_id/card_data/in_duel 组件）≈ 150-200B
- 一局规则固定的卡总数 ≤56 张（deck 27×2 + 手牌 8×2 + 场 7×2 + 装备 7×2 + 秘密 ≤3×2，弃牌堆为循环兜底）
- 牌桌落盘 NBT 最坏 ≈ **12KB**（正常 5-8KB）；对照 chunk 同步包 2MB / `NbtAccounter` 2MB 上限占 0.6%，安全
- 技能注册表内存 <200KB（按 200 个技能计）；dispatcher 每次触发微秒级，无性能压力

**附带发现（P2 优化项）**：现有 `DuelPlayerData.savePublic` 将 deck（54 张）与 discard **内容**全量放进公开同步包，每次 `sync()` 重发（~8KB/次），而客户端只需数量——P2 应改为 deck/discard 仅同步数量，可显著减小高频同步包。

**印卡场景扩展评估（发现/三选一等生成新卡的技能）**：
- 印出的卡与普通卡同构（~200B/张，技能数据化零影响），但**弃牌堆无上限**使其成为唯一膨胀点
- 场景 A（正常对局）：疲劳伤害硬限对局 ≈44-60 回合（hpCap≤999），最坏 ~3000 张卡 ≈ 400-600KB 落盘 NBT——不超 2MB，但高频全量同步会卡顿
- 场景 B（**真·无限**）：印卡治疗闭环（每回合印治疗 > 疲劳伤害）→ 对局永不结束 → 弃牌堆无限增长 → 挂机 10000 回合 ≈60MB → 超 chunk 2MB 上限 = 坏档/崩服
- **结论：印卡上线前必须加硬上限（P2 必做）**：①弃牌堆上限（如 60 张，超出销毁最旧，NBT 上界锁死 ~23KB）；②对局回合数上限（**默认 50 可配置**，设计见 9.5 定稿）；③deck/discard 仅同步数量（设计见 9.5 定稿）

### 9.5 回合上限与双端同步优化（用户定稿，P2 实施）

**一、回合上限（对局时长保险丝，根治无限对局）**

- 新 SERVER 配置 `DuelConfig.maxTurnLimit`：**默认 50**，建议范围 10-200；注册方式同 `trapZoneLimit`（`CardduelMod` 构造器 `registerConfig(ModConfig.Type.SERVER, ...)`）
- **配置注释必须写明警告**（玩家在配置界面可见）：过高的回合数上限会显著增大对局状态体积与存档体积（弃牌堆持续增长；配合印卡/治疗闭环可致无界膨胀），可能造成卡顿或坏档——除非确有需要，请勿设置过高（>100 不推荐）
- **客户端提醒**：`ClientboundDuelSyncPayload` 顶层增 `turnLimit` 字段（服务端读 `DuelConfig.MAX_TURN_LIMIT` 下发）；`BattleBoardHud` 回合指示改为「第 X / 50 回合」，剩余 ≤5 回合时以警告色显示
- **结算规则**：`turnNumber` 达到上限 → 对局立即 FINISHED：剩余血量多者胜，相等判平局（判胜时机在回合结束检查，`DuelEngine` 回合循环内）
- 与疲劳的关系：疲劳是常规终结手段，回合上限只是最终保险丝；正常对局（hpCap≤999）约 10-60 回合内结束，50 回合默认值不影响常规对局体验

**二、双端同步优化（两阶段，阶段一必做）**

- **阶段一（必做）——deck/discard 内容移出公开同步**：现有 `DuelPlayerData.savePublic` 将 deck（54 张）与 discard **内容**全量塞进公开同步通道（`DuelTableBlockEntity.getUpdateTag`），每次同步 ~8KB 且客户端只读数量。改为：**存档 NBT 保留全量内容**（deck/discard 内容继续落盘，重进不丢牌）；**对局中公开同步仅携带数量**（`DeckCount`/`DiscardCount`）。**例外**：SETUP/WAITING 阶段（对局前）仍随包发送 deck 内容——`DuelTableBlockEntityRenderer.renderDecks` 需要渲染双方牌组预览；对局中该渲染分支不执行，故只同步数量。印卡后同理——弃牌堆内容永不进对局中公开同步。
- **阶段二（可选）——同步时机脏标记化**：目前每次状态变化都全量重发公开视图，可改为事件驱动（仅变化字段）或按 tick 节流合并，进一步压缩高频操作（攻击/出牌连点）的包量。
- **不变项**：手牌/秘密内容维持现有定向私发（`ClientboundDuelHandPayload` / `ClientboundDuelTrapPayload` 只发本人），不因瘦身而改变；服务器存档（落盘 NBT）不受影响。
- **预期收益**：对局中单次公开同步 ~8KB → ~1KB；印卡场景下收益同比例放大（弃牌堆内容永不参与对局中同步后，同步成本与弃牌堆规模完全解耦）。

**三、实现记录（代码已实现，未提交）**

- `config/DuelConfig.java`：`MAX_TURN_LIMIT`（`defineInRange("maxTurnLimit", 50, 10, 200)`）+ 配置注释警告（膨胀/坏档风险，>100 不推荐）
- `duel/DuelEngine.java`：`endTurn` 在切换行动方前检查 `turnNumber >= MAX_TURN_LIMIT` → 广播 `cardduel.duel.turn_limit` → `finishDuel(table, decideByHp(table))`；新增 `decideByHp`（血多者胜，相等 null）；`finishDuel` 平局分支广播 `cardduel.duel.draw_game`
- `duel/DuelPlayerData.java`：`savePublic(tag, provider, includeDeckContents)`——true 写 Deck/Discard 全量（存档/对局前预览），false 写 `DeckCount`/`DiscardCount`；`save()` 传 true 保持存档全量
- `block/entity/DuelTableBlockEntity.java`：`savePublic` 增参透传；`saveAdditional` 传 true；`getUpdateTag` 按 `phase == SETUP || WAITING` 决定是否携带 deck 内容
- `network/payload/ClientboundDuelSyncPayload.java`：record 增 `int turnLimit`（encode/decode varInt、`of()` 读 `DuelConfig.MAX_TURN_LIMIT.get()`）
- `client/hud/BattleBoardHud.java`：回合指示改 `cardduel.hud.turn_total`（第 %s/%s 回合），剩余 ≤5 回合用警告色 0xFFFF7043
- lang zh/en 新增：`cardduel.duel.turn_limit`、`cardduel.duel.draw_game`（注意 `cardduel.duel.draw` 已占用为抽牌消息）、`cardduel.hud.turn_total`
- 编译通过（4 条既有 deprecation 警告不变）

---

## 10. 卡面视觉与 UI 规范（用户定稿）

> 用户提供末地/下界/自然三派系卡框样图（每派系一行 × 4 稀有度），本轮定稿卡面布局、tooltip 与卡背方向。

### 10.1 卡框规格

- **结构**：6 派系 × 4 稀有度 = 24 个框；命名 `frame_{tribe}_{rarity}.png`；分辨率 128×128（预留 HUD 缩放余量，主要线条 ≥4px）
- **派系材质定稿**：末地=末地石（淡黄）、下界=**黑曜石（地狱门意象）**、自然=原木/苔藓（棕绿）、虚空=**深板岩砖+幽匿微光**（蓝灰）、红石=**红石矿石**（深灰+红色发光颗粒）、史蒂夫=**工作台/熔炉等工作方块**（玩家可互动人造物）
- **稀有度表现（用户定稿：包边+四角宝石）**：**不加外圈**（避免抢派系框主角、避免与黑曜石双层边缘糊成一片），改用两处矿物信号：
  - **下 1/3 数值区包边**：数值区边缘用对应矿物色包边（3-5px，用**矿物纹理**而非纯色）——玩家看费用/攻/血时顺手识别稀有度
  - **四角宝石**：四角各嵌一颗对应矿物宝石图标（矿石质感，简化图标保证小尺寸可辨）
  - 矿物阶梯：普通=**铁**（灰）、稀有=**金**（黄）、罕见=**钻石**（青）、传说=**下界合金**（紫黑）——MC 原版装备阶梯语义，零学习成本
  - 原底部中央小宝石保留作辅助，或并入四角方案由美术取舍
- 角饰建议六派系统一结构（末地尖塔/下界火焰/自然枝叶/虚空幽匿/红石矿石/史蒂夫铁砧）——留待框 v2 迭代

### 10.2 卡面布局

- **上 2/3 = 立绘**（MC 方形贴图放入零裁切零留白）；**下 1/3 = 数值区**（费用左上 / 攻左下 / 血右下；数值区边缘按稀有度矿物色包边 + 四角矿物宝石，见 §10.1）
- **卡面不渲染任何文字（含卡名）**——小尺寸不可读，全部信息走 tooltip
- **tooltip（鼠标悬浮）**：仅 2D 贴图渲染（复用 `cardTexture` 机制，不做 3D ItemStack 渲染省开销）；内容=卡名（首行）+ 稀有度/类型 + 费用/攻/血 + 技能描述；数据源已有 `CardIndexPOJO.tooltip`，P2 定制样式（稀有度着色、行宽、中英适配）

### 10.3 卡背与空槽

- **卡背**：用户将设计多款，**玩家在卡包袋中选择**。P2 实现要点：玩家级偏好存储（本地配置或玩家数据）→ 卡包袋"卡背"页签 → 牌库堆、对手手牌背、秘密区渲染接入所选卡背
- **空槽**：不渲染任何贴图（牌桌为测试模型、后续迭代）；现有 `DuelTableBlockEntityRenderer.renderBoardHalf` 已跳过空位渲染，无需改动

---

## 11. 待用户敲定的事项

1. **平衡版 v3 数值**（54 张，标准对局 10 法力/30 血/50 回合，卡表见 `CARDS.md`）：整体数值与流派设计待双客户端实测验证后微调
2. 个别卡的主题与数值是否要调整（如监守者 8 费、末影龙 9 费、不死图腾 6 费是否合理）
3. 秘密触发的优先级（多秘密同时满足时按放置顺序依次触发，当前实现）——如要改动 P2 再议
