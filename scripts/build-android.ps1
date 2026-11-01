$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
$taskRoot=(Resolve-Path '.').Path
$taskTools=Join-Path $taskRoot '.android-build'
if(!$taskTools.StartsWith($taskRoot+'\')){throw 'Build directory outside workspace'}
node scripts/prepare-android-tools.js
if($LASTEXITCODE -ne 0){throw 'Portable build tools download failed'}
if(!(Test-Path (Join-Path $taskTools 'jdk'))){Expand-Archive -LiteralPath (Join-Path $taskTools 'jdk.zip') -DestinationPath (Join-Path $taskTools 'jdk')}
$taskJdk=(Get-ChildItem -LiteralPath (Join-Path $taskTools 'jdk') -Directory | Where-Object {Test-Path (Join-Path $_.FullName 'bin/java.exe')} | Select-Object -First 1).FullName
if(!$taskJdk){throw 'JDK archive did not contain Java'}
$env:JAVA_HOME=$taskJdk
$env:ANDROID_HOME=Join-Path $taskTools 'sdk'
$env:ANDROID_USER_HOME=Join-Path $taskTools 'android-user'
$env:GRADLE_USER_HOME=Join-Path $taskTools 'gradle'
$taskTmpDir=Join-Path $taskTools 'tmp'
New-Item -ItemType Directory -Force -Path $taskTmpDir | Out-Null
# Windows JDK socket paths have a short limit; use a project-local directory.
$env:JAVA_TOOL_OPTIONS='-Djava.net.preferIPv4Stack=true -Djdk.net.unixdomain.tmpdir="'+$taskTmpDir+'" -Djava.io.tmpdir="'+$taskTmpDir+'"'
if(!(Test-Path (Join-Path $env:ANDROID_HOME 'cmdline-tools/latest/bin/sdkmanager.bat'))){
    Expand-Archive -LiteralPath (Join-Path $taskTools 'cmdline.zip') -DestinationPath (Join-Path $taskTools 'cmdline-unpacked') -Force
    New-Item -ItemType Directory -Force -Path (Join-Path $env:ANDROID_HOME 'cmdline-tools/latest') | Out-Null
    Copy-Item -Path (Join-Path $taskTools 'cmdline-unpacked/cmdline-tools/*') -Destination (Join-Path $env:ANDROID_HOME 'cmdline-tools/latest') -Recurse -Force
}
$taskSdkManager=Join-Path $env:ANDROID_HOME 'cmdline-tools/latest/bin/sdkmanager.bat'
& $taskSdkManager --sdk_root=$env:ANDROID_HOME 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
if($LASTEXITCODE -ne 0){throw 'Android SDK package installation failed'}
npm run android:sync
if($LASTEXITCODE -ne 0){throw 'Web build / native sync failed'}
$taskSdkPath=$env:ANDROID_HOME.Replace('\','/')
Set-Content -LiteralPath android/local.properties -Value "sdk.dir=$taskSdkPath"
Push-Location android
try { & .\gradlew.bat --no-daemon "-Duser.home=$taskTools/android-user" assembleDebug } finally { Pop-Location }
if($LASTEXITCODE -ne 0){throw 'Android APK build failed'}
New-Item -ItemType Directory -Force -Path artifacts | Out-Null
Copy-Item -LiteralPath android/app/build/outputs/apk/debug/app-debug.apk -Destination artifacts/YoriNovi-debug.apk -Force
Write-Output 'APK: artifacts/YoriNovi-debug.apk'
