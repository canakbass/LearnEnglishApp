package com.app.wordlearn.domain.model

/**
 * Domain user model.
 *
 * [userId] Firebase UID (stable, unique) — daha önce [Int] hashCode()'du, ancak
 * 32-bit hashCode farklı UID'lerde collision üretebileceği ve JVM sürümleri arasında
 * stabil olmadığı için [String] tipine alındı.
 */
data class User(
    val userId: String,
    val username: String,
    val email: String? = null,
    val level: String = "Başlangıç",
    val score: Int = 0,
    val joinDate: Long = System.currentTimeMillis()
)
