# Card Duel 卡面生成器：① 24 个卡框（你的 12 个实画 + 3 个未画派系同风格补全，均补四角宝石）
#                    ② 54 张卡面（上 2/3 立绘 + 下 1/3 数值区：矿物包边 + 三槽预留 攻/血/费）
param(
    [string]$Root = 'D:\cyd\board_games',
    [switch]$SkipFrames
)

Add-Type -AssemblyName System.Drawing
$packDir = Join-Path $Root 'src\main\resources\assets\cardduel\custom\default_card_pack\default_pack'
$frameDir = Join-Path $Root 'art\frames'
$texDir = Join-Path $packDir 'textures'
foreach ($d in @($frameDir, $texDir)) { if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force -Path $d | Out-Null } }

$jsonPath = Join-Path $Root 'art\tools\cards.json'
$cards = ([System.IO.File]::ReadAllText($jsonPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json)
$rarityMeta = $cards.rarityMeta
$tribeMeta = $cards.tribeMeta

function New-Brush([int]$r, [int]$gg, [int]$b, [int]$a = 255) { New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, $r, $gg, $b)) }
function Hex-Brush([string]$hex, [int]$a = 255) {
    $h = $hex.TrimStart('#')
    $r = [Convert]::ToInt32($h.Substring(0, 2), 16); $gg = [Convert]::ToInt32($h.Substring(2, 2), 16); $b = [Convert]::ToInt32($h.Substring(4, 2), 16)
    New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, $r, $gg, $b))
}
function Hex-Color([string]$hex, [int]$a = 255) {
    $h = $hex.TrimStart('#')
    $r = [Convert]::ToInt32($h.Substring(0, 2), 16); $gg = [Convert]::ToInt32($h.Substring(2, 2), 16); $b = [Convert]::ToInt32($h.Substring(4, 2), 16)
    [System.Drawing.Color]::FromArgb($a, $r, $gg, $b)
}
function Shift-Color([System.Drawing.Color]$c, [double]$f) {
    $r = [Math]::Min(255, [Math]::Max(0, [int]($c.R + 255 * $f)))
    $gg = [Math]::Min(255, [Math]::Max(0, [int]($c.G + 255 * $f)))
    $b = [Math]::Min(255, [Math]::Max(0, [int]($c.B + 255 * $f)))
    [System.Drawing.Color]::FromArgb($c.A, $r, $gg, $b)
}
function FR($gr, $brush, [int]$x, [int]$y, [int]$w, [int]$h) { $gr.FillRectangle($brush, (New-Object System.Drawing.Rectangle $x, $y, $w, $h)) }

# ---------- 确定性伪随机（按卡 id 播种） ----------
function New-Rng([string]$seedText) {
    $h = 7
    foreach ($ch in $seedText.ToCharArray()) { $h = (($h * 31) + [int]$ch) % 2147483647 }
    return @{ s = [int64]$h }
}
function Rng-Next($rng, [int]$max) {
    $rng.s = ($rng.s * 1103515245 + 12345) -band 0x7FFFFFFF
    return [int](($rng.s / 65536) % $max)
}

# =====================================================================
# 一、卡框：48x64，内洞 (6,5)-(43,55)
# =====================================================================
$FW = 48; $FH = 64
$HX0 = 6; $HY0 = 5; $HX1 = 43; $HY1 = 55
$script:Frames = @{}

function Add-CornerGems($bmp, [string]$gemHex) {
    $col = Hex-Color $gemHex
    $edge = Shift-Color $col -0.45
    $shape = @('.X.', 'XXX', '.X.')
    $spots = @(@(4, 3), @(45, 3), @(4, 57), @(45, 57))
    foreach ($s in $spots) {
        $ox = $s[0] - 1; $oy = $s[1] - 1
        for ($ry = 0; $ry -lt 3; $ry++) {
            for ($rx = 0; $rx -lt 3; $rx++) {
                if ($shape[$ry][$rx] -eq 'X') {
                    $c = $col
                    if ($ry -eq 0 -or ($ry -eq 1 -and $rx -eq 0)) { $c = Shift-Color $col 0.25 }
                    $bmp.SetPixel(($ox + $rx), ($oy + $ry), $c)
                }
            }
        }
        $bmp.SetPixel(($ox + 1), ($oy + 3), $edge)
    }
}

function New-Diamond($bmp, [int]$cx, [int]$cy, [int]$size, [System.Drawing.Color]$col) {
    $half = [int][Math]::Floor($size / 2)
    for ($dy = -$half; $dy -le $half; $dy++) {
        $w = $half - [Math]::Abs($dy)
        for ($dx = -$w; $dx -le $w; $dx++) {
            $c = $col
            if ($dy -le -$half + 1 -and $dx -le 0) { $c = Shift-Color $col 0.3 }
            if ($dy -ge $half - 1) { $c = Shift-Color $col -0.3 }
            $bmp.SetPixel(($cx + $dx), ($cy + $dy), $c)
        }
    }
}

$factionStyle = @{
    'void'     = @{ base = '#3A4048'; dark = '#2A2F35'; light = '#4E565E'; accent = '#1FA9A0'; speckle = '#57D6C8' }
    'redstone' = @{ base = '#4A4A50'; dark = '#33333A'; light = '#5E5E66'; accent = '#8A1A1A'; speckle = '#C02020' }
    'steve'    = @{ base = '#8B6239'; dark = '#5E4126'; light = '#A87A4A'; accent = '#C0C0C4'; speckle = '#B8A88A' }
}

function New-ProceduralFrame([string]$tribe) {
    $st = $factionStyle[$tribe]
    $bmp = New-Object System.Drawing.Bitmap $FW, $FH
    $base = Hex-Color $st.base; $dark = Hex-Color $st.dark; $light = Hex-Color $st.light
    $rng = New-Rng "frame-$tribe"
    for ($y = 0; $y -lt $FH; $y++) {
        for ($x = 0; $x -lt $FW; $x++) {
            $insideHole = ($x -ge $HX0 -and $x -le $HX1 -and $y -ge $HY0 -and $y -le $HY1)
            if ($insideHole) { continue }
            $c = $base
            # 材质纹理：噪点
            $n = Rng-Next $rng 100
            if ($n -lt 22) { $c = $dark } elseif ($n -lt 40) { $c = $light }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    # 外轮廓 1px 深色
    for ($x = 0; $x -lt $FW; $x++) { $bmp.SetPixel($x, 0, $dark); $bmp.SetPixel($x, ($FH - 1), $dark) }
    for ($y = 0; $y -lt $FH; $y++) { $bmp.SetPixel(0, $y, $dark); $bmp.SetPixel(($FW - 1), $y, $dark) }
    # 内洞外沿 1px 深色（立体感）
    for ($x = ($HX0 - 1); $x -le ($HX1 + 1); $x++) { $bmp.SetPixel($x, ($HY0 - 1), $dark); $bmp.SetPixel($x, ($HY1 + 1), $dark) }
    for ($y = ($HY0 - 1); $y -le ($HY1 + 1); $y++) { $bmp.SetPixel(($HX0 - 1), $y, $dark); $bmp.SetPixel(($HX1 + 1), $y, $dark) }
    # 顶部派系点缀条
    $acc = Hex-Color $st.accent
    for ($x = 4; $x -le ($FW - 5); $x++) { if (($x % 2) -eq 0) { $bmp.SetPixel($x, 2, $acc) } }
    # 派系斑点（红石发光颗粒等）
    $spk = Hex-Color $st.speckle
    $rng2 = New-Rng "frame-spk-$tribe"
    for ($i = 0; $i -lt 40; $i++) {
        $x = 1 + (Rng-Next $rng2 ($FW - 2)); $y = 1 + (Rng-Next $rng2 ($FH - 2))
        $insideHole = ($x -ge $HX0 -and $x -le $HX1 -and $y -ge $HY0 -and $y -le $HY1)
        if (-not $insideHole) { $bmp.SetPixel($x, $y, $spk) }
    }
    # 派系特征
    if ($tribe -eq 'steve') {
        $stud = Hex-Color '#C8C8CC'
        foreach ($p in @(@(2, 2), @(45, 2), @(2, 61), @(45, 61))) { $bmp.SetPixel($p[0], $p[1], $stud) }
    }
    if ($tribe -eq 'void') {
        $acc2 = Hex-Color $st.accent
        for ($y = 57; $y -le 61; $y++) { if (($y % 2) -eq 1) { $bmp.SetPixel(3, $y, $acc2); $bmp.SetPixel(44, $y, $acc2) } }
    }
    return $bmp
}

# --- 生成 24 个框 ---
$tribes = @('nature', 'nether', 'end', 'void', 'redstone', 'steve')
$rarities = @('common', 'rare', 'epic', 'legendary')
$sheet = New-Object System.Drawing.Bitmap (Join-Path $Root 'art\reference\all.png')
$drawnRows = @{ 'end' = 0; 'nether' = 1; 'nature' = 2 }

if (-not $SkipFrames) {
    foreach ($tribe in $tribes) {
        for ($ri = 0; $ri -lt 4; $ri++) {
            $rarity = $rarities[$ri]
            $bmp = $null
            if ($drawnRows.ContainsKey($tribe)) {
                $cx = 16 + $ri * 64
                $cy = 16 + $drawnRows[$tribe] * 80
                $bmp = New-Object System.Drawing.Bitmap $FW, $FH
                $gg = [System.Drawing.Graphics]::FromImage($bmp)
                $srcRect = New-Object System.Drawing.Rectangle $cx, $cy, $FW, $FH
                $dstRect = New-Object System.Drawing.Rectangle 0, 0, $FW, $FH
                $gg.DrawImage($sheet, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
                $gg.Dispose()
            } else {
                $bmp = New-ProceduralFrame $tribe
                New-Diamond $bmp 24 58 5 (Hex-Color $rarityMeta.$rarity.gem)
            }
            Add-CornerGems $bmp $rarityMeta.$rarity.gem
            $bmp.Save((Join-Path $frameDir "frame_${tribe}_${rarity}.png"), [System.Drawing.Imaging.ImageFormat]::Png)
            $script:Frames["${tribe}_${rarity}"] = $bmp
            Write-Output "frame: frame_${tribe}_${rarity}.png"
        }
    }
} else {
    foreach ($tribe in $tribes) {
        foreach ($rarity in $rarities) {
            $p = Join-Path $frameDir "frame_${tribe}_${rarity}.png"
            $script:Frames["${tribe}_${rarity}"] = New-Object System.Drawing.Bitmap $p
        }
    }
}
$sheet.Dispose()
Write-Output "frames ready: $($script:Frames.Count)"
