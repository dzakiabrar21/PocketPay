package com.example.pocketpay.di

import com.example.pocketpay.data.repository.BankingRepositoryImpl
import com.example.pocketpay.domain.repository.BankingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBankingRepository(
        impl: BankingRepositoryImpl
    ): BankingRepository
}