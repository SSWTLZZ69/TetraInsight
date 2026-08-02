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

# Original 32x32 pixel-art emblem: a Tetra-family diamond plate containing
# a monochrome inspection iris. All coordinates are integer pixels.

# Deep silhouette. The palette deliberately mirrors Tetra-family addon marks:
# pure black, pure white and one neutral 50% gray.
Fill-Poly '#000000' @(@(16, 0), @(32, 16), @(16, 32), @(0, 16))

# Thin segmented schematic rim. White describes the lit upper facets while
# gray is reserved for the lower shadow; the plate itself remains black.
Fill-Poly '#ffffff' @(@(16, 1), @(31, 16), @(29, 18), @(14, 3))
Fill-Poly '#ffffff' @(@(1, 16), @(16, 1), @(18, 3), @(3, 18))
Fill-Poly '#808080' @(@(31, 16), @(16, 31), @(14, 29), @(29, 14))
Fill-Poly '#808080' @(@(16, 31), @(1, 16), @(3, 14), @(18, 29))
Fill-Poly '#000000' @(@(16, 4), @(28, 16), @(16, 28), @(4, 16))

# Four Tetra-like connector tabs and their single-pixel signal lamps.
Fill-Rect '#000000' 14 0 4 4
Fill-Rect '#ffffff' 15 1 2 2
Fill-Rect '#000000' 28 14 4 4
Fill-Rect '#ffffff' 29 15 2 2
Fill-Rect '#000000' 14 28 4 4
Fill-Rect '#808080' 15 29 2 2
Fill-Rect '#000000' 0 14 4 4
Fill-Rect '#ffffff' 1 15 2 2

# White line-art inspection eye on the black plate.
Fill-Poly '#ffffff' @(
    @(6, 16), @(10, 12), @(15, 10), @(17, 10), @(22, 12), @(26, 16),
    @(22, 20), @(17, 22), @(15, 22), @(10, 20))
Fill-Poly '#000000' @(
    @(9, 16), @(12, 14), @(16, 12), @(20, 14), @(23, 16),
    @(20, 18), @(16, 20), @(12, 18))

# Faceted lens, pupil and glint.
Fill-Poly '#808080' @(@(16, 12), @(20, 16), @(16, 20), @(12, 16))
Fill-Poly '#ffffff' @(@(16, 13), @(19, 16), @(16, 19), @(13, 16))
Fill-Rect '#000000' 15 15 3 3
Fill-Rect '#ffffff' 16 15 1 1

# Compact analysis notches and the three grayscale material channels.
Fill-Rect '#808080' 16 9 1 2
Fill-Rect '#808080' 16 21 1 2
Fill-Rect '#808080' 6 16 2 1
Fill-Rect '#808080' 24 16 2 1
Fill-Rect '#ffffff' 12 23 2 1
Fill-Rect '#808080' 15 23 2 1
Fill-Rect '#000000' 18 23 2 1

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
