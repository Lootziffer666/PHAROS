package com.flow.pharos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flow.pharos.core.model.*
import com.flow.pharos.core.storage.SeedRepository
import com.flow.pharos.feature.archive.ArchiveScreen
import com.flow.pharos.feature.relations.RelationsScreen
import com.flow.pharos.feature.settings.SettingsScreen
import com.flow.pharos.provider.customopenai.CustomOpenAiProvider
import com.flow.pharos.provider.ollama.OllamaProvider
import com.flow.pharos.provider.perplexity.PerplexityProvider

class MainActivity: ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { PharosApp() }
  }
}

class MainViewModel: ViewModel() {
  private val repo = SeedRepository()
  val state = mutableStateOf(
    PharosUiState(
      archive = repo.archive(),
      llmProviders = listOf(
        LlmProviderState(LlmProviderKind.PERPLEXITY, "Perplexity", BuildConfig.PERPLEXITY_API_KEY.isNotBlank(), BuildConfig.PERPLEXITY_API_KEY.isNotBlank(), "web-grounded cloud"),
        LlmProviderState(LlmProviderKind.OLLAMA, "Ollama / local", true, BuildConfig.OLLAMA_BASE_URL.isNotBlank(), "free local models + pull menu"),
        LlmProviderState(LlmProviderKind.CUSTOM_OPENAI, "Custom gateway", true, BuildConfig.CUSTOM_OPENAI_BASE_URL.isNotBlank(), "AnythingLLM-like no-code pipeline backend")
      )
    )
  )
  val providers = listOf(
    PerplexityProvider(BuildConfig.PERPLEXITY_API_KEY),
    OllamaProvider(BuildConfig.OLLAMA_BASE_URL),
    CustomOpenAiProvider(BuildConfig.CUSTOM_OPENAI_BASE_URL, BuildConfig.CUSTOM_OPENAI_API_KEY)
  )
}

@Composable fun PharosApp(vm: MainViewModel = viewModel()) {
  val ui = vm.state.value
  var tab by remember { mutableStateOf(0) }
  Scaffold(topBar = { TopAppBar(title = { Text("Pharos · ${BuildConfig.CHAT_ID}") }) }) { pad ->
    Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
      TabRow(selectedTabIndex = tab) {
        listOf("Archive","Relations","Settings").forEachIndexed { i, label -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) }) }
      }
      when(tab) {
        0 -> ArchiveScreen(ui.archive.artifacts)
        1 -> RelationsScreen(ui.archive.relations)
        else -> SettingsScreen(ui)
      }
    }
  }
}
