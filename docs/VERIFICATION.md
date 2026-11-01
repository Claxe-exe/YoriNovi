# YoriNovi doğrulama kaydı

Tarih: **2 Kasım 2026 — Europe/Istanbul**.

Bu dosya telefon merkezli sürümü açıklar. Önceki sunucu testleri yerelde `legacy/docs/` altındadır ve yeni sürümün kanıtı sayılmaz.

## Doğrulananlar

- Yerel JavaScript veri zinciri, gerçek resmî kaynaklardan veri alıp `data/local-check.json` dosyasına kaydetti. Genişletilen 8 kaynakla **19 gerçek olay** oluştu; son tekrar taramasında **19 → 19**, eklenen 0.
- Kaynak listesi sonradan 8 resmî kaynağa / 10 konuya genişletildi. Oshi no Ko, Chainsaw Man ve 86 sitelerine doğrudan erişim doğrulandı; SPY×FAMILY HTTP 403 verdiği için etkin değil.
- Android'in kullandığı `PhoneCore.java` JDK üzerinde derlendi. 12 JS/native sınıflandırma senaryosu, 42 olay alanı karşılaştırması, sessiz saat, RSS zinciri ve geçersiz tarih kontrolleri geçti.
- Gerçek resmî HTML / RSS örneklerinde native Java ve JavaScript ayrıştırıcılarının makale sayıları ve olay kimlikleri karşılaştırıldı. Bu test PhoneCore içindir; WorkManager veya Android SQLite testinin yerine geçmez.
- `npm test`: 8 yerel test geçti — tekrar, eski haber, video kimliği, takip kapsamı, sessiz saat, yedek round-trip, kötü yedek reddi ve kaynak hatası yalıtımı.
- Tarayıcıda 1365×950 ve 390×844 görünüm kontrol edildi. Favoriye alma ve okundu bilgisi, sayfa yenilendikten sonra IndexedDB'den korundu; yatay taşma saptanmadı.
- Siyah-mor tema varsayılan; kaynak alma için hesap açma / giriş ekranı yoktur. Kullanıcı logosu eklendi.
- TypeScript / Vite üretim derlemesi ve Capacitor Android senkronu geçti. Firebase Push eklentisi ve bulut bağımlılıkları aktif projeden kaldırıldı.
- Varsayılan kontrol hedefi 4 saate değiştirildi; önceki yerel deneme ayarları için program sürümüyle bir defalık geçiş var.

## Kontrol edilecekler

- **APK derlemesi başarılı:** JDK 21, Android SDK 36 ve Gradle 8.14.3 ile `assembleDebug`. Son çıktı `artifacts/YoriNovi-debug.apk` (6.24 MB), v0.2.0 / versionCode 2. Android uygulamasının tüm Java kaynakları gerçek Android bağımlılıklarıyla derlendi.
- APK meta verisi kontrol edildi: görünen ad `YoriNovi`, hedef SDK 36, paket `com.fallowthis.app`. Paket içindeki katalog **8 kaynak / 10 konu / 240 dakika** içeriyor; kullanıcı logosu pakette mevcut.
- `apksigner verify --verbose` başarılı; APK v2 imzası doğrulandı. Bu bir debug/test imzasıdır.
- APK SHA-256: `4d9c9f0ce3090b427d6d23c3a22045ea1339f9d4be95b584aa68d5755167274b`.
- Gerçek Android cihazında kurulum, arka plan görevinin 4 saatlik hedefte yürütülmesi, yeniden başlatma, pil tasarrufu ve bildirim teslimatı henüz doğrulanmadı.
- Windows'ta PWA yüklemesi ve gerçek işletim sistemi bildirimi doğrulanmadı. Tarayıcı kapalıyken tarama yapılmaz.
- Android / Windows otomatik eşitlemesi bu sürümde yoktur; dosya aktarımı tasarlandı. Yedek biçimi otomatik testte sınandı; Android sistem paylaşım ekranı gerçek cihaz bekliyor.
- SQLite ve WorkManager kodu Android uygulamasında bulunur; JVM çekirdek testleri bu Android servislerinin çalıştığını kanıtlamaz.
- Kaynakların tamamı her telefon / ağda erişilebilir olmayabilir. Kaynak tasarım değişiklikleri ve ifade kalıpları duyuruların kaçmasına yol açabilir. Kaynaklar arası kusursuz anlamsal tekrar ayıklama garantisi yoktur.

## Yerel kanıt dosyaları

- `artifacts/local-live-check.json`: canlı yerel toplama / tekrar kontrolü.
- `artifacts/native-test/live.json`: resmî HTML/RSS karşılaştırma örnekleri; Git'e eklenmez.
- `artifacts/native-test/golden.json`: test senaryoları; üretim akışına eklenmez.
- `artifacts/purple-desktop.jpg`, `artifacts/purple-mobile.jpg`: yeni arayüz kontrolleri. Son isim / logo sonrası güncel ekran görüntüleri ayrıca kaydedilebilir.
