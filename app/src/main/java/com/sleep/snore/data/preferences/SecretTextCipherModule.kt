package com.sleep.snore.data.preferences

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecretTextCipherModule {

    @Binds
    @Singleton
    abstract fun bindSecretTextCipher(cipher: AndroidKeyStoreSecretTextCipher): SecretTextCipher
}
