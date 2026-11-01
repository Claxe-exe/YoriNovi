# YoriNovi mimarisi

## Platformlar

Android ana çalışma platformudur. React / Capacitor arayüzü, `FallowMonitor` adlı uygulama içi yerel köprü üzerinden SQLite'a ve Android özelliklerine erişir. Bu iç isim ve eski paket / yedek kimlikleri veri uyumluluğu için korunmuştur; görünen ürün adı YoriNovi'dir.

Windows tarafı aynı responsive arayüzü ve IndexedDB depolamasını kullanır. Uzak API veya hesap yoktur. Android ve Windows arasında yalnızca kullanıcının başlattığı JSON dosya aktarımı vardır.

## Ayrımlar

| Bileşen | Sorumluluk |
| --- | --- |
| `shared/catalog.js` | Kategori, konu ve resmî kaynakları ayrı kimliklerle tanımlar. Kaynaklar favori tercihinden bağımsızdır. |
| `shared/anime.js` | Animeye özel sınıflandırma, Türkçe şablonlar, sezon / proje / video kimliği. |
| `shared/parsers.js` | Kaynak HTML/RSS → ortak makale biçimi. Yeni HTML kaynağı çoğunlukla yalnızca seçici yapılandırması ister. |
| `shared/collect.js` | Kaynak döngüsü, koşullu HTTP, ilk aktarım, erişim hatası yalıtımı. |
| `shared/state.js` | Yerel tercihler, takip eşleştirmesi, sessiz saat ve doğrulanan yedek biçimi. |
| `shared/local.js` | Makalelerden tekil olay oluşturma ve yerel bildirim adayları. |
| `src/local.ts` | Android köprüsü veya tarayıcı IndexedDB / ağ adaptörü seçimi. |
| `PhoneCore.java` | Android çalışanının kullanacağı platformdan bağımsız ayrıştırma ve anime kuralları. JS eşdeğerliği test edilir. |
| `PhoneStore.java` | Android SQLite transaction'ları; makale URL'si, olay kimliği ve bildirim adayı benzersizliği. |
| `PhoneEngine.java` | Android HTTP erişimi, veri alımı ve izin / sessiz saat kontrolüyle yerel bildirim. |
| `MonitorWorker.java` | WorkManager ile benzersiz dönemsel iş; hedef 4 saat, ağ ve düşük pil koşulları. |
| `FallowMonitorPlugin.java` | Arayüzden yerel işlemler, bildirim izni ve yedek paylaşımı; ağ işleri ayrı executor'da. |

Katalog `scripts/write-catalog.js` ile Android asset'ine derleme sırasında yazılır. Kaynaklar için URL izin listesi sabittir; istemciden keyfî kaynak adresi kabul edilmez. HTML sayfaları çalıştırılmaz, yalnızca metin / link olarak ayrıştırılır.

## Veri akışı

1. Kullanıcı ilk kez açtığında veya Şimdi kontrol et dediğinde yerel kontrol başlar. Periyodik WorkManager işi de aynı `PhoneEngine` yolunu kullanır; uygulamanın WebView'i veya Activity'si arka planda açılmaz.
2. Kaynaklar doğrudan HTTPS ile okunur. Koşullu HTTP ETag, bağlantı / okuma süresi ve gövde boyutu sınırları uygulanır. Yeni kaynakların ilk aktarımında yalnızca yapılandırılmış arşiv sayfaları alınır.
3. Anime modülü uygun olayları sınıflandırır. SQLite makale, olay ve bildirim adayı yazımlarını aynı transaction'da yapar. Bir makale birden fazla olay türü üretebilir.
4. Aynı sezon onayı veya video başka kaynaktan gelirse olay çoğaltılmaz; kaynak kanıtı eklenir. İlk aktarım sessizdir. Yalnızca son 48 saatteki, yeni keşfedilen uygun olaylar bildirim adayı olur.
5. Bildirim izni, güncel takip kapsamı, haber türü ve sessiz saat cihazda tekrar kontrol edilir. Gönderilen olay `sent` olarak işaretlenir; Android bildirimi olay kimliğiyle sabit etiketlenir. İşlem gönderim sonrası çöküyorsa teorik yeniden teslim mümkündür; mutlak exactly-once iddiası yoktur.
6. Arayüz aynı SQLite kayıtlarını açar. Kayıtlı haberlerin okunması için internet gerekmez. Android bundle'ı uygulama içindedir; Windows'ta açık tarayıcı / yerel önizleme kullanılır.

## Arka plan sözleşmesi

Varsayılan 240 dakika hedeflenir. Bir sonraki kesin saat hesaplanıp vaat edilmez; Android işi geciktirebilir. İş benzersiz isimle `ExistingPeriodicWorkPolicy.UPDATE` kullanır; ayar değişiminde kopya zamanlayıcı açılmaz. Arka plan kapatılınca dönemsel iş iptal edilir. Normal iş için ağ bağlantısı, düşük olmayan pil; seçilirse ölçülmeyen ağ gerekir. Elle kontrol, kullanıcının açık isteğidir ve periyodik ağ / pil kısıtlarını beklemez.

İşleyici ağırlıklı olarak kaynakları sırayla tarar, bir kaynağın hata vermesi diğerlerini durdurmaz. Elle ve dönemsel kontrolün aynı anda havuzu değiştirmesi process içi kilitle engellenir. Kaynak başına son kontrol, son başarı ve hata saklanır.

## Genişletme

Gelecek kategoriler tahminen geliştirilmedi. Yeni kategori için katalog kaydı ve ilgili kategorinin Makale → Olay kuralları eklenir; genel yerel depolama, bildirim izni, sessiz saat ve yedek yolu korunur. Android ve tarayıcı kural eşdeğerliği aynı test sözleşmesine eklenir. Animeye özel sezon ve tarih çıkarımı başka modüllere yayılmaz.

Ücretli AI sağlayıcısı yoktur. İleride isteğe bağlı özet sağlayıcısı eklenebilir; kaynak kanıtı, resmî izin listesi ve tekrar kimliği bu sağlayıcıya bırakılmaz.

## GitHub ve gizli bilgiler

Kaynak kod, katalog, kullanıcı tarafından sağlanan logo ve testler yüklenir. Yerel haber / okundu verileri, yedekler, SDK / JDK / Gradle önbelleği, anahtarlar ve APK derleme çıktıları Git dışında kalır. APK ayrıca sürüm eki olarak paylaşılabilir. Bu sürümde Firebase hizmet hesabı veya başka API anahtarı gerekmemektedir.
