# Generates the launcher icon vectors (designs previewed in tools/icons.html).
$res = Join-Path (Split-Path $PSScriptRoot -Parent) 'app\src\main\res'
$utf8 = New-Object Text.UTF8Encoding $false
function Vec($name, $paths) {
    $x = "<vector xmlns:android=`"http://schemas.android.com/apk/res/android`"`n    android:width=`"108dp`" android:height=`"108dp`"`n    android:viewportWidth=`"108`" android:viewportHeight=`"108`">`n$paths</vector>`n"
    [IO.File]::WriteAllText("$res\drawable\$name.xml", $x, $utf8)
}
function F($color, $d) { "    <path android:fillColor=`"$color`" android:pathData=`"$d`"/>`n" }
function S($color, $w, $d, $join = 'miter') { "    <path android:strokeColor=`"$color`" android:strokeWidth=`"$w`" android:strokeLineCap=`"round`" android:strokeLineJoin=`"$join`" android:pathData=`"$d`"/>`n" }

Vec 'ic_launcher_fg_classic' ((F '#FFFFFF' 'M34,28h40a4,4 0 0 1 4,4v50l-5,-4 -5,4 -5,-4 -5,4 -5,-4 -5,4 -5,-4 -5,4 -5,-4 -3,4V32a4,4 0 0 1 4,-4z') + (S '#1A3A8F' 5 'M63,43.5A12,12 0 1 0 63,64.5') + (S '#1A3A8F' 4 'M44,50.5H59M44,57.5H59') + (F '#FFB300' 'M70,26a10,10 0 1 1 0,20a10,10 0 1 1 0,-20z') + (F '#FFFFFF' 'M70,29.5c-3.3,0 -5,2.4 -5,5.5v3.2l-1.6,2.3h13.2l-1.6,-2.3V35c0,-3.1 -1.7,-5.5 -5,-5.5zM68,41.5a2,2 0 0 0 4,0z'))
Vec 'ic_launcher_fg_calendar' ((F '#FFFFFF' 'M36,34h36a6,6 0 0 1 6,6v32a6,6 0 0 1 -6,6h-36a6,6 0 0 1 -6,-6v-32a6,6 0 0 1 6,-6z') + (F '#FF7043' 'M36,34h36a6,6 0 0 1 6,6v6H30v-6a6,6 0 0 1 6,-6z') + (F '#E0F2F1' 'M42.5,28a2.5,2.5 0 0 1 2.5,2.5v7a2.5,2.5 0 0 1 -5,0v-7a2.5,2.5 0 0 1 2.5,-2.5zM65.5,28a2.5,2.5 0 0 1 2.5,2.5v7a2.5,2.5 0 0 1 -5,0v-7a2.5,2.5 0 0 1 2.5,-2.5z') + (S '#00796B' 6 'M42,61l8,8 16,-15' 'round'))
Vec 'ic_launcher_fg_gold' ((F '#FFC107' 'M54,29a25,25 0 1 1 0,50a25,25 0 1 1 0,-50z') + (S '#FF8F00' 2.5 'M54,34a20,20 0 1 1 0,40a20,20 0 1 1 0,-40z') + (S '#3E2723' 5 'M62,44.5A11,11 0 1 0 62,63.5') + (S '#3E2723' 3.6 'M44,51H58M44,57.5H58'))
Vec 'ic_launcher_fg_minimal' ((S '#E1BEE7' 5 'M78,54A24,24 0 1 1 71,37') + (F '#E1BEE7' 'M76.5,29.5L78.5,43 65,41z') + (S '#FFFFFF' 5.5 'M62,44A11.5,11.5 0 1 0 62,64') + (S '#FFFFFF' 4 'M43,51H57M43,57.5H57'))

$icons = [ordered]@{ ic_launcher = @('classic', '#1A3A8F'); ic_launcher_calendar = @('calendar', '#00796B'); ic_launcher_gold = @('gold', '#000000'); ic_launcher_minimal = @('minimal', '#6A1B9A') }
$colors = ''
foreach ($k in $icons.Keys) {
    $id = $icons[$k][0]
    $colors += "    <color name=`"icon_bg_$id`">$($icons[$k][1])</color>`n"
    $x = "<?xml version=`"1.0`" encoding=`"utf-8`"?>`n<adaptive-icon xmlns:android=`"http://schemas.android.com/apk/res/android`">`n    <background android:drawable=`"@color/icon_bg_$id`" />`n    <foreground android:drawable=`"@drawable/ic_launcher_fg_$id`" />`n</adaptive-icon>`n"
    [IO.File]::WriteAllText("$res\mipmap-anydpi-v26\$k.xml", $x, $utf8)
}
[IO.File]::WriteAllText("$res\values\icon_colors.xml", "<resources>`n$colors</resources>`n", $utf8)
