package com.flow.pharos.di

import android.content.Context
import com.flow.pharos.BuildConfig
import com.flow.pharos.core.llm.AiApiProvider
import com.flow.pharos.core.storage.BudgetRepository
import com.flow.pharos.core.storage.SeedRepository
import com.flow.pharos.core.storage.db.PharosDatabase
import com.flow.pharos.core.storage.repository.AnalysisRepository
import com.flow.pharos.core.storage.repository.ClaimRepository
import com.flow.pharos.core.storage.repository.FileRepository
import com.flow.pharos.core.storage.repository.FolderRepository
import com.flow.pharos.core.storage.repository.ProjectRepository
import com.flow.pharos.core.storage.repository.SettingsRepository
import com.flow.pharos.provider.customopenai.CustomOpenAiProvider
import com.flow.pharos.provider.ollama.OllamaProvider
import com.flow.pharos.provider.perplexity.PerplexityProvider
import com.flow.pharos.core.llm.LlmGateway
import com.flow.pharos.usecase.AnalysisUseCase
import com.flow.pharos.usecase.ClaimExtractionUseCase
import com.flow.pharos.usecase.ConflictDetectionUseCase
import com.flow.pharos.usecase.MasterfileUseCase
import com.flow.pharos.usecase.ProjectClusteringUseCase
import com.flow.pharos.usecase.ScanUseCase
import com.flow.pharos.usecase.SsotPipelineUseCase
import com.flow.pharos.util.PdfTextExtractor
import com.flow.pharos.util.TextExtractor
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

    @Provides
    @Singleton
    fun providePharosDatabase(@ApplicationContext context: Context): PharosDatabase =
        PharosDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideFolderRepository(database: PharosDatabase): FolderRepository =
        FolderRepository(database.folderDao())

    @Provides
    @Singleton
    fun provideFileRepository(database: PharosDatabase): FileRepository =
        FileRepository(database.fileDao())

    @Provides
    @Singleton
    fun provideAnalysisRepository(database: PharosDatabase): AnalysisRepository =
        AnalysisRepository(database.analysisDao())

    @Provides
    @Singleton
    fun provideClaimRepository(database: PharosDatabase): ClaimRepository =
        ClaimRepository(database.claimDao())

    @Provides
    @Singleton
    fun provideProjectRepository(database: PharosDatabase): ProjectRepository =
        ProjectRepository(database.projectDao(), database.projectFileCrossRefDao())

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideTextExtractor(@ApplicationContext context: Context): TextExtractor =
        TextExtractor(context)

    @Provides
    @Singleton
    fun providePdfTextExtractor(): PdfTextExtractor = PdfTextExtractor()

    @Provides
    @Singleton
    fun provideAiApiProvider(perplexityProvider: PerplexityProvider): AiApiProvider =
        perplexityProvider

    @Provides
    @Singleton
    fun provideScanUseCase(
        @ApplicationContext context: Context,
        folderRepo: FolderRepository,
        fileRepo: FileRepository,
        textExtractor: TextExtractor,
        pdfExtractor: PdfTextExtractor
    ): ScanUseCase = ScanUseCase(context, folderRepo, fileRepo, textExtractor, pdfExtractor)

    @Provides
    @Singleton
    fun provideAnalysisUseCase(
        fileRepo: FileRepository,
        analysisRepo: AnalysisRepository,
        settingsRepo: SettingsRepository,
        aiProvider: AiApiProvider,
        textExtractor: TextExtractor,
        pdfExtractor: PdfTextExtractor,
        @ApplicationContext context: Context
    ): AnalysisUseCase = AnalysisUseCase(fileRepo, analysisRepo, settingsRepo, aiProvider, textExtractor, pdfExtractor, context)

    @Provides
    @Singleton
    fun provideProjectClusteringUseCase(
        analysisRepo: AnalysisRepository,
        projectRepo: ProjectRepository,
        fileRepo: FileRepository
    ): ProjectClusteringUseCase = ProjectClusteringUseCase(analysisRepo, projectRepo, fileRepo)

    @Provides
    @Singleton
    fun provideMasterfileUseCase(
        @ApplicationContext context: Context,
        projectRepo: ProjectRepository,
        fileRepo: FileRepository,
        analysisRepo: AnalysisRepository,
        folderRepo: FolderRepository
    ): MasterfileUseCase = MasterfileUseCase(context, projectRepo, fileRepo, analysisRepo, folderRepo)

    @Provides
    @Singleton
    fun provideLlmGateway(
        ollamaProvider: OllamaProvider,
        customOpenAiProvider: CustomOpenAiProvider,
        settingsRepository: SettingsRepository
    ): LlmGateway {
        // Pick the LLM provider based on user configuration; default to Ollama for local inference
        return when (settingsRepository.getLlmProviderType()) {
            SettingsRepository.PROVIDER_CUSTOM_OPENAI -> customOpenAiProvider
            else -> ollamaProvider
        }
    }

    @Provides
    @Singleton
    fun provideClaimExtractionUseCase(
        llmGateway: LlmGateway
    ): ClaimExtractionUseCase = ClaimExtractionUseCase(llmGateway)

    @Provides
    @Singleton
    fun provideConflictDetectionUseCase(
        claimRepo: ClaimRepository,
        llmGateway: LlmGateway
    ): ConflictDetectionUseCase = ConflictDetectionUseCase(claimRepo, llmGateway)

    @Provides
    @Singleton
    fun provideSsotPipelineUseCase(
        fileRepo: FileRepository,
        analysisRepo: AnalysisRepository,
        claimRepo: ClaimRepository,
        claimExtractionUseCase: ClaimExtractionUseCase,
        conflictDetectionUseCase: ConflictDetectionUseCase,
        settingsRepo: SettingsRepository,
        textExtractor: TextExtractor,
        pdfExtractor: PdfTextExtractor,
        @ApplicationContext context: Context
    ): SsotPipelineUseCase = SsotPipelineUseCase(
        fileRepo, analysisRepo, claimRepo,
        claimExtractionUseCase, conflictDetectionUseCase,
        settingsRepo, textExtractor, pdfExtractor, context
    )
}
