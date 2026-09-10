# 分析 all.png 卡框样图：输出 12 个框的包围盒、边框主色、宝石色、内部透明度
Add-Type -AssemblyName System.Drawing
$src = 'D:\cyd\board_games\art\reference\all.png'
$bmp = New-Object System.Drawing.Bitmap $src
$W = [int]$bmp.Width
$H = [int]$bmp.Height
Write-Output "image: ${W}x${H} pixels format=$($bmp.PixelFormat)"

function Test-Content {
    param([System.Drawing.Color]$c)
    if ($script:hasAlpha) { return ($c.A -gt 20) }
    $isWhite = ($c.R -gt 245) -and ($c.G -gt 245) -and ($c.B -gt 245)
    return (-not $isWhite)
}

# 背景是否透明
$script:hasAlpha = $false
for ($yy = 0; $yy -lt $H; $yy += 5) {
    for ($xx = 0; $xx -lt $W; $xx += 5) {
        $px = $bmp.GetPixel($xx, $yy)
        if ($px.A -lt 250) { $script:hasAlpha = $true; break }
    }
    if ($script:hasAlpha) { break }
}
Write-Output "transparentBg=$($script:hasAlpha)"

# 行投影
$rowHit = New-Object 'int[]' $H
for ($yy = 0; $yy -lt $H; $yy++) {
    $n = 0
    for ($xx = 0; $xx -lt $W; $xx++) {
        $px = $bmp.GetPixel($xx, $yy)
        if (Test-Content -c $px) { $n = $n + 1 }
    }
    $rowHit[$yy] = $n
}
$rowBands = New-Object System.Collections.ArrayList
$inBand = $false
$bandStart = 0
for ($yy = 0; $yy -lt $H; $yy++) {
    if (($rowHit[$yy] -gt 2) -and (-not $inBand)) { $inBand = $true; $bandStart = $yy }
    elseif (($rowHit[$yy] -le 2) -and $inBand) {
        $inBand = $false
        [void]$rowBands.Add(@($bandStart, ($yy - 1)))
    }
}
if ($inBand) { [void]$rowBands.Add(@($bandStart, ($H - 1))) }
$txt = ($rowBands | ForEach-Object { "$($_[0])-$($_[1])" }) -join ', '
Write-Output "rowBands($($rowBands.Count)): $txt"

# 每行带 → 列带 → 单元格
$cells = New-Object System.Collections.ArrayList
foreach ($b in $rowBands) {
    $y0 = [int]$b[0]
    $y1 = [int]$b[1]
    $colHit = New-Object 'int[]' $W
    for ($xx = 0; $xx -lt $W; $xx++) {
        $n = 0
        for ($yy = $y0; $yy -le $y1; $yy++) {
            $px = $bmp.GetPixel($xx, $yy)
            if (Test-Content -c $px) { $n = $n + 1 }
        }
        $colHit[$xx] = $n
    }
    $inCol = $false
    $colStart = 0
    for ($xx = 0; $xx -lt $W; $xx++) {
        if (($colHit[$xx] -gt 1) -and (-not $inCol)) { $inCol = $true; $colStart = $xx }
        elseif (($colHit[$xx] -le 1) -and $inCol) {
            $inCol = $false
            [void]$cells.Add(@($colStart, $y0, ($xx - 1), $y1))
        }
    }
    if ($inCol) { [void]$cells.Add(@($colStart, $y0, ($W - 1), $y1)) }
}
Write-Output "cells: $($cells.Count)"
$idx = 0
foreach ($c in $cells) {
    $cx0 = [int]$c[0]; $cy0 = [int]$c[1]; $cx1 = [int]$c[2]; $cy1 = [int]$c[3]
    $cw = $cx1 - $cx0 + 1
    $ch = $cy1 - $cy0 + 1
    $midX = [int](($cx0 + $cx1) / 2)
    $edge = $bmp.GetPixel($midX, ($cy0 + 1))
    $center = $bmp.GetPixel($midX, [int](($cy0 + $cy1) / 2))
    # 右侧 35% 区域找最饱和像素（宝石）
    $gem = $null
    for ($yy = $cy0; $yy -le $cy1; $yy++) {
        for ($xx = [int]($cx1 - $cw * 0.35); $xx -le $cx1; $xx++) {
            $px = $bmp.GetPixel($xx, $yy)
            if (-not (Test-Content -c $px)) { continue }
            $mx = [Math]::Max($px.R, [Math]::Max($px.G, $px.B))
            $mn = [Math]::Min($px.R, [Math]::Min($px.G, $px.B))
            if (($mx - $mn) -gt 45) { $gem = $px; break }
        }
        if ($gem) { break }
    }
    $gemTxt = 'none'
    if ($gem) { $gemTxt = '#{0:X2}{1:X2}{2:X2}' -f $gem.R, $gem.G, $gem.B }
    Write-Output ("[{0,2}] ({1,3},{2,3})-({3,3},{4,3}) size={5,3}x{6,3} aspect={7:N2} edge=#{8:X2}{9:X2}{10:X2} centerA={11,3} gem=#{12}" -f $idx, $cx0, $cy0, $cx1, $cy1, $cw, $ch, ($cw / [double]$ch), $edge.R, $edge.G, $edge.B, $center.A, $gemTxt)
    $idx = $idx + 1
}
$bmp.Dispose()
