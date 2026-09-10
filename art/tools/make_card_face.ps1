# 从 all.png 取指定卡框，合成一张卡面（上 2/3 立绘 + 下 1/3 数值区），输出原生分辨率 + 放大预览
param(
    [int]$FrameRow = 2,        # 0=末地石 1=黑曜石 2=自然苔藓
    [int]$FrameCol = 0,        # 0=普通(铁) 1=稀有(金) 2=罕见(钻石) 3=传说(下界合金)
    [int]$Cost = 1,
    [int]$Atk = 1,
    [int]$Hp = 3,
    [string]$OutDir = 'D:\cyd\board_games\art\drafts',
    [string]$OutName = 'card_face_sheep'
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Drawing.Drawing2D -ErrorAction SilentlyContinue

$sheet = New-Object System.Drawing.Bitmap 'D:\cyd\board_games\art\reference\all.png'
# 由分析脚本测得的固定网格：列起点 16/80/144/208，行起点 16/96/176，单元 48x64
$cellX = 16 + $FrameCol * 64
$cellY = 16 + $FrameRow * 80
$fw = 48; $fh = 64
$frame = New-Object System.Drawing.Bitmap $fw, $fh
$gF = [System.Drawing.Graphics]::FromImage($frame)
$gF.DrawImage($sheet, (New-Object System.Drawing.Rectangle 0, 0, $fw, $fh), (New-Object System.Drawing.Rectangle $cellX, $cellY, $fw, $fh), [System.Drawing.GraphicsUnit]::Pixel)
$gF.Dispose()

# 找透明洞（内部可绘制区域）
$holeX0 = 0; $holeY0 = 0; $holeX1 = $fw - 1; $holeY1 = $fh - 1
$cx = [int]($fw / 2); $cy = [int]($fh / 2)
for ($x = $cx; $x -ge 0; $x--) { if ($frame.GetPixel($x, $cy).A -gt 40) { $holeX0 = $x + 1; break } }
for ($x = $cx; $x -lt $fw; $x++) { if ($frame.GetPixel($x, $cy).A -gt 40) { $holeX1 = $x - 1; break } }
for ($y = $cy; $y -ge 0; $y--) { if ($frame.GetPixel($cx, $y).A -gt 40) { $holeY0 = $y + 1; break } }
for ($y = $cy; $y -lt $fh; $y++) { if ($frame.GetPixel($cx, $y).A -gt 40) { $holeY1 = $y - 1; break } }
$holeW = $holeX1 - $holeX0 + 1
$holeH = $holeY1 - $holeY0 + 1
Write-Output "frame($FrameRow,$FrameCol) hole=($holeX0,$holeY0)-($holeX1,$holeY1) ${holeW}x${holeH}"

$canvas = New-Object System.Drawing.Bitmap $fw, $fh
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::Transparent)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor

function New-Brush([int]$r, [int]$gg, [int]$b, [int]$a = 255) {
    return (New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($a, $r, $gg, $b)))
}
function Fill-Rect($gr, $brush, [int]$x, [int]$y, [int]$w, [int]$h) {
    $gr.FillRectangle($brush, (New-Object System.Drawing.Rectangle $x, $y, $w, $h))
}

# ============ 立绘区：上 2/3 ============
$artY0 = $holeY0
$artY1 = $holeY0 + [int][Math]::Floor($holeH * 2 / 3) - 1
$artH = $artY1 - $artY0 + 1

# 天空 + 草地（MC 风格色带）
$sky1 = New-Brush 122 176 224      # 天空上
$sky2 = New-Brush 158 203 238      # 天空下
$grassA = New-Brush 106 170 80     # 草上
$grassB = New-Brush 84 141 62      # 草下
$dirt = New-Brush 121 85 58
$skyH = [int]($artH * 0.62)
Fill-Rect $g $sky1 $holeX0 $artY0 $holeW $skyH
Fill-Rect $g $sky2 $holeX0 ($artY0 + [int]($skyH * 0.55)) $holeW ([int]($skyH * 0.45))
$gy = $artY0 + $skyH
Fill-Rect $g $grassA $holeX0 $gy $holeW ($artH - $skyH)
Fill-Rect $g $grassB $holeX0 ($gy + [int](($artH - $skyH) * 0.55)) $holeW ($artH - $skyH - [int](($artH - $skyH) * 0.55))
# 云
$cloud = New-Brush 255 255 255
Fill-Rect $g $cloud ($holeX0 + 3) ($artY0 + 4) 9 2
Fill-Rect $g $cloud ($holeX0 + 5) ($artY0 + 3) 5 1
Fill-Rect $g $cloud ($holeX0 + $holeW - 14) ($artY0 + 9) 8 2

# 羊（白色羊毛身体 + 头 + 腿），坐在草地上
$woolA = New-Brush 236 236 232
$woolB = New-Brush 205 205 200
$face = New-Brush 226 205 192
$legs = New-Brush 176 155 133
$eye = New-Brush 30 30 30
$bodyX = $holeX0 + 6
$bodyY = $gy - 10
Fill-Rect $g $woolA $bodyX $bodyY 20 12
Fill-Rect $g $woolB $bodyX $bodyY 20 3
Fill-Rect $g $woolB ($bodyX + 15) ($bodyY + 3) 5 9
# 蓬松边缘（羊毛块感）
Fill-Rect $g $woolA ($bodyX - 1) ($bodyY + 2) 1 6
Fill-Rect $g $woolA ($bodyX + 20) ($bodyY + 2) 1 5
Fill-Rect $g $woolA ($bodyX + 3) ($bodyY - 1) 5 1
Fill-Rect $g $woolA ($bodyX + 12) ($bodyY - 1) 6 1
# 头（右侧）
$headX = $bodyX + 18
$headY = $bodyY + 1
Fill-Rect $g $face $headX $headY 7 7
Fill-Rect $g (New-Brush 200 180 168) ($headX + 5) ($headY + 3) 2 3
Fill-Rect $g $eye ($headX + 4) ($headY + 2) 1 1
Fill-Rect $g $eye ($headX + 1) ($headY + 2) 1 1
# 腿
Fill-Rect $g $legs ($bodyX + 2) ($bodyY + 12) 3 4
Fill-Rect $g $legs ($bodyX + 7) ($bodyY + 12) 3 4
Fill-Rect $g $legs ($bodyX + 13) ($bodyY + 12) 3 4
Fill-Rect $g $legs ($bodyX + 17) ($bodyY + 12) 3 4

# ============ 数值区：下 1/3 ============
$valY0 = $artY1 + 1
$valH = $holeY1 - $valY0 + 1
$band = New-Brush 28 30 36 235
Fill-Rect $g $band $holeX0 $valY0 $holeW $valH
# 稀有度矿物色包边（普通=铁 银白）
$mineral = switch ($FrameCol) { 0 { New-Brush 200 200 205 } 1 { New-Brush 235 190 70 } 2 { New-Brush 95 220 220 } 3 { New-Brush 90 60 110 } }
Fill-Rect $g $mineral $holeX0 $valY0 $holeW 1
Fill-Rect $g $mineral $holeX0 ($valY0 + $valH - 1) $holeW 1
Fill-Rect $g $mineral $holeX0 $valY0 1 $valH
Fill-Rect $g $mineral ($holeX0 + $holeW - 1) $valY0 1 $valH

# 3x5 像素数字
$digits = @{
    '0' = @('111', '101', '101', '101', '111'); '1' = @('010', '110', '010', '010', '111')
    '2' = @('111', '001', '111', '100', '111'); '3' = @('111', '001', '111', '001', '111')
    '4' = @('101', '101', '111', '001', '001'); '5' = @('111', '100', '111', '001', '111')
    '6' = @('111', '100', '111', '101', '111'); '7' = @('111', '001', '010', '010', '010')
    '8' = @('111', '101', '111', '101', '111'); '9' = @('111', '101', '111', '001', '111')
}
function Draw-Number($gr, [string]$text, [int]$x, [int]$y, $colorBrush, $shadowBrush) {
    $cx2 = $x
    foreach ($ch in $text.ToCharArray()) {
        $rows = $digits["$ch"]
        for ($ry = 0; $ry -lt 5; $ry++) {
            for ($rx = 0; $rx -lt 3; $rx++) {
                if ($rows[$ry][$rx] -eq '1') {
                    $gr.FillRectangle($shadowBrush, (New-Object System.Drawing.Rectangle ($cx2 + $rx + 1), ($y + $ry + 1), 1, 1))
                    $gr.FillRectangle($colorBrush, (New-Object System.Drawing.Rectangle ($cx2 + $rx), ($y + $ry), 1, 1))
                }
            }
        }
        $cx2 = $cx2 + 4
    }
}
$shadow = New-Brush 0 0 0 200
$costCol = New-Brush 120 210 255
$atkCol = New-Brush 255 170 90
$hpCol = New-Brush 255 110 110
# 费用：数值区左上
Draw-Number $g "$Cost" ($holeX0 + 2) ($valY0 + 2) $costCol $shadow
# 攻：数值区左下；血：数值区右下（同一行底部）
$hpText = "$Hp"
$hpW = $hpText.Length * 4 - 1
Draw-Number $g "$Atk" ($holeX0 + 2) ($valY0 + $valH - 7) $atkCol $shadow
Draw-Number $g $hpText ($holeX1 - 1 - $hpW) ($valY0 + $valH - 7) $hpCol $shadow

$g.Dispose()

# 叠加卡框
$g2 = [System.Drawing.Graphics]::FromImage($canvas)
$g2.DrawImage($frame, 0, 0)
$g2.Dispose()

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }
$native = Join-Path $OutDir "$OutName`_${fw}x${fh}.png"
$canvas.Save($native, [System.Drawing.Imaging.ImageFormat]::Png)

# 放大预览（最近邻 x8）
$S = 8
$prev = New-Object System.Drawing.Bitmap ($fw * $S), ($fh * $S)
$gp = [System.Drawing.Graphics]::FromImage($prev)
$gp.Clear([System.Drawing.Color]::FromArgb(255, 24, 24, 28))
$gp.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gp.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$gp.DrawImage($canvas, (New-Object System.Drawing.Rectangle 0, 0, ($fw * $S), ($fh * $S)), (New-Object System.Drawing.Rectangle 0, 0, $fw, $fh), [System.Drawing.GraphicsUnit]::Pixel)
$gp.Dispose()
$preview = Join-Path $OutDir "$OutName`_preview_$($fw * $S)x$($fh * $S).png"
$prev.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output "saved: $native"
Write-Output "saved: $preview"
$prev.Dispose(); $canvas.Dispose(); $frame.Dispose(); $sheet.Dispose()
