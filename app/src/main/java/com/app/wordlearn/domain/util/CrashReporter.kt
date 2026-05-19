package com.app.wordlearn.domain.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Hata raporlama için tek nokta.
 *
 * - Tüm catch'lerde sadece `Log.e()` çağırmak yerine [reportException] kullanılır.
 * - DEBUG build'de console'a yazar; RELEASE build'de Crashlytics'e non-fatal
 *   olarak gönderir.
 */
object CrashReporter {

    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    fun reportException(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        runCatching {
            crashlytics.log("[$tag] $message")
            crashlytics.recordException(throwable)
        }
    }

    fun log(tag: String, message: String) {
        Log.d(tag, message)
        runCatching { crashlytics.log("[$tag] $message") }
    }

    /** Logged-in kullanıcı bilgisini Crashlytics'e ekler — privacy: sadece UID. */
    fun setUserId(uid: String?) {
        runCatching {
            if (uid.isNullOrBlank()) {
                crashlytics.setUserId("")
            } else {
                crashlytics.setUserId(uid)
            }
        }
    }
}
