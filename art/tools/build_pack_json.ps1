# 从 cards.json 生成完整数据包：cards/index/<id>.json + cards/data/<id>_data.json + lang/zh_cn.json + lang/en_us.json
param([string]$Root = 'D:\cyd\board_games')

$packDir = Join-Path $Root 'src\main\resources\assets\cardduel\custom\default_card_pack\default_pack'
$indexDir = Join-Path $packDir 'cards\index'
$dataDir = Join-Path $packDir 'cards\data'
$langDir = Join-Path $packDir 'lang'
foreach ($d in @($indexDir, $dataDir, $langDir)) { if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force -Path $d | Out-Null } }

$cards = ([System.IO.File]::ReadAllText((Join-Path $Root 'art\tools\cards.json'), [System.Text.Encoding]::UTF8) | ConvertFrom-Json)
$ns = $cards.pack.namespace
$prefix = $cards.pack.langPrefix
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Esc([string]$s) {
    if ($null -eq $s) { return '' }
    return $s.Replace('\', '\\').Replace('"', '\"').Replace("`r", '').Replace("`n", '\n')
}
function Write-Utf8([string]$path, [string]$text) { [System.IO.File]::WriteAllText($path, $text, $utf8) }

$zhLines = New-Object System.Collections.ArrayList
$enLines = New-Object System.Collections.ArrayList
$n = 0
foreach ($c in $cards.cards) {
    $id = $c.id
    # --- index ---
    $idx = @()
    $idx += '{'
    $idx += "  `"name`": `"$prefix$id.name`","
    $idx += "  `"data`": `"${ns}:${id}_data`","
    $idx += "  `"stack_size`": $($cards.pack.stackSize),"
    $idx += "  `"tooltip`": `"$prefix$id.tooltip`","
    $idx += "  `"type`": `"$($c.type)`","
    $idx += "  `"texture`": `"${ns}:$id`""
    $idx += '}'
    Write-Utf8 (Join-Path $indexDir "$id.json") (($idx -join "`n") + "`n")
    # --- data ---
    $dat = @()
    $dat += '{'
    $dat += "  `"hp`": $($c.hp),"
    $dat += "  `"mp`": $($c.mp),"
    $dat += "  `"atk`": $($c.atk),"
    $dat += "  `"description`": `"$prefix$id.description`","
    $dat += "  `"type`": `"$($c.type)`","
    $dat += "  `"skill`": `"$($c.skill)`","
    $dat += "  `"tribe`": `"$($c.tribe)`""
    $dat += '}'
    Write-Utf8 (Join-Path $dataDir "${id}_data.json") (($dat -join "`n") + "`n")
    # --- lang ---
    $tzh = $cards.tribeMeta.($c.tribe).zh
    $rzh = $cards.rarityMeta.($c.rarity).zh
    $ten = $cards.tribeMeta.($c.tribe).en
    $ren = $cards.rarityMeta.($c.rarity).en
    [void]$zhLines.Add("  `"$prefix$id.name`": `"$(Esc $c.zh)`",")
    [void]$zhLines.Add("  `"$prefix$id.description`": `"$(Esc $c.descZh)`",")
    [void]$zhLines.Add("  `"$prefix$id.tooltip`": `"$tzh · $rzh｜$(Esc $c.mc)`",")
    [void]$enLines.Add("  `"$prefix$id.name`": `"$(Esc $c.en)`",")
    [void]$enLines.Add("  `"$prefix$id.description`": `"$(Esc $c.descEn)`",")
    [void]$enLines.Add("  `"$prefix$id.tooltip`": `"$ten · $ren | $(Esc $c.mc)`",")
    $n++
}
# 末项去掉尾逗号
$zhLines[$zhLines.Count - 1] = $zhLines[$zhLines.Count - 1].TrimEnd(',')
$enLines[$enLines.Count - 1] = $enLines[$enLines.Count - 1].TrimEnd(',')
Write-Utf8 (Join-Path $langDir 'zh_cn.json') ("{`n" + ($zhLines -join "`n") + "`n}`n")
Write-Utf8 (Join-Path $langDir 'en_us.json') ("{`n" + ($enLines -join "`n") + "`n}`n")
Write-Output "cards written: $n  (index + data + 2 lang files)"
