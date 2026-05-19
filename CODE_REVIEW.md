# WordLearn - Kod Analizi ve Düzeltme Planı

> Tarih: 19 Mayıs 2026
> Kapsam: `com.app.wordlearn` (Compose + Hilt + Room + Firebase)

---

## 1) SİYAH EKRAN — Kök Sebep ve Uygulanan Düzeltmeler

### Tespit
- Fiziksel cihazda sorun yok, **sadece emülatörde** siyah ekran.
- Logcat'te kritik satır:
  `userfaultfd: MOVE ioctl seems unsupported: Connection timed out`
- Bu, Android emülatörünün ART (Android Runtime) Garbage Collector'ının userfaultfd çekirdek özelliğini kullanmaya çalıştığını, ancak emülatör çekirdeğinin bunu desteklemediğini gösteriyor. Sonuç: Compose render pipeline bloke oluyor → ekran siyah kalıyor.
- Bu bir **emülatör/host uyumluluk sorunu**, doğrudan kodun hatası değil. Fakat kodda bu durumu daha kötüleştiren noktalar vardı (bkz. uygulanan fix'ler).

### Uygulanan Kod Düzeltmeleri
1. `AuthViewModel.initializeApp()` içindeki yapay `delay(1500)` kaldırıldı — yükleme süresini boşuna 1.5s uzatıyordu.
2. DB seed'i artık auth check'i bloklamıyor — iki ayrı coroutine'de paralel çalışıyor.
3. Firebase auth çağrısı `withTimeoutOrNull(3s)` + `runCatching` ile sarmalandı — network yokken veya Firebase yavaşken sonsuza dek loading state'te kalmayı engelliyor.
4. `MainActivity.onCreate` artık önce `setContent` çağırıyor, WorkManager kurulumu sonra ve `try/catch` içinde — Worker hatası ekranı bloklamıyor.
5. Hangi adımda takıldığını görmek için anlamlı log'lar eklendi (`AuthViewModel` ve `MainActivity`).

### Emülatör İçin Yapman Gerekenler (öncelik sırasıyla)
1. **AVD'yi sil ve yeniden oluştur** — Android Studio → Device Manager → mevcut AVD → "Wipe Data" + sonra "Cold Boot Now". Çözmezse yeni AVD oluştur.
2. **Sistem image'ını değiştir** — API 34 yerine API 33 (Google APIs) veya API 35 dene. API 34 ile bazı host'larda userfaultfd sorunu yaygın.
3. **Android Emulator'ı güncelle** — SDK Manager → SDK Tools → "Android Emulator" en son sürüm.
4. **Hardware acceleration'ı kontrol et** — Windows'ta WHPX (Windows Hypervisor Platform) etkin olmalı. `bcdedit /set hypervisorlaunchtype auto`.
5. **AVD ayarları** — RAM'i en az 2048 MB, Heap'i 512 MB yap.

---

## 2) Code Smells — Öncelik Sırasıyla

### 🔴 KRİTİK (data integrity / runtime crash riski)

#### S1. `UserRepositoryImpl` içinde `firebaseUser.uid.hashCode()` ile userId üretiliyor
`uid` 28 karakterlik benzersiz bir string, ama `hashCode()` sadece 32-bit Int döndürüyor → **hash collision** mümkün. Üstelik `hashCode()` JVM sürümleri arasında stabil değil. İki farklı kullanıcı aynı `userId`'yi alabilir, Room'da PK çakışması olur.
- **Çözüm:** `User.userId`'yi `String` yap, doğrudan `firebaseUser.uid` kullan. Tüm domain modelinde ve DAO'da güncelle.

#### S2. `UserRepository.updateUserLevel` ve `updateScore` boş stub
```kotlin
override suspend fun updateUserLevel(userId: Int, level: String) {
    // Level is stored locally in Settings, not in Firebase
}
```
Çağıran kod bir şey yapıldığını sanıyor ama hiçbir şey olmuyor. Sessiz bug.
- **Çözüm:** Ya gerçekten implement et, ya da bu metodları interface'den kaldır ve çağrılarını `SettingsRepository`'ye yönlendir.

#### S3. `AuthViewModel.resetPassword` — `errorMessage` alanını başarı mesajı için de kullanıyor
```kotlin
errorMessage = "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi."
```
UI bunu hata zannedip kırmızı snackbar gösterebilir.
- **Çözüm:** `AuthState`'e `infoMessage` veya `snackbarMessage: SnackbarEvent?` ekle.

#### S4. `InitializeDatabaseUseCase` SQL parse'ı regex ile yapıyor
```kotlin
Regex("'([^']*)'").findAll(trimmed)
```
Bir değer içinde tek tırnak (`'`) olursa parse bozulur. Şu an SQL'inde escape yoksa OK, ama kırılgan.
- **Çözüm:** SQL dosyası yerine **pre-populated Room database** asset'i kullan (`Room.databaseBuilder(...).createFromAsset("...")`). Veya JSON formatına geç (`assets/system_words.json` zaten var).

### 🟠 YÜKSEK (mimari / sürdürülebilirlik)

#### S5. `AppDatabase.clearUserData()` raw SQL ve `openHelper.writableDatabase` kullanıyor
DAO katmanını bypass ediyor; Room'un yazma garantilerini kaybediyor.
- **Çözüm:** Her DAO'ya `@Query("DELETE FROM ...")` `deleteAll()` ekle, `clearUserData()` bunları sırayla çağırsın.

#### S6. `NavGraph` içinde iki ayrı `NavController` (auth + main)
Karmaşık, derin link desteği zor, state restore sıkıntılı.
- **Çözüm:** Tek bir `NavController`, `startDestination`'ı `authState`'e göre belirle. Veya **Compose Navigation 2.8+** ile type-safe destinations'a geç.

#### S7. Route'lar string olarak yazılmış (`"quiz"`, `"wordle"`, `"addword"`, `"saved_stories"`)
Sealed class `Screen` sadece bottom nav için kullanılmış, diğerleri free string. Typo riski + IDE refactor desteği yok.
- **Çözüm:** Tüm rotaları `Screen` sealed sınıfına ekle veya type-safe Navigation API'sini kullan.

#### S8. `MainActivity` içinde inline WorkManager scheduling
Activity'nin işi değil; ayrıca her `onCreate`'te yeniden enqueue ediyor.
- **Çözüm:** `Application.onCreate`'e taşı veya `androidx.startup.Initializer` kullan. `ExistingPeriodicWorkPolicy.KEEP` ile sadece bir kez kurulsun.

#### S9. Reminder saatleri (8:00 ve 20:00) hardcoded
Kullanıcının ayarlardan değiştirememesi UX kaybı.
- **Çözüm:** `SettingsRepository`'ye `reminderTimes: List<LocalTime>` ekle, kullanıcı ayarlayabilsin.

#### S10. Repository implementasyonları `FirebaseAuth.getInstance()` singleton'a doğrudan bağımlı
Unit test imkânsız.
- **Çözüm:** `FirebaseAuth` instance'ını Hilt module'den provide et, constructor injection ile geç.

### 🟡 ORTA (kalite / performans / lokalizasyon)

#### S11. Türkçe stringler kodda hardcoded
Tüm hata mesajları, ekran başlıkları, button label'ları. İleride lokalizasyon imkânsız.
- **Çözüm:** Yavaş yavaş `res/values/strings.xml` (TR) ve `res/values-en/strings.xml` (EN) altına taşı. ViewModel'lerde `Application.getString(R.string.x)` veya `StringResource` enum kullan.

#### S12. `build.gradle.kts` içinde `WEB_CLIENT_ID` hardcoded
```kotlin
buildConfigField("String", "WEB_CLIENT_ID", "\"190133...\"")
```
Public repo'ya pushlanırsa Firebase config sızar.
- **Çözüm:** `GEMINI_API_KEY` zaten `local.properties`'ten okunuyor — `WEB_CLIENT_ID`'yi de oraya taşı.

#### S13. `material-icons-extended` paketi tam dahil
APK'ya ~80MB ekler. R8 etkin olmadığı için temizlenmiyor.
- **Çözüm:** `release` build'inde `isMinifyEnabled = true`, `isShrinkResources = true`. Ya da spesifik icon'ları manuel kopyala/ekle.

#### S14. Release build'de minify kapalı
```kotlin
release {
    isMinifyEnabled = false
}
```
- **Çözüm:** `isMinifyEnabled = true` yap, Hilt/Room/Firebase için proguard kuralları ekle (genelde varsayılan tutar).

#### S15. `fallbackToDestructiveMigration()` aktif
Schema bump'ında kullanıcı verisi silinir.
- **Çözüm:** Migration sınıfı yaz: `.addMigrations(MIGRATION_2_3)`. Versiyon 3'ten 4'e gittiğinde data kaybı olmasın.

#### S16. Compose BOM 2023.10.01 ve Hilt 2.50 eski
- **Çözüm:** BOM 2024.04+, Hilt 2.51+ güncelle. Compose Compiler 1.5.8 ile uyumlu sürümleri seç. (Kotlin 1.9.22 ile uyumlu.)

#### S17. Crashlytics yok
Production'da silent crash'leri göremezsin.
- **Çözüm:** Firebase Crashlytics ekle, Application.onCreate'te `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)`.

### 🟢 DÜŞÜK (cosmetics)

- **S18.** `AuthViewModel` constructor'ında `settingsRepository` field'ı kullanılmaksızın inject ediliyor mu kontrol et — bazı dosyalarda lazy/unused dependency olabilir.
- **S19.** `NavGraph` içinde `TopAppBar` title'ları `when (currentRoute)` ile mapleniyor — bu maple `Screen` sealed sınıfına metadata olarak taşınabilir.
- **S20.** Constants.kt'te bazı sabitler kullanılmıyor olabilir — IDE "unused" uyarılarını temizle.

---

## 3) Önerilen İlerleyiş

### Sprint 1 — Stability (1-2 gün)
- [x] Black screen hızlı fix (uygulandı)
- [ ] S1: userId tipini String'e çevir
- [ ] S2: Stub metodları temizle
- [ ] S3: AuthState'e infoMessage ekle

### Sprint 2 — Architecture (3-5 gün)
- [ ] S5: clearUserData DAO'ya taşı
- [ ] S6-S7: Navigation refactor (tek NavController + sealed routes)
- [ ] S8-S9: WorkManager init + configurable times
- [ ] S10: FirebaseAuth Hilt module

### Sprint 3 — Quality (2-3 gün)
- [ ] S11: strings.xml migration (en kritik ekranlardan başla)
- [ ] S12-S14: build.gradle config (minify, secret keys)
- [ ] S15: Room migrations
- [ ] S16: Dependency güncellemeleri
- [ ] S17: Crashlytics entegrasyonu

### Sprint 4 — Polish
- [ ] S4: SQL seed yerine pre-populated DB
- [ ] S18-S20: küçük temizlikler
- [ ] Unit test coverage artır

---

## 4) Hızlı Sanity Check Komutları

```bash
# Lint
./gradlew lintDebug

# Unit testler
./gradlew testDebugUnitTest

# Bağımlılık ağacı (problem varsa)
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

Sorun yaşadığın spesifik bir code smell varsa benimle paylaş, birlikte düzeltelim.
