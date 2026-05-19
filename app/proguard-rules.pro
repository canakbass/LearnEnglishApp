# =====================================================================
# WordLearn — ProGuard / R8 rules
# =====================================================================
# Çoğu kütüphane (Hilt, Room, Firebase, Coil, Coroutines) kendi
# consumer-rules.pro'sunu getirir; aşağıdaki kurallar bizim koda
# özgü ihtiyaçları + ekstra savunma katmanlarını içerir.

# ---------- Genel ----------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Source dosya adını koru ama dosya adını gizle (stack trace okunabilir kalsın)
-renamesourcefileattribute SourceFile

# ---------- Kotlin ----------
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# Coroutines: kütüphane kendi kurallarını getiriyor ama ek savunma:
-dontwarn kotlinx.coroutines.**

# ---------- Hilt / Dagger ----------
# Hilt 2.50 kendi consumer-rules'ını getiriyor; ek savunma:
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# ---------- Room ----------
# Room kendi consumer-rules'ını getirir; entity ve DAO interface'lerini koru:
-keep class com.app.wordlearn.data.local.entity.** { *; }
-keep interface com.app.wordlearn.data.local.dao.** { *; }
-keep class com.app.wordlearn.data.local.AppDatabase { *; }
-keep class com.app.wordlearn.data.local.AppDatabase_Impl { *; }

# ---------- Domain modelleri ----------
# Reflection ile erişim olmadığı sürece gerekmez, ama immutable data
# class'ları DI ile inject edildiği için constructor'larını koru:
-keep class com.app.wordlearn.domain.model.** { *; }

# ---------- Firebase ----------
# Firebase BoM içindeki kütüphaneler kendi kurallarını getirir.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Sign-In
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.**

# ---------- Gemini AI SDK ----------
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ---------- BuildConfig ----------
-keep class com.app.wordlearn.BuildConfig { *; }

# ---------- Compose ----------
# AGP minify ile Compose'u zaten doğru tutuyor; ek savunma için
# @Composable fonksiyon isimlerini koru (hata ayıklamada yardımcı):
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
