package com.kanso.core.security.di

import com.kanso.core.security.AndroidKeystoreVaultCrypto
import com.kanso.core.security.VaultCrypto
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindVaultCrypto(impl: AndroidKeystoreVaultCrypto): VaultCrypto
}
