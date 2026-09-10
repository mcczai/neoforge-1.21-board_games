# 54 张卡面生成：上 2/3 立绘 + 下 1/3 数值区（矿物色包边 + 三槽预留：攻=铁剑 / 血=红心 / 费=绿宝石）
param([string]$Root = 'D:\cyd\board_games')

Add-Type -AssemblyName System.Drawing
$packDir = Join-Path $Root 'src\main\resources\assets\cardduel\custom\default_card_pack\default_pack'
$frameDir = Join-Path $Root 'art\frames'
$texDir = Join-Path $packDir 'textures'
if (-not (Test-Path $texDir)) { New-Item -ItemType Directory -Force -Path $texDir | Out-Null }

$cards = ([System.IO.File]::ReadAllText((Join-Path $Root 'art\tools\cards.json'), [System.Text.Encoding]::UTF8) | ConvertFrom-Json)
$rarityMeta = $cards.rarityMeta

function Hex-Color([string]$hex, [int]$a = 255) {
    $h = $hex.TrimStart('#')
    [System.Drawing.Color]::FromArgb($a, [Convert]::ToInt32($h.Substring(0,2),16), [Convert]::ToInt32($h.Substring(2,2),16), [Convert]::ToInt32($h.Substring(4,2),16))
}
function Shift-Color([System.Drawing.Color]$c, [double]$f) {
    [System.Drawing.Color]::FromArgb($c.A, [Math]::Min(255,[Math]::Max(0,[int]($c.R + 255*$f))), [Math]::Min(255,[Math]::Max(0,[int]($c.G + 255*$f))), [Math]::Min(255,[Math]::Max(0,[int]($c.B + 255*$f))))
}
$script:BrushCache = @{}
function B([System.Drawing.Color]$c) {
    $k = "$($c.A)_$($c.R)_$($c.G)_$($c.B)"
    if (-not $script:BrushCache.ContainsKey($k)) { $script:BrushCache[$k] = New-Object System.Drawing.SolidBrush $c }
    return $script:BrushCache[$k]
}
function FR($gr, [System.Drawing.Color]$c, [int]$x, [int]$y, [int]$w, [int]$h) { $gr.FillRectangle((B $c), (New-Object System.Drawing.Rectangle $x, $y, $w, $h)) }
function Draw-Sprite($gr, $sprite, [int]$x0, [int]$y0, $pal) {
    for ($r = 0; $r -lt $sprite.Count; $r++) {
        $row = $sprite[$r]
        for ($c = 0; $c -lt $row.Length; $c++) {
            $ch = "$($row[$c])"
            if ($ch -eq '.') { continue }
            if (-not $pal.ContainsKey($ch)) { continue }
            $gr.FillRectangle((B $pal[$ch]), (New-Object System.Drawing.Rectangle ($x0 + $c), ($y0 + $r), 1, 1))
        }
    }
}

# ============ 立绘精灵表 ============
$SP = @{}
$SP['quadruped'] = @(
 '.....2222.....', '....222222....', '..111111112...', '.111111111122.',
 '.111111111124.', '.111111111444.', '.1111111111.5.', '.1111111111...',
 '.3311111133...', '..3..3..3..3..', '..5..5..5..5..')
$SP['humanoid'] = @(
 '....2222....', '...222222...', '...255552...', '...222222...', '....3333....',
 '..11111111..', '.1111111111.', '.1111111111.', '.1111111111.', '..11111111..',
 '..11111111..', '..111..111..', '..333..333..', '..333..333..')
$SP['flyer'] = @(
 '..1........1..', '.111......111.', '11111....11111', '.1111.22.1111.',
 '..111.22.111..', '....112211....', '....122221....', '....122221....',
 '.....1221.....', '......55......')
$SP['bug'] = @(
 '....222222....', '..2211111122..', '.111111111111.', '11151111111151',
 '.111111111111.', '..3311111133..', '...3.3..3.3...', '...5.5..5.5...')
$SP['boss'] = @(
 '.....222222.....', '...2222222222...', '..221111111122..', '.22111111111122.',
 '.11111111111111.', '.11551111115511.', '.11111111111111.', '.11111111111111.',
 '.33111111111133.', '.33311111111333.', '..333333333333..', '..33..3333..33..',
 '..55..5555..55..', '......5..5......')
$SP['potion'] = @(
 '.....44.....', '.....44.....', '....3333....', '....3223....', '...311113...',
 '..31111113..', '.1111111111.', '.1111111111.', '.1211111121.', '.1221111221.',
 '.1221111221.', '.3333333333.', '.3333333333.', '..33333333..')
$SP['orb'] = @(
 '....2222....', '..22111122..', '.2211111122.', '.2111111112.', '211111111112',
 '211111111112', '211111111112', '.2111111112.', '.2211111122.', '..22111122..',
 '....2222....')
$SP['skull'] = @(
 '...222222...', '..22222222..', '.2222222222.', '.2552222552.', '.2552222552.',
 '.2222222222.', '..22222522..', '..22222222..', '...2.22.2...', '...2.22.2...',
 '...2.22.2...')
$SP['bucket'] = @(
 '..33333333..', '..32222223..', '..31111113..', '.3111111113.', '.3111111113.',
 '.3111111113.', '.3111111113.', '.3311111133.', '..33111133..', '...333333...')
$SP['apple'] = @(
 '.....4......', '....44......', '..2222......', '.22111222...', '2111111112..',
 '2111111112..', '2111111112..', '2111111112..', '.211111112..', '..22111122..',
 '....2222....')
$SP['totem'] = @(
 '....4444....', '...411114...', '...411114...', '...444444...', '....1111....',
 '...111111...', '...155551...', '...111111...', '...111111...', '....1111....',
 '...444444...', '...411114...', '...444444...')
$SP['plate'] = @(
 '..3333333333..', '.321111111123.', '.311111111113.', '.311115511113.',
 '.311115511113.', '.311111111113.', '.321111111123.', '..3333333333..')
$SP['anvil'] = @(
 '..1111111111..', '.111111111111.', '.111111111111.', '..1111111111..',
 '...11111111...', '....111111....', '....111111....', '...11111111...',
 '..1111111111..', '..3333333333..')
$SP['plant'] = @(
 '.....11.....', '..1..11..1..', '..11.11.11..', '...1111111..', '....1111....',
 '....1111....', '....1111....', '....1111....', '....1111....', '...333333...',
 '...311113...', '...333333...')
$SP['tnt'] = @(
 '.......4....', '......44....', '..55555555..', '..11111111..', '.1111111111.',
 '.4444444444.', '.1111111111.', '.1111111111.', '.3333333333.', '..33333333..')
$SP['sculk'] = @(
 '.....4444.....', '...44111144...', '..4111111114..', '.111111111111.',
 '.111144111111.', '.111444411111.', '.111144111111.', '.111111111111.',
 '.331111111133.', '..3333333333..', '...3.3333.3...')
$SP['sword'] = @(
 '..............1.', '.............121', '............1211', '...........1211.',
 '..........1211..', '.........1211...', '........1211....', '.......1211.....',
 '......1211......', '.....1211.......', '..4.1211........', '.4441211........',
 '.444411.........', '..5511..........', '..551...........', '..55............')
$SP['shield'] = @(
 '.333333333333.', '.311111111113.', '.311111111113.', '.312211112213.',
 '.312211112213.', '.311111111113.', '.311122111113.', '.311122111113.',
 '..3111111113..', '..3111111113..', '...31111113...', '....311113....',
 '.....3113.....', '......33......')
$SP['bow'] = @(
 '....44........', '...4114.......', '..4111........', '..411.........',
 '..41..........', '..41..........', '..41..........', '..41..........',
 '..411.........', '..4111........', '...4114.......', '....44........')
$SP['armor'] = @(
 '..22......22..', '.2112....2112.', '.211111111112.', '.211111111112.',
 '.211111111112.', '.211111111112.', '..1111111111..', '..1111111111..',
 '..1111111111..', '..1111111111..', '..1111111111..', '..3311111133..',
 '..33......33..')
$SP['elytra'] = @(
 '...11......11...', '..1111....1111..', '.111111..111111.', '1111111..1111111',
 '111111....111111', '.11111....11111.', '..111......111..', '...11..44..11...',
 '.......44.......')
$SP['trident'] = @(
 '..1...1...1.....', '..1...1...1.....', '..1...1...1.....', '..11..1..11.....',
 '...1111111......', '.....414........', '......4.........', '......4.........',
 '......4.........', '......4.........', '......4.........', '.....444........',
 '......4.........')
$SP['dust'] = @(
 '...1..11....', '..111.111...', '.1111111111.', '.1111111111.', '..11111111..',
 '...111111...', '....1111....', '.....11.....')

# 数值区三槽图标（固定配色，与卡牌无关）
$SP['icon_atk'] = @(
 '........1', '.......12', '......121', '.....121.', '....121..', '...121...',
 '..121....', '.4121....', '.4411....', '..331....', '..33.....')
$SP['icon_hp'] = @(
 '.33...33.', '3133.3313', '311111113', '311111113', '.3111113.', '..31113..',
 '...313...', '....3....')
$SP['icon_mp'] = @(
 '..33333..', '.3211123.', '321111123', '321111123', '.3211123.', '.3211123.',
 '..32123..', '...323...')
$iconAtkPal = @{ '1' = (Hex-Color '#D8D8DC'); '2' = (Hex-Color '#F4F4F8'); '3' = (Hex-Color '#6B4A2A'); '4' = (Hex-Color '#A8A8B0') }
$iconHpPal  = @{ '1' = (Hex-Color '#E13B3B'); '2' = (Hex-Color '#FF8A8A'); '3' = (Hex-Color '#5E0E0E') }
$iconMpPal  = @{ '1' = (Hex-Color '#17DD62'); '2' = (Hex-Color '#8CF5B4'); '3' = (Hex-Color '#0B6B31') }

# ============ 立绘背景（按类型/派系） ============
$skyGround = @{
    'nature'   = @('#7FB6E8', '#6AAA50'); 'nether' = @('#5E2620', '#8A4422'); 'end' = @('#2E2740', '#6A5A80')
    'void'     = @('#1C2A30', '#2A4A48'); 'redstone' = @('#3A2A2A', '#5A3A38'); 'steve' = @('#5A7AA8', '#7A6A4A')
}

$FW = 48; $FH = 64
$HX0 = 6; $HY0 = 5; $HX1 = 43; $HY1 = 55
$VAL_Y0 = 39; $VAL_Y1 = 55
$ART_Y0 = $HY0; $ART_Y1 = $VAL_Y0 - 1
$ART_H = $ART_Y1 - $ART_Y0 + 1
$HOLE_W = $HX1 - $HX0 + 1

$slots = @{}
$made = 0
$missing = @()
foreach ($card in $cards.cards) {
    $framePath = Join-Path $frameDir "frame_$($card.tribe)_$($card.rarity).png"
    if (-not (Test-Path $framePath)) { $missing += $framePath; continue }
    $frame = New-Object System.Drawing.Bitmap $framePath
    $bmp = New-Object System.Drawing.Bitmap $FW, $FH
    $gr = [System.Drawing.Graphics]::FromImage($bmp)
    $gr.Clear([System.Drawing.Color]::Transparent)
    $gr.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor

    $accent = Hex-Color $card.accent
    # 保证精灵在深色背景上仍有对比：亮度不足的主色提亮到最低亮度
    $lum = (0.299 * $accent.R + 0.587 * $accent.G + 0.114 * $accent.B) / 255.0
    $body = $accent
    if ($lum -lt 0.45) { $body = Shift-Color $accent (0.45 - $lum) }
    $pal = @{
        '1' = $body
        '2' = (Shift-Color $body 0.32)
        '3' = (Shift-Color $body -0.30)
        '4' = (Hex-Color '#E8E8EC')
        '5' = (Hex-Color '#15151A')
    }

    # --- 立绘背景 ---
    $sg = $skyGround[$card.tribe]
    if ($card.type -eq 'summon') {
        $sky = Hex-Color $sg[0]; $gnd = Hex-Color $sg[1]
        $split = $ART_Y0 + [int]($ART_H * 0.58)
        FR $gr $sky $HX0 $ART_Y0 $HOLE_W ($split - $ART_Y0)
        FR $gr $gnd $HX0 $split $HOLE_W ($ART_Y1 - $split + 1)
        FR $gr (Shift-Color $gnd 0.06) $HX0 ($split + 1) $HOLE_W 1
        # 云
        $cl = Hex-Color '#F2F6FA'
        FR $gr $cl ($HX0 + 3) ($ART_Y0 + 3) 7 1
        FR $gr $cl ($HX0 + 4) ($ART_Y0 + 4) 9 1
        FR $gr $cl ($HX1 - 12) ($ART_Y0 + 7) 6 1
    } elseif ($card.type -eq 'mana') {
        $base = Hex-Color '#1E2028'
        FR $gr $base $HX0 $ART_Y0 $HOLE_W $ART_H
        # 中心柔光：低透明度 accent 叠加（保证精灵与底色对比）
        FR $gr (Hex-Color $card.accent 75) ($HX0 + 3) ($ART_Y0 + 2) ($HOLE_W - 6) ($ART_H - 4)
        FR $gr (Hex-Color $card.accent 55) ($HX0 + 7) ($ART_Y0 + 6) ($HOLE_W - 14) ($ART_H - 12)
    } elseif ($card.type -eq 'trap') {
        $base = Hex-Color '#32333B'; $line = Hex-Color '#3E3F49'
        FR $gr $base $HX0 $ART_Y0 $HOLE_W $ART_H
        for ($x = $HX0; $x -le $HX1; $x += 6) { FR $gr $line $x $ART_Y0 1 $ART_H }
        for ($y = $ART_Y0; $y -le $ART_Y1; $y += 6) { FR $gr $line $HX0 $y $HOLE_W 1 }
    } else {
        $base = Hex-Color '#4A3A2A'; $line = Hex-Color '#3A2C20'; $hi = Hex-Color '#57452F'
        FR $gr $base $HX0 $ART_Y0 $HOLE_W $ART_H
        for ($y = $ART_Y0; $y -le $ART_Y1; $y += 5) { FR $gr $line $HX0 $y $HOLE_W 1; FR $gr $hi $HX0 ($y + 1) $HOLE_W 1 }
    }

    # --- 立绘精灵 ---
    $spr = $SP[$card.art]
    if ($spr) {
        $sh = $spr.Count
        $sw = 0
        foreach ($r in $spr) { if ($r.Length -gt $sw) { $sw = $r.Length } }
        $sx = $HX0 + [int](($HOLE_W - $sw) / 2)
        $sy = $ART_Y0 + [int](($ART_H - $sh) / 2)
        Draw-Sprite $gr $spr $sx $sy $pal
    }

    # --- 数值区 ---
    $band = Hex-Color '#1B1D23'
    FR $gr $band $HX0 $VAL_Y0 $HOLE_W ($VAL_Y1 - $VAL_Y0 + 1)
    $gem = Hex-Color $rarityMeta.($card.rarity).gem
    # 矿物色包边（1px 外框 + 1px 内亮线）
    FR $gr $gem $HX0 $VAL_Y0 $HOLE_W 1
    FR $gr $gem $HX0 $VAL_Y1 $HOLE_W 1
    FR $gr $gem $HX0 $VAL_Y0 1 ($VAL_Y1 - $VAL_Y0 + 1)
    FR $gr $gem $HX1 $VAL_Y0 1 ($VAL_Y1 - $VAL_Y0 + 1)
    FR $gr (Shift-Color $gem 0.25) ($HX0 + 1) ($VAL_Y0 + 1) ($HOLE_W - 2) 1
    # 三槽：攻(铁剑) / 血(红心) / 费(绿宝石)
    $order = @(@('atk', 'icon_atk', $iconAtkPal), @('hp', 'icon_hp', $iconHpPal), @('mp', 'icon_mp', $iconMpPal))
    $iconW = 9; $gap = 3
    $totalW = $iconW * 3 + $gap * 2
    $startX = $HX0 + [int](($HOLE_W - $totalW) / 2)
    $slotY = $VAL_Y0 + [int](($VAL_Y1 - $VAL_Y0 + 1 - 11) / 2) + 1
    $slotInfo = @{}
    for ($i = 0; $i -lt 3; $i++) {
        $name = $order[$i][0]; $spriteName = $order[$i][1]; $p = $order[$i][2]
        $ix = $startX + $i * ($iconW + $gap)
        Draw-Sprite $gr $SP[$spriteName] $ix $slotY $p
        $slotInfo[$name] = @{ icon = $spriteName; x = $ix; y = $slotY; w = 9; h = $SP[$spriteName].Count }
    }

    $gr.Dispose()

    # --- 叠加卡框 ---
    $gr2 = [System.Drawing.Graphics]::FromImage($bmp)
    $gr2.DrawImage($frame, 0, 0)
    $gr2.Dispose()
    $frame.Dispose()

    $target = Join-Path $texDir "$($card.id).png"
    $bmp.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $made++
    if ($card.id -eq 'sheep') {
        $slots = @{
            cardWidth = $FW; cardHeight = $FH
            hole = @($HX0, $HY0, $HX1, $HY1)
            artArea = @($HX0, $ART_Y0, $HX1, $ART_Y1)
            valueBand = @($HX0, $VAL_Y0, $HX1, $VAL_Y1)
            slots = $slotInfo
        }
    }
}
Write-Output "card faces written: $made"
if ($missing.Count -gt 0) { Write-Output "MISSING FRAMES:"; $missing | ForEach-Object { Write-Output " - $_" } }
if ($slots.Count -gt 0) {
    $slotsPath = Join-Path $Root 'art\frames\card_face_slots.json'
    $slots | ConvertTo-Json -Depth 6 | Set-Content -Path $slotsPath -Encoding UTF8
    Write-Output "slots json: $slotsPath"
}
$script:BrushCache.Values | ForEach-Object { $_.Dispose() }
