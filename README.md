# Pharos

Pharos is a dual-platform **document intelligence system** for **Android** and **Windows Desktop**.
Both platforms share a common sync protocol and provenance model via pure Kotlin/JVM core modules.

## What is Pharos?

**Pharos Android** is a native Android application that scans local document folders, analyses them
with a pluggable LLM backend, clusters them into projects, and writes Markdown "masterfiles" that
summarise each project. It supports a hybrid model where light processing happens on the phone and
heavier pipelines run on a PC-hosted local LLM gateway (Ollama or custom OpenAI-compatible endpoint).

**Pharos Desktop (Windows)** is a Compose for Desktop application that picks local and shared/network
folders, generates SHA-256 sync manifests, shows visual diffs between two folders, and syncs a shared
folder into a local destination. It uses the same `pharos_manifest.json` protocol as the Android app,
enabling bi-directional sync without a cloud intermediary.

## Module map

```
Pharos/
├── app/                       # Android application (Jetpack Compose, Hilt, Material 3)
├── desktop/                   # Windows Desktop application (Compose for Desktop)
│
├── core/
│   ├── sync/                  # ✦ Shared (pure JVM): SHA-256 manifest sync protocol
│   ├── truth/                 # ✦ Shared (pure JVM): Provenance model, trust semantics
│   ├── model/                 # Android: Room entities, data models, enums
│   ├── storage/               # Android: Room DB, DAOs, repositories, EncryptedSharedPreferences
│   └── llm/                   # Android: LlmGateway + AiApiProvider interfaces, JSON parser
│
└── provider/
    ├── perplexity/            # Perplexity API provider
    ├── ollama/                # Ollama (local LLM) provider
    └── customopenai/          # Custom OpenAI-compatible gateway provider
```

`✦` marks the two fully platform-agnostic modules that are shared by Android and Desktop today.
`core/model`, `core/storage`, `core/llm`, and all `provider/` modules are currently Android-specific.
Full KMP migration (Room → SQLDelight, OkHttp → Ktor, Hilt → Koin) is the recommended long-term path
— see [`docs/ROADMAP_WINDOWS_SYNC.md`](docs/ROADMAP_WINDOWS_SYNC.md) for the detailed plan.

## Android features

| Feature | Status |
|---------|--------|
| SAF-based recursive document scanner with SHA-256 delta detection | ✅ |
| AI document analysis — Perplexity API (topics, summaries, action items, confidence) | ✅ |
| Auto project clustering from AI analysis results | ✅ |
| Masterfile generation (`PHAROS_MASTER_<Project>.md` written to user folder) | ✅ |
| Room database — 5 entities (Folder, File, Analysis, Project, ProjectFileCrossRef) | ✅ |
| Encrypted API key storage — EncryptedSharedPreferences + Android Keystore | ✅ |
| PDF text extraction via PDFBox-Android | ✅ |
| 3 LLM providers — Perplexity, Ollama (local), Custom OpenAI gateway | ✅ |
| Budget management — daily spend limit with cheap-first policy | ✅ |
| 5-tab navigation — Dashboard, Folders, Files, Projects, Settings | ✅ |
| Sync conflict detection with provenance metadata (UI surfacing pending) | ⚠️ |
| Cloud sync (Google Drive) | ❌ |
| Contradiction detection UI | ❌ |
| Relation graph canvas | ❌ |

## Desktop features (Windows)

| Feature | Status |
|---------|--------|
| Local and shared/network folder picker | ✅ |
| SHA-256 manifest generation and display | ✅ |
| Visual diff — added / modified / deleted / unchanged | ✅ |
| One-click sync from shared folder to local | ✅ |
| Cloud sync | ❌ |
| Watch-mode (background continuous sync) | ❌ |

## Shared modules

### `core/sync`
Manifest-based delta sync protocol shared by Android and Desktop:
- `SyncManifest` / `ManifestEntry` — JSON file manifest (path, hash, size, timestamp)
- `ManifestComparator` — diffs two manifests → `SyncDiff` (added / modified / deleted / unchanged)
- `FileHasher` — SHA-256 hashing for files, streams, and byte arrays
- `SyncEngine` — manifest generation, read/write `pharos_manifest.json`, one-way sync
- `SyncConflictDetector` — surfaces bi-directional edit conflicts with provenance metadata
- **33 unit tests**

### `core/truth`
Provenance-first truth model for transparent data lineage:
- `ProvenanceLevel` — SOURCE → EXTRACTION → DERIVATION → HYPOTHESIS
- `VerificationState` — Unverified / Confirmed / Disputed / Outdated
- `TrustMetadata` — freshness, staleness, safe-to-resume classification
- `TrustAssessment<T>` — wraps any value with full trust context and labels
- `ProvenanceRecord` — links claims to source evidence with derivation chains
- `ProvenanceClassifier` — classifies AI analysis, file scans, clustering by provenance level
- **71 unit tests**

## Build

```bash
# Android Debug APK
./gradlew assembleDebug

# Windows Desktop JAR
./gradlew :desktop:packageUberJarForCurrentOS
```

## Test

```bash
# Shared JVM modules (no emulator required)
./gradlew :core:truth:test :core:sync:test --no-daemon

# Android app unit tests
./gradlew :app:test --no-daemon
```

## CI/CD

GitHub Actions builds both targets on every push:
- **Ubuntu runner** → Android Debug APK artifact
- **Windows runner** → Desktop JAR artifact

## Docs

- [`docs/ROADMAP_WINDOWS_SYNC.md`](docs/ROADMAP_WINDOWS_SYNC.md) — full KMP migration plan and sync options
- [`docs/SUPPORT_MATRIX.md`](docs/SUPPORT_MATRIX.md) — detailed implemented / missing feature matrix
- [`docs/LOCAL_LLM_API_SPEC.md`](docs/LOCAL_LLM_API_SPEC.md) — LLM gateway API specification
- [`docs/COMPARISON_PHAROS_VS_DOCUPILOT.md`](docs/COMPARISON_PHAROS_VS_DOCUPILOT.md) — benchmark against DocuPilot reference project
