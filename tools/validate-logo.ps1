param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$basePath = Join-Path $ProjectRoot 'docs\assets\tetra-insight-logo-32.png'
$targets = @(
    @{ Path = Join-Path $ProjectRoot 'src\main\resources\assets\tetra_insight\icon.png'; Scale = 4 },
    @{ Path = Join-Path $ProjectRoot 'docs\assets\tetra-insight-logo-512.png'; Scale = 16 }
)

$base = [System.Drawing.Bitmap]::FromFile($basePath)
try {
    if ($base.Width -ne 32 -or $base.Height -ne 32) {
        throw "Expected 32x32 base sprite, got $($base.Width)x$($base.Height)"
    }
    $baseCorner = $base.GetPixel(0, 0)
    if ($baseCorner.A -ne 255 -or $baseCorner.R -ne 0 -or $baseCorner.G -ne 0 -or $baseCorner.B -ne 0) {
        throw 'Base sprite corner is not opaque black'
    }

    foreach ($target in $targets) {
        $path = [string]$target['Path']
        $scale = [int]$target['Scale']
        $bitmap = [System.Drawing.Bitmap]::FromFile($path)
        try {
            $expectedSize = 32 * $scale
            if ($bitmap.Width -ne $expectedSize -or $bitmap.Height -ne $expectedSize) {
                throw "Unexpected logo size at $path"
            }
            $corner = $bitmap.GetPixel(0, 0)
            if ($corner.A -ne 255 -or $corner.R -ne 0 -or $corner.G -ne 0 -or $corner.B -ne 0) {
                throw "Logo corner is not opaque black at $path"
            }

            for ($logicalY = 0; $logicalY -lt 32; $logicalY++) {
                for ($logicalX = 0; $logicalX -lt 32; $logicalX++) {
                    $expected = $base.GetPixel($logicalX, $logicalY).ToArgb()
                    $left = $logicalX * $scale
                    $top = $logicalY * $scale
                    $samples = @(
                        @($left, $top),
                        @(($left + $scale - 1), $top),
                        @($left, ($top + $scale - 1)),
                        @(($left + $scale - 1), ($top + $scale - 1)),
                        @(($left + [int]($scale / 2)), ($top + [int]($scale / 2)))
                    )
                    foreach ($sample in $samples) {
                        if ($bitmap.GetPixel($sample[0], $sample[1]).ToArgb() -ne $expected) {
                            throw "Non-nearest-neighbor pixel at $path`: $($sample[0]),$($sample[1])"
                        }
                    }
                }
            }

            Write-Output "OK $path ($($bitmap.Width)x$($bitmap.Height), black background, nearest-neighbor)"
        } finally {
            $bitmap.Dispose()
        }
    }
} finally {
    $base.Dispose()
}

$coverPath = Join-Path $ProjectRoot 'docs\assets\tetra-insight-cover-1920x1080.png'
$cover = [System.Drawing.Bitmap]::FromFile($coverPath)
try {
    if ($cover.Width -ne 1920 -or $cover.Height -ne 1080) {
        throw "Expected 1920x1080 cover, got $($cover.Width)x$($cover.Height)"
    }
    foreach ($point in @(@(0, 0), @(1919, 0), @(0, 1079), @(1919, 1079))) {
        $pixel = $cover.GetPixel($point[0], $point[1])
        if ($pixel.A -ne 255 -or $pixel.R -ne 0 -or $pixel.G -ne 0 -or $pixel.B -ne 0) {
            throw "Cover corner is not opaque black at $($point[0]),$($point[1])"
        }
    }
    Write-Output "OK $coverPath (1920x1080, black background, centered nearest-neighbor rune)"
} finally {
    $cover.Dispose()
}
