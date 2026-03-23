# SUPPORT_MATRIX
Chat-ID: CH-20260308-04

Implemented
- modular project skeleton
- archive/relation/settings screens
- free local model menu
- paid-model daily budget policy
- AnythingLLM-like custom gateway seam
- future-ready idea for chat extraction into next pipeline step
- `app/build.gradle.kts` with all BuildConfig fields (PERPLEXITY_API_KEY, OLLAMA_BASE_URL, CUSTOM_OPENAI_BASE_URL, CUSTOM_OPENAI_API_KEY, CHAT_ID)
- bundled `gradle/wrapper/gradle-wrapper.jar`

Fixed
- LazyColumn nested inside verticalScroll Column (unbounded-height runtime crash) — content now wrapped in Box(weight(1f)), ArchiveScreen/RelationsScreen use fillMaxSize()
- SettingsScreen now scrolls independently via verticalScroll
- AndroidManifest: added android:icon and android:supportsRtl

Missing / partial
- no repository scanner yet
- no graph canvas yet
- no real LLM network calls (ping only)
