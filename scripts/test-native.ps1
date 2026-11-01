$ErrorActionPreference = 'Stop'
$taskJdk = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) { $env:JAVA_HOME } elseif (Test-Path 'C:\Program Files\Java\jdk-17\bin\javac.exe') { 'C:\Program Files\Java\jdk-17' } else { throw 'JAVA_HOME değişkenini bir JDK dizinine ayarla.' }
node scripts/write-catalog.js
if (!(Test-Path 'artifacts/native-test/golden.json') -or !(Test-Path 'artifacts/native-test/jsoup.jar')) { node scripts/prepare-native-test.js }
if ($LASTEXITCODE -ne 0) { throw 'Native test hazırlığı başarısız.' }
& (Join-Path $taskJdk 'bin/javac.exe') -encoding UTF-8 -cp 'artifacts/native-test/jsoup.jar;artifacts/native-test/json.jar' -d artifacts/native-test/classes android/app/src/main/java/com/fallowthis/app/PhoneCore.java tests/native/NativeCoreTest.java
if ($LASTEXITCODE -ne 0) { throw 'Java çekirdek derlemesi başarısız.' }
& (Join-Path $taskJdk 'bin/java.exe') -cp 'artifacts/native-test/classes;artifacts/native-test/jsoup.jar;artifacts/native-test/json.jar' NativeCoreTest
if ($LASTEXITCODE -ne 0) { throw 'Java çekirdek testleri başarısız.' }
