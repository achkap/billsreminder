# Wraps Greek string literals in tr("el", "en") using tools/translations.txt (UTF-8).
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$ui = Join-Path $root 'app\src\main\java\gr\logariasmoi\ui'
$utf8 = New-Object Text.UTF8Encoding $false
$file = $null; $text = $null
$flush = {
    if ($file) {
        if ($text -notmatch 'import gr\.logariasmoi\.data\.tr\r?\n') {
            $idx = $text.IndexOf("`nimport ")
            $text = $text.Insert($idx + 1, "import gr.logariasmoi.data.tr`n")
        }
        [IO.File]::WriteAllText($file, $text, $utf8)
    }
}
foreach ($line in [IO.File]::ReadAllLines((Join-Path $PSScriptRoot 'translations.txt'), [Text.Encoding]::UTF8)) {
    if (-not $line) { continue }
    if ($line.StartsWith('## ')) {
        & $flush
        $file = Join-Path $ui $line.Substring(3).Trim()
        $text = [IO.File]::ReadAllText($file, [Text.Encoding]::UTF8)
        continue
    }
    $parts = $line -split '\|\|', 2
    $el = $parts[0]; $en = $parts[1]
    $pattern = '(?<!tr\()"' + [regex]::Escape($el) + '"'
    $replacement = 'tr("' + $el + '", "' + $en + '")'
    $count = [regex]::Matches($text, $pattern).Count
    if ($count -eq 0) { Write-Output ("MISSING in " + (Split-Path $file -Leaf) + ": " + $el) }
    $text = [regex]::Replace($text, $pattern, [System.Text.RegularExpressions.MatchEvaluator] { param($m) $replacement })
}
& $flush
