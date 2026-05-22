# WordLearn

> Kendi tempon, kendi kelimelerin, kendi cihazın. WordLearn — İngilizce kelime
> öğrenmeyi tamamen yerel, çevrim dışı çalışan ve sana ait kalan bir uygulamaya
> dönüştürür.

WordLearn; reklamsız, satın alımsız, abonelik istemeyen, kullanıcıyı bir
veritabanı satırı olarak görmeyen bir kelime öğrenme aracıdır. Her şey **senin
cihazında** kalır — kelimelerin, ilerlemen, hikayelerin, istatistiklerin. Hesap
sistemi sadece cihazlar arası taşıma kolaylığı için var; yokken de
uygulama tam çalışır.

---

## Felsefe

- **Local-first.** Tüm veriler `Room` veritabanında, kullanıcı cihazında durur.
  Senkronizasyon zorunluluğu yok, sunucuya bağımlılık yok.
- **Çevrim dışı çalışır.** İnternet bağlantısı **sadece** Word Chain hikaye
  üretici (Gemini AI) ve Firebase ile **isteğe bağlı** giriş için gerekir. Diğer
  her şey — günlük quiz, Wordle, kelime listesi, ilerleme takibi, analiz
  ekranı — internet kapalıyken bile sorunsuz çalışır.
- **Öz denetim.** Hangi seviyede başlayacağına, günde kaç yeni kelime
  öğreneceğine, neyi ne zaman tekrar edeceğine sen karar verirsin.
  Algoritma seni yönlendirir; ama nihai söz hep sende.
- **Verilerin senindir.** Tek bir butonla tüm verini ZIP olarak dışa aktarabilir,
  cihaz değiştirdiğinde geri yükleyebilirsin. Bulut yok, bağımlılık yok, vendor
  lock-in yok.
- **Para kazanma odaklı değil.** WordLearn ticari bir ürün gibi değil, açık ve
  kullanıcıya saygılı bir yerel ortam gibi tasarlandı. Reklam yok, "premium"
  ekran yok, "verilerini yedeklemek için abone ol" yok.

---

## Özellikler

### Öğrenme

- **Günlük Quiz** — Aralıklı tekrar (spaced repetition) algoritmasıyla, dün
  yanlış yaptığın ve bugün tekrar görmen gereken kelimeleri öne çıkarır.
  Gün içinde çıkıp tekrar girersen kaldığın yerden devam eder; aynı soru iki
  kez sorulmaz. Günlük kelime sayısı değişikliği sadece ertesi günden itibaren
  geçerli olur.
- **Spaced Repetition Algoritması** — 6 doğru üst üste bir stage atlatır; her
  stage farklı aralıkta tekrar getirir (1 gün → 7 → 30 → 90 → 180 → öğrenildi).
  Yanlış cevap stage'i sıfırlar, ertesi gün tekrar sorar.
- **Tekrar Oturumu** — Günlük kotanı doldurduktan sonra tekrar girersen
  bugünkü kelimeler karışık sırayla pratik için tekrar gelir. Bu oturumda
  istatistiklerin değişmez — algoritmaya kısa süreli ezber sızmaz.
- **Quiz'de örnek cümle** — Her kelime için kayıtlı örnek cümleler quiz
  ekranında küçük italic fontla altında görünür.
- **Wordle** — 5 harfli kelime tahmin oyunu. Eğlenceli pratik.
- **Word Chain (Hikaye Üretici)** — Birkaç kelimeden Gemini AI'la kısa hikayeler
  üretir; istersen kaydedip sonradan tekrar okuyabilirsin. _(İsteğe bağlı,
  internet gerekir.)_
- **Kelime Listesi** — Sistemde 1000+ İngilizce-Türkçe kelime; üzerine kendi
  kelimelerini, örnek cümlelerini ve isteğe bağlı **resimlerini** ekleyebilirsin.
  Resimler hem kelime listesinde hem quiz sırasında gösterilir.
  - **İki sekme:** "Öğrenilmemiş" (hiç başlanmamış) ve "Öğreniyorum"
    (başladığın veya tamamen öğrendiğin) kelimeler ayrı görünür. Sağ sekmede
    her kelimenin aşama rozeti (🌱 Başladım, 🌿 Aşama 2, … ✓ Öğrenildi).
  - **Kendi kelimelerini silebilirsin** — kart üzerinde kırmızı silme ikonu
    yalnızca kullanıcı kelimelerinde görünür; sistem kelimeleri korunur.
- **Sesli Okuma (TTS)** — Cihazın yerel ses motoruyla telaffuz; harici servis
  yok.

### İstatistik

- **Analiz Ekranı** — Kaç kelime öğrendin, günlük serin (streak), doğruluk
  oranın, hangi seviyede ilerliyorsun — hepsi yerel olarak hesaplanır.

### Bildirim

- **Günlük Hatırlatıcı** — Sabah ve akşam 2 farklı zamanda hatırlatma. Tüm
  zamanlama cihazda; sunucuya push bildirim ihtiyacı yok.

### Hesap & Giriş

- **Misafir Modu** — Hesap açmadan hemen kullanmaya başla. Veriler yerel
  veritabanında tutulur; sonradan hesaba geçmek istersen `Dışa Aktar / İçe Aktar`
  ile ilerlemeni taşıyabilirsin.
- **Hesap Yönetimi** — Ayarlar ekranından kullanıcı adını değiştirebilir veya
  hesabını kalıcı olarak silebilirsin.

### Veri Taşınabilirliği

- **Dışa Aktar / İçe Aktar** — Ayarlar ekranındaki tek bir butonla tüm verini
  `.zip` arşivi olarak istediğin yere kaydet. Aynı arşivi başka bir cihazda
  içe aktararak kaldığın yerden devam et.
- **Arşivin içinde ne var:**
  - `data.json` — kendi eklediğin kelimeler ve örnek cümleleri, tüm
    kelimelerin ilerleme kayıtları, quiz oturumları + cevapları, ayarların,
    Word Chain hikayelerin.
  - `images/` — kullanıcı kelimelerine eklediğin resimler ve hikaye görselleri.
- **Eşleştirme:** İlerleme kayıtları kelime ID'siyle değil, _İngilizce kelime
  metniyle_ eşleştirilir. Bu sayede farklı cihazlarda sistemdeki kelimelerin
  ID'leri farklı olsa bile yedek doğru kelimeye bağlanır.
- **Format açık:** ZIP içindeki `data.json` standart bir JSON dosyası — istersen
  metin editöründe açar, gözden geçirir, hatta elle düzenleyebilirsin.

---

## Mimari

Clean Architecture + tek modüllü Android uygulama:

```
com.app.wordlearn
├── data
│   ├── backup     → BackupRepository (export/import)
│   ├── local      → Room DB, DAOs, entity'ler, mappers, migrations
│   ├── remote     → API arayüzleri (Firebase + Gemini)
│   └── repository → Repository implementasyonları
├── domain
│   ├── model      → Domain veri sınıfları (Word, QuizSession, BackupData…)
│   ├── repository → Repository interface'leri
│   ├── usecase    → Tek sorumluluklu iş akışları
│   └── util       → CrashReporter, Constants, TtsManager
├── presentation
│   ├── auth       → Login / Register / Email Verification ekranları
│   ├── home       → Ana sayfa
│   ├── quiz       → Günlük quiz
│   ├── wordle     → Wordle oyunu
│   ├── wordchain  → Word Chain hikaye üretici
│   ├── words      → Kelime listesi + ekleme
│   ├── analytics  → İstatistik ekranı
│   ├── settings   → Ayarlar (yedek alma dahil)
│   ├── navigation → Sealed Screen + tek NavController
│   ├── theme      → Material 3 renkleri + tema
│   ├── components → LoadingScreen vb. ortak öğeler
│   └── worker     → WorkManager hatırlatıcıları
├── di             → Hilt modülleri (Database, Firebase, Repository, Network)
├── MainActivity, WordLearnApplication
```

### Teknoloji Seçimleri

| Katman | Teknoloji |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation, tek NavController + iki nested graph (auth / main) |
| DI | Hilt 2.51 |
| Yerel veritabanı | Room 2.6.1 + KSP, sürüm migration'lı |
| Reaktif veri | Kotlin Coroutines + StateFlow |
| Serileştirme | kotlinx-serialization-json |
| Görsel yükleme | Coil |
| Auth (opsiyonel) | Firebase Auth + Google Sign-In |
| Hata raporlama | Firebase Crashlytics (sadece release build'de açık) |
| Hikaye üretimi (opsiyonel) | Google Generative AI (Gemini) |
| Bildirim zamanlaması | WorkManager |

---

## Kurulum

### Gereksinimler

- Android Studio Hedgehog (2023.1) veya üstü
- JDK 17
- Android SDK 34
- Min SDK 26 (Android 8.0)

### Adımlar

1. Repoyu klonla:
   ```bash
   git clone <repo-url>
   cd EnApp
   ```
2. `local.properties` dosyasını oluştur ve içine kendi anahtarlarını ekle:
   ```properties
   sdk.dir=C:\\Users\\<sen>\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=<gemini_api_key>
   WEB_CLIENT_ID=<google_sign_in_web_client_id>
   ```
   - **Gemini key (opsiyonel):** Word Chain hikaye üretici için gerekli. Yoksa
     uygulamanın geri kalanı çalışır; sadece Word Chain bölümü işlevsiz olur.
     <https://aistudio.google.com/app/apikey>
   - **WEB_CLIENT_ID (opsiyonel):** Google ile giriş için. Yoksa kullanıcılar
     e-posta/şifre veya hiç giriş yapmadan kullanabilir.
3. `google-services.json`'u `app/` klasörüne koy. _(Firebase'siz kullanım için
   bu adımı atlamak istiyorsan, Firebase plugin'lerini `app/build.gradle.kts`
   içinden kaldırabilirsin.)_
4. Sync gradle ve çalıştır:
   ```bash
   ./gradlew assembleDebug
   ```

### Build türleri

- **Debug** — Minify kapalı, Crashlytics toplama kapalı.
- **Release** — `isMinifyEnabled = true`, `isShrinkResources = true`,
  ProGuard kuralları `app/proguard-rules.pro` içinde. Crashlytics non-fatal'ları
  rapor eder.

---

## Veri Modeli

Şu anda Room v5 şemasında 7 tablo var:

| Tablo | İçerik |
| --- | --- |
| `words` | Hem sistem hem kullanıcı kelimeleri (`source` alanıyla ayrılır) |
| `word_samples` | Bir kelimeye ait örnek cümleler |
| `word_progress` | Spaced repetition için ilerleme verileri |
| `quiz_sessions` | Yapılan quiz oturumları |
| `quiz_answers` | Her sorudaki cevap kayıtları |
| `settings` | Yerel kullanıcı ayarları (günlük kelime sayısı, seviye vb.) |
| `stories` | Word Chain ile üretilip kaydedilen hikayeler |

Sistem kelimeleri ilk açılışta `assets/system_words_seed.json` dosyasından
seed edilir (~1000 kelime, A1–C1 seviyelerde). Kullanıcı verisinde değişiklik
yapan tüm yollar suspend ve Room'un `withTransaction` API'siyle korunur.

Migration'lar `data/local/Migrations.kt` içinde. Mevcut migration'lar:

- `MIGRATION_3_4`: `users` tablosu kaldırıldı (kimlik Firebase'de tutuluyor,
  yerel `UserEntity` ölü kod halindeydi).
- `MIGRATION_4_5`: `word_progress` tablosuna `lastShownDate` eklendi —
  quiz devam mantığı (bugün gösterilmiş ama cevaplanmamış kelimeler) için.

---

## Test

```bash
./gradlew testDebugUnitTest
```

Mevcut testler `app/src/test/.../domain/usecase/`:
- `BuildDailyQuizUseCaseTest` — 20 white-box test: quiz havuzu seçimi,
  rank sıralaması, practice mode, restore sonrası progress kullanımı,
  spaced repetition due hesaplaması, distinct garantisi
- `ProcessAnswerUseCaseTest` — 14 test: stage geçişleri (0→1→2→3→4→5),
  streak biriktirme, yanlış cevap stage sıfırlama, duplicate answer
  defansif return, totalCorrect/totalAttempts artışı
- `SyncWordsUseCaseTest`, `WordleUseCaseTest` — eski testler

> **Windows + Türkçe path notu:** Eğer proje Türkçe karakter içeren bir yolda
> (`yazılım yapımı` gibi) duruyorsa Gradle test runner classpath URL'sini
> hatalı çözümler ve `ClassNotFoundException` atar. Bu durumda projeyi
> `C:\dev\` gibi ASCII bir klasöre taşı veya Android Studio'dan tek tek
> test çalıştır. Kod compile temiz, sadece JVM file:// URL bug'ı engel.

---

## Performans Notları

- **Kelime listesi (1000+ kelime):** `LazyColumn` `items(items, key, contentType)`
  ile item identity korunur, scroll'da recomposition skip aktif. Item
  composable'ı (`WordRow`) skippable parametre tipleriyle tasarlandı;
  callback'ler `remember`'la sabitlendi.
- **Arama:** `MutableStateFlow` + `debounce(300L)` — kullanıcı yazarken her
  tuş vuruşunda 1000 kelime taranmaz, son tuştan 300ms sonra tek seferde
  filtre uygulanır.
- **Resim yükleme:** Coil + `remember(path) { File(path) }` ile her
  recomposition'da File allocation yok. Coil default memory cache ile resimli
  user kelimelerinde scroll akıcı kalır.
- **Quiz seçimi:** `BuildDailyQuizUseCase` tek adımda `allProgress` üzerinden
  filter+sort yapar; eski 6 ayrı seçim adımı ve birden fazla DAO çağrısı
  yerine bellekte tek sıralama.
- **Cevap kaydı (`submitAnswer`)**: `withContext(NonCancellable)` ile sarılır
  — kullanıcı ekrandan çıksa bile DB yazımı yarıda kalmaz.

---

## Gizlilik

- Tüm öğrenme verisi yerel `Room` veritabanında.
- Firebase Auth — sadece giriş bilgileri (e-posta, displayName, UID).
  İlerlemen Firebase'e gitmez.
- Crashlytics — sadece release build'de açık ve sadece çökme/non-fatal
  exception kayıtları. Hangi kelimeleri öğrendiğine, ne zaman quiz çözdüğüne
  dair hiçbir veri toplanmaz.
- Gemini AI — yalnızca Word Chain hikaye üretirken senin verdiğin kelimeleri
  prompt olarak gönderir; cevap yerel veritabanına yazılır.
- Yedek dosyası — tamamen senin kontrolünde. Cihaz dışında bir yere
  gönderilmez; ne zaman kimin görsün istersen `Dışa Aktar` butonuyla sen
  paylaşırsın.

---

## Yol Haritası

- [ ] String resource'lara geçiş (TR + EN lokalizasyon altyapısı)
- [ ] Pre-populated Room database (assets/.db) ile soğuk başlangıç süresini
      kısaltma
- [ ] Konfigürasyon edilebilir hatırlatıcı saatleri (Settings ekranı)
- [ ] Type-safe Navigation (Compose Navigation 2.8+)
- [ ] Daha fazla unit/instrumented test
- [ ] Tema (dark/light) seçici (şu an sistem tercihini takip ediyor)

---

## Geliştirme Süreci — Şeffaflık

Bu projenin önemli bir bölümü **Claude (Anthropic) AI asistanı** ile pair
programming usulü geliştirildi. Pratik olarak:

- Mimari kararlar, debugging, refactor, white-box test yazımı, performans
  optimizasyonu, dokümantasyon ve README dahil; tasarım gözden geçirme,
  bug avı (özellikle backup/restore + spaced repetition + LazyColumn jank),
  ve karar dökümantasyonu birlikte yapıldı.
- İnsan geliştirici (Can) ürün vizyonu, akış tasarımı, cihaz üzerinde
  manuel test, UX kararları, Firebase Console konfigurasyonu ve son
  onayları yürüttü. AI önerileri körü körüne uygulanmadı; her birim
  inceleme + onay sonrası commit'lendi.
- Git history'sindeki `Co-Authored-By: Claude` etiketi bu işbirliğinin
  kayıt altındaki halidir.

Bu disclosure modern yazılım geliştirme uygulamalarına uygun şeffaflık
amacıyla eklenmiştir.

---

## Lisans

Tüm hakları saklıdır. Eğer projeyi açık kaynak yapmak istersen buraya `MIT` veya
`Apache-2.0` lisans dosyası ekleyebiliriz.
