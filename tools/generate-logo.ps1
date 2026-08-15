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

function Draw-PixelLine(
    [string]$Hex,
    [int]$X1,
    [int]$Y1,
    [int]$X2,
    [int]$Y2
) {
    $x = $X1
    $y = $Y1
    $dx = [Math]::Abs($X2 - $X1)
    $sx = if ($X1 -lt $X2) { 1 } else { -1 }
    $dy = -[Math]::Abs($Y2 - $Y1)
    $sy = if ($Y1 -lt $Y2) { 1 } else { -1 }
    $error = $dx + $dy

    while ($true) {
        Fill-Rect $Hex $x $y 1 1
        if ($x -eq $X2 -and $y -eq $Y2) {
            break
        }
        $twiceError = 2 * $error
        if ($twiceError -ge $dy) {
            $error += $dy
            $x += $sx
        }
        if ($twiceError -le $dx) {
            $error += $dx
            $y += $sy
        }
    }
}

# Original 32x32 pixel rune. Tetra's own mark reads more like a sparse carved
# totem than a conventional badge, so every structural stroke is exactly one
# logical pixel wide. The diamond remains, but it is a broken inscription line
# rather than a heavy frame.
Draw-PixelLine '#ffffff' 7 14 14 7
Draw-PixelLine '#ffffff' 18 7 25 14
Draw-PixelLine '#808080' 25 18 18 25
Draw-PixelLine '#ffffff' 14 25 7 18

# Detached axis marks give the diamond the modular, runic cadence of Tetra's
# original symbol without thickening its outline.
Fill-Rect '#ffffff' 16 2 1 1
Draw-PixelLine '#ffffff' 16 4 16 5
Fill-Rect '#ffffff' 29 16 1 1
Draw-PixelLine '#ffffff' 27 16 28 16
Fill-Rect '#808080' 16 29 1 1
Draw-PixelLine '#808080' 16 27 16 28
Fill-Rect '#ffffff' 2 16 1 1
Draw-PixelLine '#ffffff' 4 16 5 16

# The center is one thin eye rune, not a filled eye or a second diamond.
Draw-PixelLine '#ffffff' 10 16 14 13
Draw-PixelLine '#ffffff' 14 13 18 13
Draw-PixelLine '#ffffff' 18 13 22 16
Draw-PixelLine '#808080' 10 18 14 21
Draw-PixelLine '#808080' 14 21 18 21
Draw-PixelLine '#808080' 18 21 22 18
Fill-Rect '#ffffff' 16 16 1 2

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
        $canvas.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $canvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $canvas.Clear([System.Drawing.Color]::Black)
        $canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
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

function Export-Cover([System.Drawing.Bitmap]$Source, [string]$Path) {
    $width = 1920
    $height = 1080
    $logoSize = 320
    $logoX = [int](($width - $logoSize) / 2)
    $logoY = [int](($height - $logoSize) / 2)
    $directory = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $output = New-Object System.Drawing.Bitmap(
        $width,
        $height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $output.SetResolution(96, 96)
    $canvas = [System.Drawing.Graphics]::FromImage($output)
    try {
        $canvas.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $canvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $canvas.Clear([System.Drawing.Color]::Black)
        $canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
        $canvas.DrawImage(
            $Source,
            (New-Object System.Drawing.Rectangle($logoX, $logoY, $logoSize, $logoSize)),
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
$coverPath = Join-Path $ProjectRoot 'docs\assets\tetra-insight-cover-1920x1080.png'

Export-NearestNeighbor $sprite 32 $basePath
Export-NearestNeighbor $sprite 128 $modIconPath
Export-NearestNeighbor $sprite 512 $curseForgePath
Export-Cover $sprite $coverPath
$sprite.Dispose()

Write-Output $basePath
Write-Output $modIconPath
Write-Output $curseForgePath
Write-Output $coverPath
