# YoriNovi

**Telefon merkezli, hesap ve sunucu gerektirmeyen gelişme takibi.** İlk modül anime duyurularıdır. Siyah-mor arayüzde genel takip, favoriler, haber türleri, sessiz saatler ve dosyayla yedek aktarımı bulunur. Uygulama logosu kullanıcı tarafından sağlandı.

## Çalışma şekli

- **Android:** Resmî siteler doğrudan telefondan okunur. Haberler, favoriler, tercihler ve okundu bilgisi uygulamanın SQLite veritabanında saklanır. Bildirim telefonda oluşturulur. Firebase, ücretli AI API'si, VPS, alan adı veya uygulama hesabı gerekmez.
- **Arka plan:** Varsayılan hedef **4 saatte bir** kontroldür. WorkManager, uygulamayı ekrana getirmeden işi yürütür. Telefonun ekranı açılmaz. Pil tasarrufu, Doze, düşük pil ve ağ kısıtları görevi geciktirebilir; tam saatinde çalışma garantisi yoktur. Zorla durdurulan uygulama yeniden açılana kadar çalışmayabilir. Kullanıcı aralığı 15/30/60/180/240 dakika olarak değiştirebilir.
- **Windows / tarayıcı:** Haberler ve tercihler IndexedDB'de cihazda saklanır. Telefon yedeği dosya olarak içe alınabilir. Otomatik cihazlar arası eşitleme yoktur. Tarayıcı kapalıyken haber taraması yapılmaz. İsteğe bağlı yerel Node önizlemesi kaynakların CORS kısıtlarını aşmak için sabit resmî izin listesinden veri taşır; Android bu sürece bağlanmaz ve PC'nin açık kalmasına ihtiyaç duymaz.

Resmî Android açıklaması: [kalıcı arka plan işleri](https://developer.android.com/develop/background-work/background-tasks/persistent-work), [dönemsel iş](https://developer.android.com/develop/background-work/background-tasks/persistent-work/getting-started/define-work).

## Yerelde aç

Node.js 22.13 veya üzeri gerekir.

```powershell
npm ci
npm run build
npm start
```

`http://localhost:3001` adresini aç. Hesap oluşturma adımı yoktur. İlk açılışta gerçek resmî kaynaklar taranır; tamamlanması bağlantıya göre biraz sürebilir. `npm run dev`, arayüzü `http://localhost:5173` adresinde canlı geliştirmeye açar. Önizleme yalnızca `127.0.0.1` adresine bağlanır.

`npm start` kişisel Windows önizlemesi içindir; telefon için sürekli açık tutulmaz. Önizleme süreci kullanıcı verisini veya takip tercihlerini saklamaz; bunlar tarayıcıda kalır. Normal bir statik barındırmada doğrudan kaynak erişimi CORS nedeniyle reddedilebilir; hata kaynak ekranında gösterilir. Böyle bir durumda Windows okuyucusunda Android'den dışa aktarılan yedeği kullan.

## Android APK

Standart yol: Android Studio, JDK 21 ve Android SDK 36 ile:

```powershell
npm run android:sync
npm run android:open
```

Android Studio'da cihaz seçip Run veya Build APK kullan. **`google-services.json` veya API anahtarı ekleme gereği yoktur.**

Bu Windows projesinde taşınabilir derleyici yolu da hazır:

```powershell
npm run android:apk
```

Bu komut JDK 21 ve resmî Android komut satırı araçlarını `.android-build/` altına indirir, SHA-256 özetlerini doğrular, SDK 36'yı kurar, arayüzü paketler ve `artifacts/YoriNovi-debug.apk` üretir. İlk SDK kurulumunda Google SDK lisansı terminalde onay ister. Bu oturumda kullanıcı SDK lisansının kabulüne açıkça izin verdi. Araçlar, Gradle önbelleği, SDK lisans kayıtları ve debug imzalama dosyaları GitHub'a eklenmez. İlk indirme ve derleme yaklaşık birkaç GB boş alan gerektirebilir.

APK bir **debug / test** paketidir; Play Store sürümü veya dağıtım için kişisel üretim anahtarıyla imzalanmış release değildir. İç paket kimliği `com.fallowthis.app`, güncellemede yerel kayıtların korunması için sabit tutuldu; görünen ad **YoriNovi**'dir.

Telefon testi:

1. APK'yi kurup uygulamayı bir kez aç; ilk kaynak kontrolünü bekle.
2. Ayarlar'da kontrol aralığının **4 saat**, arka plan takibinin açık olduğunu kontrol et.
3. Bildirim izni verip **Test bildirimi gönder** düğmesini kullan. Servis isteğinin kabulü cihazda görünmenin kanıtı değildir; bildirimi gerçekten görmelisin.
4. Uygulamayı normal biçimde arka plana al, sonraki zaman aralığında Kaynaklar ekranındaki son kontrol zamanını incele. Bildirime dokununca ilgili haber açılmalıdır.
5. Kotasız ağ ve sessiz saat seçeneklerini kendi telefonunda sınayarak pil politikalarını değerlendir.

**Gerçek Android cihazında arka plan zamanlaması ve bildirim teslimatı bu geliştirme oturumunda doğrulanmadı.** Derleme ve çekirdek testlerini gerçek cihaz kanıtı olarak değerlendirme.

## Kaynaklar ve kapsam

Kaynakları uygulama geliştirirken resmî sitelerinden bulup doğrudan erişimlerini doğruladık. Etkin kaynaklar:

| Kaynak | Tür / ilk içe aktarım |
| --- | --- |
| [Lycoris Recoil](https://lycoris-recoil.com/news/) | Resmî HTML, ilk 3 sayfa |
| [Bocchi the Rock!](https://bocchi.rocks/omnibus/news/) | Resmî HTML, ilk 2 sayfa |
| [Re:ZERO](https://re-zero-anime.jp/tv/news/) | Resmî HTML, sayfa içi duyurular |
| [Demon Slayer](https://kimetsu.com/anime/news/) | Resmî HTML, ilk 2 sayfa |
| [Toei Animation Europe](https://www.toei-animation.com/feed/) | Resmî yayıncı RSS |
| [Oshi no Ko](https://ichigoproduction.com/Season3/news/) | Resmî HTML, ilk liste sayfası |
| [Chainsaw Man](https://chainsawman.dog/news/) | Resmî HTML, listede sunulan arşiv |
| [86 Eighty-Six](https://anime-86.com/news/) | Resmî HTML, ilk 3 sayfa |

Katalogda bu yapımlara ek olarak Toei akışında eşleştirilen One Piece, Dragon Ball ve geniş keşifte Le Collège Noir yer alır. **Toplam 10 konu**, bütün anime evreni değildir. Genel takip bu açık katalog üzerinden çalışır; tek tek favorilemek gerekmez. Favoriler, genel kategori takibi kapalıyken de dahil edilir.

SPY×FAMILY, Frieren ve Jujutsu Kaisen resmî siteleri doğrudan denemede HTTP 403 döndürdü; etkin değiller. X'in [resmî API erişimi](https://docs.x.com/x-api/getting-started/pricing) hesap, anahtar ve kullanıma göre ücret gerektirdiğinden X bağlı değildir. Kaynak ekranı erişim hatalarını gizlemez. CAPTCHA veya oturum engeli aşılmaz.

## Haber işleme

- Yeni proje / devam onayı, yayın tarihi ve resmî fragman ayrı türlerdir; bildirim tercihleri ayrı seçilir.
- Yalnızca resmî izin listesindeki kaynaklar taranır. Ürün, oyun, konser, bölüm ön izlemesi, tekrar yayın ve tanınan söylenti kalıpları ayıklanır.
- Aynı URL tekrar işlenmez. Aynı sezon onayı, proje + yayın tarihi ve aynı video kimliği üzerinden olaylar birleştirilir; tüm kaynak bağlantıları korunur. Belirsiz olaylarda normalize başlık kullanılır. Farklı dilde, kesin işaretsiz duyurularda kusursuz anlamsal birleştirme garantisi yoktur.
- İlk içe aktarım, içe aktarılan yedekler ve 48 saatten eski duyurular yeni bildirim oluşturmaz. Sessiz saatte bekleyen haber sonraki uygun kontrolde ele alınır; 48 saati aşarsa iptal edilir.
- Türkçe kısa özetler kurallarla üretilir; tam çeviri değildir. Hikâye ayrıntıları özete alınmaz. Kaynak başlığı istenirse ayrıntıda açılır.
- Duyuru tarihi ve açıklanan yayın tarihi ayrı tutulur. Yalnızca yıl biliniyorsa gün/ay uydurulmaz.
- Aynı URL'deki sonradan yapılan metin değişikliği bu sürümde yeniden işlenmez. Belirsiz ifade kalıpları, liste dışında kalan eski duyurular ve katalog dışı yapımlar kaçabilir.

## Yerel yedek

Ayarlar → **Yedek dışa aktar**. Android sistemin dosya kaydetme / paylaşma ekranını açar; Windows JSON dosyası indirir. Yedek tercihleri, haberleri ve okundu bilgisini içerir. **Yedek içe al** mevcut cihaz kaydını değiştirir; uygulama bu işlemi ayrıca onaylatır. Bulut hesabı veya otomatik eşitleme yoktur. Eski `fallow-local` dosya biçimi uyumluluk için korunmuştur.

Tarayıcı verileri silinirse kayıtlar kaybolur; Android uygulamasını kaldırmak da uygulama içi verileri siler. Öncesinde yedek al. `data/fallow.sqlite` önceki sunucu denemesinin gerçek kayıtlarını içerir; yeni telefon uygulaması bu dosyaya ihtiyaç duymaz ve dosya GitHub'a yüklenmez.

## Doğrulama

```powershell
npm test
npm run test:native
npm run build
npm run ingest
```

- `npm test`: yerel tekrar ayıklama, eski duyurular, kapsam, sessiz saat, yedek doğrulaması ve kaynak hatası yalıtımı.
- `test:native`: Android'in kullandığı **aynı PhoneCore.java** sınıfını JVM'de derler; 12 sentetik eşdeğerlik senaryosunda 42 alan kontrolünü yapar. Varsa gerçek kaynak HTML/RSS örneklerini de JavaScript ile karşılaştırır. WorkManager, Android SQLite veya gerçek cihaz bildirimi testi değildir.
- `ingest`: canlı kaynakları kullanarak `data/local-check.json` ve `artifacts/local-live-check.json` üretir; arayüze sahte seed eklemez. İkinci çalıştırmada tekrarların eklenmediği incelenebilir. Bu dosya Ayarlar'dan yedek olarak içe de alınabilir.

Son durum: [doğrulama kaydı](docs/VERIFICATION.md). Mimari: [ARCHITECTURE.md](docs/ARCHITECTURE.md).

Önceki sunucu yaklaşımı yerelde `legacy/` altında arşivlendi; çalıştırılmaz ve yeni GitHub deposuna dahil edilmez. Aktif uygulamada ücretli servis veya gizli API anahtarı bulunmaz.
