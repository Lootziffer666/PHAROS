# SUPPORT_MATRIX
Chat-ID: CH-20260308-04

## Implemented (Phase 1 – Baseline)
- modular project skeleton (core/model, core/storage, core/llm, feature/*, provider/*)
- archive/relation/settings screens (legacy, replaced by new screens below)
- free local model menu
- paid-model daily budget policy
- AnythingLLM-like custom gateway seam
- future-ready idea for chat extraction into next pipeline step
- `app/build.gradle.kts` with all BuildConfig fields
- bundled `gradle/wrapper/gradle-wrapper.jar`

## Implemented (Phase 2 – DocuPilot Feature Port)
- **Room Database** with 5 entities (Folder, File, Analysis, Project, ProjectFileCrossRef)
- **SAF File Scanner** – recursive document scanning via Storage Access Framework with SHA-256 hash-based delta detection
- **AI Document Analysis** – Perplexity API integration with structured JSON response parsing (topics, project suggestions, summary, action items, confidence)
- **Project Clustering** – auto-derives projects from AI analysis results (suggestion + topic frequency)
- **Masterfile Generation** – writes `PHAROS_MASTER_<ProjectName>.md` into user-selected folder
- **Encrypted API Key Storage** – EncryptedSharedPreferences backed by Android Keystore
- **PDF Text Extraction** – PDFBox-Android for `.pdf` content extraction
- **New 5-Tab Navigation** – Dashboard, Folders, Files, Projects, Settings
- **Dashboard Screen** – scan/analyze/update buttons with progress indicators and cancel support
- **Folders Screen** – SAF folder picker with persistent URI permissions
- **Files Screen** – file list with status icons (NEVER, UP_TO_DATE, STALE, FAILED, UNSUPPORTED)
- **File Detail Screen** – analysis results (summary, topics, action items, confidence) + per-file analyze button
- **Projects Screen** – auto-generated project list with detail view showing assigned files
- **Settings Screen** – encrypted API key management, test, analysis mode toggle, privacy notice
- **PharosTheme** – Material3 light/dark/dynamic color theme
- **Unit Tests** – 28 tests (JsonParser, FilenameSanitizer, ProjectClustering)
- **Hilt DI** – all new dependencies wired via Dagger Hilt @Module

## Implemented (Phase 3 – Sync & Windows Desktop)
- **`core/sync` module** – pure Kotlin/JVM library with SHA-256 manifest-based sync protocol
  - `SyncManifest` / `ManifestEntry` – JSON-serializable file manifest (path, hash, size, timestamp)
  - `ManifestComparator` – compares two manifests → `SyncDiff` (added, modified, deleted, unchanged)
  - `FileHasher` – SHA-256 hashing for `File`, `InputStream`, and `ByteArray`
  - `SyncEngine` – generates manifests, reads/writes `pharos_manifest.json`, one-way sync
  - **33 unit tests** (ManifestComparator, FileHasher, SyncEngine, SyncConflictDetector)
- **`desktop/` module** – Compose for Desktop (Windows) application
  - Folder picker (local + remote/shared)
  - Generate & display manifest
  - Compare two folders → visual diff (new / modified / deleted / unchanged)
  - One-click sync from shared folder to local
- **CI/CD** – GitHub Actions builds both APK (Ubuntu) and Windows desktop JAR (Windows runner)
- **Android ↔ Windows sync protocol** – both platforms use the same `pharos_manifest.json` format and `core/sync` comparison logic

## Implemented (Phase 4 – Truth Model & Cross-Platform Parity)
- **`core/truth` module** – pure Kotlin/JVM shared truth model for provenance-first lighthouse semantics
  - `ProvenanceLevel` – semantic layers: SOURCE, EXTRACTION, DERIVATION, HYPOTHESIS
  - `VerificationState` – sealed class: Unverified, Confirmed, Disputed, Outdated
  - `ConflictRecord` – explicit conflict surfacing with resolution tracking
  - `TrustMetadata` – freshness, staleness detection, safe-to-resume classification
  - `ProvenanceRecord` – links claims to source evidence with derivation chains
  - `TrustAssessment<T>` – wraps any value with full trust context and trust labels
  - `ProvenanceClassifier` – classifies content (AI analysis, file scans, clustering) into provenance levels
- **Sync conflict detection** (`SyncConflictDetector`) – detects when both devices modify the same file, surfaces conflicts with provenance metadata instead of silently resolving
- **Cross-platform parity** – `core/truth` is pure JVM, shared identically by Android app and Desktop client
- **Lighthouse tests** – 62+ unit tests covering:
  - Provenance level ordering and boundary semantics
  - Verification state transitions
  - Conflict detection, surfacing, and resolution
  - Trust metadata freshness and staleness
  - Provenance record source-backing and verification needs
  - Trust assessment reliability, uncertainty, and label consistency
  - AI content classification (never presented as source fact)
  - Sync conflict detection with provenance metadata
  - Platform parity scenarios (resume after days away, consistent trust labels)

## Fixed
- LazyColumn nested inside verticalScroll Column (unbounded-height runtime crash)
- SettingsScreen now scrolls independently via verticalScroll
- AndroidManifest: added android:icon and android:supportsRtl

## Missing / partial
- no cloud sync yet (local folder sync via SAF is implemented, shared-folder manifest sync is implemented)
- no graph canvas yet
- contradiction detection: conflict model in place (`ConflictRecord`, `SyncConflictDetector`), UI surfacing pending
- no topic clustering rules yet (AI-driven clustering is implemented, now classified as DERIVATION via ProvenanceClassifier)
