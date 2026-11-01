$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskLogo=[System.Drawing.Bitmap]::new((Join-Path (Get-Location) 'public/brand/yorinovi-logo.png'))
try {
    foreach($taskSize in @(192,512)) {
        $taskBitmap=[System.Drawing.Bitmap]::new($taskSize,$taskSize)
        $taskGraphics=[System.Drawing.Graphics]::FromImage($taskBitmap)
        try {$taskGraphics.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic;$taskGraphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#0b0a10'));$taskGraphics.DrawImage($taskLogo,0,0,$taskSize,$taskSize);$taskBitmap.Save((Join-Path (Get-Location) "public/icon-$taskSize.png"),[System.Drawing.Imaging.ImageFormat]::Png)} finally {$taskGraphics.Dispose();$taskBitmap.Dispose()}
    }
    Copy-Item -LiteralPath public/icon-512.png -Destination android/app/src/main/res/drawable/yorinovi_logo.png -Force
    foreach($taskDensity in @(@('mdpi',48),@('hdpi',72),@('xhdpi',96),@('xxhdpi',144),@('xxxhdpi',192))) {
        $taskSize=[int]$taskDensity[1];$taskBitmap=[System.Drawing.Bitmap]::new($taskSize,$taskSize);$taskGraphics=[System.Drawing.Graphics]::FromImage($taskBitmap)
        try {$taskGraphics.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic;$taskGraphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#0b0a10'));$taskGraphics.DrawImage($taskLogo,0,0,$taskSize,$taskSize);foreach($taskName in @('ic_launcher.png','ic_launcher_round.png')){$taskBitmap.Save((Join-Path (Get-Location) ('android/app/src/main/res/mipmap-'+$taskDensity[0]+'/'+$taskName)),[System.Drawing.Imaging.ImageFormat]::Png)}} finally {$taskGraphics.Dispose();$taskBitmap.Dispose()}
    }
} finally {$taskLogo.Dispose()}
