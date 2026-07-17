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
    if ($base.GetPixel(0, 0).A -ne 0) {
        throw 'Base sprite corner is not transparent'
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
            if ($bitmap.GetPixel(0, 0).A -ne 0) {
                throw "Logo corner is not transparent at $path"
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

            Write-Output "OK $path ($($bitmap.Width)x$($bitmap.Height), transparent, nearest-neighbor)"
        } finally {
            $bitmap.Dispose()
        }
    }
} finally {
    $base.Dispose()
}
