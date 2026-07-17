param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$baseSize = 32
$sprite = New-Object System.Drawing.Bitmap(
    $baseSize,
    $baseSize,
    [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($sprite)
$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$graphics.Clear([System.Drawing.Color]::Transparent)

function Convert-HexColor([string]$Hex) {
    $value = $Hex.TrimStart('#')
    return [System.Drawing.Color]::FromArgb(
        255,
        [Convert]::ToInt32($value.Substring(0, 2), 16),
        [Convert]::ToInt32($value.Substring(2, 2), 16),
        [Convert]::ToInt32($value.Substring(4, 2), 16))
}

function Fill-Rect([string]$Hex, [int]$X, [int]$Y, [int]$Width, [int]$Height) {
    $brush = New-Object System.Drawing.SolidBrush((Convert-HexColor $Hex))
    try {
        $graphics.FillRectangle($brush, $X, $Y, $Width, $Height)
    } finally {
        $brush.Dispose()
    }
}

function Fill-Poly([string]$Hex, [int[][]]$Coordinates) {
    $points = foreach ($coordinate in $Coordinates) {
        New-Object System.Drawing.Point($coordinate[0], $coordinate[1])
    }
    $brush = New-Object System.Drawing.SolidBrush((Convert-HexColor $Hex))
    try {
        $graphics.FillPolygon($brush, [System.Drawing.Point[]]$points)
    } finally {
        $brush.Dispose()
    }
}

# Original 32x32 pixel-art emblem: modular steel frame, copper clamps and
# a cyan holographic inspection lens. All coordinates are integer pixels.
Fill-Poly '#15191f' @(
    @(10, 2), @(22, 2), @(22, 4), @(26, 4), @(26, 7), @(29, 7),
    @(29, 24), @(26, 24), @(26, 28), @(22, 28), @(22, 30), @(10, 30),
    @(10, 28), @(6, 28), @(6, 24), @(3, 24), @(3, 7), @(6, 7),
    @(6, 4), @(10, 4))
Fill-Poly '#303944' @(
    @(10, 4), @(22, 4), @(22, 6), @(25, 6), @(25, 9), @(27, 9),
    @(27, 22), @(24, 22), @(24, 26), @(21, 26), @(21, 28), @(11, 28),
    @(11, 26), @(8, 26), @(8, 22), @(5, 22), @(5, 9), @(8, 9),
    @(8, 6), @(10, 6))

# Steel plates and hard pixel highlights.
Fill-Rect '#596674' 10 5 12 2
Fill-Rect '#7c8b98' 12 5 8 1
Fill-Rect '#596674' 6 10 2 11
Fill-Rect '#7c8b98' 6 11 1 7
Fill-Rect '#596674' 24 10 2 11
Fill-Rect '#3b4651' 25 12 1 7
Fill-Rect '#596674' 10 25 12 2
Fill-Rect '#7c8b98' 12 25 7 1

# Copper modular clamps.
Fill-Rect '#7c3f24' 8 3 4 4
Fill-Rect '#c26c35' 9 3 2 3
Fill-Rect '#7c3f24' 20 3 4 4
Fill-Rect '#c26c35' 21 3 2 3
Fill-Rect '#7c3f24' 3 9 4 5
Fill-Rect '#c26c35' 4 10 2 3
Fill-Rect '#7c3f24' 25 9 4 5
Fill-Rect '#c26c35' 26 10 2 3
Fill-Rect '#7c3f24' 3 18 4 5
Fill-Rect '#c26c35' 4 19 2 3
Fill-Rect '#7c3f24' 25 18 4 5
Fill-Rect '#c26c35' 26 19 2 3
Fill-Rect '#7c3f24' 8 25 4 4
Fill-Rect '#c26c35' 9 26 2 2
Fill-Rect '#7c3f24' 20 25 4 4
Fill-Rect '#c26c35' 21 26 2 2

# Holographic lens: stepped diamond with discrete highlights only.
Fill-Poly '#0b2831' @(@(16, 6), @(25, 15), @(16, 25), @(7, 15))
Fill-Poly '#0d6070' @(@(16, 8), @(23, 15), @(16, 23), @(9, 15))
Fill-Poly '#13a8b7' @(@(16, 10), @(21, 15), @(16, 21), @(11, 15))
Fill-Poly '#5be5e8' @(@(16, 11), @(20, 15), @(16, 19), @(12, 15))
Fill-Rect '#d7ffff' 14 12 4 2
Fill-Rect '#86ffff' 13 14 6 3
Fill-Rect '#29cbd2' 15 17 3 2
Fill-Rect '#0d5663' 11 16 2 2
Fill-Rect '#0d5663' 19 13 2 2

# Three material-analysis indicators.
Fill-Rect '#11151a' 12 27 8 2
Fill-Rect '#38d5db' 13 27 2 1
Fill-Rect '#e5a43b' 15 27 2 1
Fill-Rect '#6dc56a' 17 27 2 1

$graphics.Dispose()

function Export-NearestNeighbor([System.Drawing.Bitmap]$Source, [int]$Size, [string]$Path) {
    $directory = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $output = New-Object System.Drawing.Bitmap(
        $Size,
        $Size,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $output.SetResolution(96, 96)
    $canvas = [System.Drawing.Graphics]::FromImage($output)
    try {
        $canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $canvas.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $canvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $canvas.Clear([System.Drawing.Color]::Transparent)
        $canvas.DrawImage(
            $Source,
            (New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)),
            0,
            0,
            $Source.Width,
            $Source.Height,
            [System.Drawing.GraphicsUnit]::Pixel)
        $output.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $canvas.Dispose()
        $output.Dispose()
    }
}

$basePath = Join-Path $ProjectRoot 'docs\assets\tetra-insight-logo-32.png'
$curseForgePath = Join-Path $ProjectRoot 'docs\assets\tetra-insight-logo-512.png'
$modIconPath = Join-Path $ProjectRoot 'src\main\resources\assets\tetra_insight\icon.png'

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $basePath) | Out-Null
$sprite.Save($basePath, [System.Drawing.Imaging.ImageFormat]::Png)
Export-NearestNeighbor $sprite 128 $modIconPath
Export-NearestNeighbor $sprite 512 $curseForgePath
$sprite.Dispose()

Write-Output $basePath
Write-Output $modIconPath
Write-Output $curseForgePath
