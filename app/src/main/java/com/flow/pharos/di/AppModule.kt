package com.flow.pharos.di

import android.content.Context
import com.flow.pharos.BuildConfig
import com.flow.pharos.core.storage.BudgetRepository
import com.flow.pharos.core.storage.SeedRepository
import com.flow.pharos.provider.customopenai.CustomOpenAiProvider
import com.flow.pharos.provider.ollama.OllamaProvider
import com.flow.pharos.provider.perplexity.PerplexityProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSeedRepository(): SeedRepository = SeedRepository()

    @Provides
    @Singleton
    fun provideBudgetRepository(
        @ApplicationContext context: Context,
    ): BudgetRepository = BudgetRepository(context)

    @Provides
    @Singleton
    fun providePerplexityProvider(): PerplexityProvider =
        PerplexityProvider(BuildConfig.PERPLEXITY_API_KEY)

    @Provides
    @Singleton
    fun provideOllamaProvider(): OllamaProvider =
        OllamaProvider(BuildConfig.OLLAMA_BASE_URL)

    @Provides
    @Singleton
    fun provideCustomOpenAiProvider(): CustomOpenAiProvider =
        CustomOpenAiProvider(BuildConfig.CUSTOM_OPENAI_BASE_URL, BuildConfig.CUSTOM_OPENAI_API_KEY)
}
