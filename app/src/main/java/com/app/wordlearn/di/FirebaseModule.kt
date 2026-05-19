package com.app.wordlearn.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase SDK bileşenlerini DI üzerinden sağlar — böylece Repository'ler
 * statik [FirebaseAuth.getInstance] çağrısına bağımlı kalmaz, test edilebilir olur.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}
