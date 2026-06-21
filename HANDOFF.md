# HANDOFF: PHAROS SSOT Claims Pipeline

## PR & Branch

- **PR**: [#19 — feat: SSOT Claims Pipeline mit Conflict Resolution & Timeline](https://github.com/Lootziffer666/PHAROS/pull/19)
- **Branch**: `feature/ssot-claims-pipeline`
- **Repo**: `Lootziffer666/PHAROS`
- **Diff**: 21 Dateien, +1681 Zeilen, 4 Commits

---

## 1. Kontext & Ausgangslage

### Problemstellung
Der User hat über Jahre verstreute Dokumente in verschiedenen Wikis, Knowledge Bases und Second-Brain-Repos gesammelt. Diese enthalten teils widersprüchliche Informationen. Ziel: Eine **Single Source of Truth (SSOT)** automatisiert aufbauen -- plattformübergreifend (Android + Windows Desktop).

### Vor-Zustand: Zwei Repos

| Repo | Rolle | Tech Stack |
|------|-------|-----------|
| **PHAROS** | Dual-Platform Document Intelligence | Kotlin, Compose Multiplatform (Android + Desktop), Hilt, Room, Multi-LLM-Provider |
| **NUGGETZ** | Simpler Android-Prototyp mit Tinder-UI | Kotlin, Compose, Room, Gemini API |

**Entscheidung**: PHAROS als Lead-Repo (bessere Architektur: modular, dual-platform, provider-agnostisch). NUGGETZ-Konzepte (Claim-Modell, SwipeCard, Undo) werden in PHAROS integriert.

---

## 2. Architektur-Überblick

```
+---------------------------------------------------------------------+
|                         PHAROS (Post-PR)                             |
+---------------------------------------------------------------------+
|                                                                     |
|  Dokument -> ScanUseCase -> FileEntity in Room DB                   |
|       |                                                             |
|  AnalysisUseCase -> LLM-Analyse pro File -> AnalysisEntity          |
|       |                                                             |
|  SsotPipelineUseCase (NEU)                                          |
|       +-- ClaimExtractionUseCase -> atomare Claims extrahieren       |
|       +-- ClaimRepository.insertAll() -> PENDING Claims in DB        |
|       +-- ConflictDetectionUseCase                                   |
|            +-- Kein Widerspruch -> Auto-Merge (APPROVED)             |
|            +-- Widerspruch -> CONFLICT + aiRationale                 |
|       |                                                             |
|  ConflictResolutionScreen (Tinder Cards)                             |
|       +-- Swipe Right -> approveAndSupersede() [Transactional]       |
|       +-- Swipe Left  -> REJECTED                                    |
|       |                                                             |
|  TimelineScreen (chronologisch, filterbar, Provenance-Chain)         |
|                                                                     |
+---------------------------------------------------------------------+
```

---

## 3. Neue Dateien & Module

### 3.1 Data Model (`core/model/`)

| Datei | Zweck |
|-------|-------|
| `entity/ClaimEntity.kt` | Atomarer Fakt mit Provenance-Feldern |
| `ClaimStatus.kt` | Enum: PENDING, APPROVED, REJECTED, CONFLICT, SUPERSEDED |
| `ClaimExtractionResponse.kt` | Gson-Modell für LLM-Response-Parsing |

**ClaimEntity-Felder:**
```kotlin
@Entity(tableName = "claims", foreignKeys = [FileEntity FK], indices = [sourceFileId, status, clusterId])
data class ClaimEntity(
    val id: String,                    // UUID
    val content: String,               // Der atomare Fakt
    val sourceFileId: String,          // FK -> FileEntity.id
    val sourceFileName: String,        // Für Display ohne Join
    val sourceTimestamp: Long,         // Wann wurde das Quelldokument zuletzt geändert
    val extractedAt: Long,             // Wann wurde der Claim extrahiert
    val status: ClaimStatus,           // Lifecycle-Status
    val confidence: Double,            // LLM-Confidence 0.0-1.0
    val clusterId: String?,            // Topic-Cluster (vom LLM vergeben)
    val supersededById: String?,       // Wer hat mich abgelöst? (Provenance)
    val supersedes: String?,           // Wen löse ich ab? (Provenance)
    val aiRationale: String?           // Warum ist das ein Konflikt? (LLM-Erklärung)
)
```

### 3.2 Storage Layer (`core/storage/`)

| Datei | Zweck |
|-------|-------|
| `db/dao/ClaimDao.kt` | Room DAO mit `@Transaction`-Methoden für atomare Approve/Supersede |
| `repository/ClaimRepository.kt` | Repository-Wrapper über ClaimDao |
| `repository/SettingsRepository.kt` | Erweitert um `getLlmModel()`, `getLlmProviderType()` |
| `db/PharosDatabase.kt` | Version 2 mit Migration (ALTER TABLE ADD claims) |

**Wichtig: Room Migration(1,2)**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `claims` (
            `id` TEXT NOT NULL, `content` TEXT NOT NULL,
            `sourceFileId` TEXT NOT NULL, `sourceFileName` TEXT NOT NULL,
            `sourceTimestamp` INTEGER NOT NULL, `extractedAt` INTEGER NOT NULL,
            `status` TEXT NOT NULL, `confidence` REAL NOT NULL,
            `clusterId` TEXT, `supersededById` TEXT, `supersedes` TEXT,
            `aiRationale` TEXT,
            PRIMARY KEY(`id`),
            FOREIGN KEY(`sourceFileId`) REFERENCES `files`(`id`) ON DELETE CASCADE
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_claims_sourceFileId` ON `claims` (`sourceFileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_claims_status` ON `claims` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_claims_clusterId` ON `claims` (`clusterId`)")
    }
}
```

### 3.3 Business Logic (`app/usecase/`)

| Datei | Zweck |
|-------|-------|
| `ClaimExtractionUseCase.kt` | LLM-Prompt -> atomare Claims aus Text extrahieren |
| `ConflictDetectionUseCase.kt` | Claims gegen SSOT prüfen, auto-merge oder CONFLICT |
| `SsotPipelineUseCase.kt` | Orchestrator: Text -> Claims -> Conflict Check |

**Pipeline-Flow im Detail:**
1. `SsotPipelineUseCase.runPipeline(fileId)` wird aufgerufen
2. Prüft ob AnalysisEntity existiert (File muss vorher analysiert sein)
3. Extrahiert Text aus File (PDF/TXT/MD)
4. `ClaimExtractionUseCase.extractClaims()` sendet strukturierten Prompt an LLM
5. LLM antwortet mit JSON: `{"claims": [{"content": "...", "confidence": 0.95, "cluster": "topic"}]}`
6. Claims werden als PENDING in DB gespeichert
7. `ConflictDetectionUseCase.detectConflicts()`:
   - Groupiert approved Claims nach `clusterId`
   - Für jeden neuen Claim: Wenn Cluster leer -> sofort APPROVED
   - Sonst: LLM-basierter Widerspruchs-Check gegen existierende Claims
   - LLM antwortet mit: `{"contradicts": true/false, "rationale": "..."}`
   - `contradicts: false` -> Auto-Merge (APPROVED, kein User-Input nötig)
   - `contradicts: true` -> CONFLICT + aiRationale gespeichert

### 3.4 UI Layer (`app/ui/`)

| Datei | Zweck |
|-------|-------|
| `components/SwipeCard.kt` | Tinder-Karte mit Drag-Gesture, Rotation, Approve/Reject-Overlay |
| `screen/ConflictResolutionScreen.kt` | Card-Stack für CONFLICT-Claims |
| `screen/TimelineScreen.kt` | Chronologische Liste aller Claims mit Filter-Chips |
| `viewmodel/ConflictResolutionViewModel.kt` | Undo-Stack, approve/reject/supersede Logic |
| `viewmodel/TimelineViewModel.kt` | Flow-basiert mit Status-Filter |
| `navigation/Screen.kt` | Erweitert: `Conflicts` + `Timeline` in Bottom Nav |

**SwipeCard zeigt:**
- Obere Hälfte: **NEUER CLAIM** (Content + Source + Timestamp)
- Trennlinie
- Untere Hälfte: **EXISTIERENDER CLAIM** (der widerspricht)
- Unten: **AI Conflict Analysis** (warum widerspricht es sich)
- Swipe Right = Neuen Claim akzeptieren, alten superseden
- Swipe Left = Neuen Claim ablehnen, alten behalten

### 3.5 DI (`app/di/AppModule.kt`)

Erweitert um:
- `provideClaimRepository`
- `provideLlmGateway` (wählt Provider basierend auf User-Settings)
- `provideClaimExtractionUseCase`
- `provideConflictDetectionUseCase`
- `provideSsotPipelineUseCase`

### 3.6 Tests (`app/src/test/`)

| Datei | Coverage |
|-------|----------|
| `ClaimExtractionParsingTest.kt` | 11 Tests: Valid JSON, Code Fences, Null Cluster, Empty, Invalid, Boundary Values |

---

## 4. Datenfluss-Diagramm

```
              +----------+
              | Dokument |  (PDF, TXT, MD auf Device/Desktop)
              +----+-----+
                   | ScanUseCase
                   v
              +----------+
              |FileEntity|  (Room: id, name, path, hash, lastModified)
              +----+-----+
                   | AnalysisUseCase (bestehend)
                   v
           +---------------+
           |AnalysisEntity |  (summary, topics, actionItems per File)
           +-------+-------+
                   | SsotPipelineUseCase.runPipeline()
                   v
     +-------------------------+
     | ClaimExtractionUseCase  |
     | LLM Prompt:             |
     | "Extract atomic facts"  |
     +------------+------------+
                  | List<ClaimEntity> (PENDING)
                  v
     +-------------------------+
     |ConflictDetectionUseCase |
     | For each claim:         |
     |  1. Find same-cluster   |
     |     approved claims     |
     |  2. LLM: "contradicts?" |
     +------+----------+-------+
            |          |
     No conflict    Conflict!
            |          |
            v          v
     +----------+  +------------------+
     | APPROVED |  | CONFLICT         |
     |(auto-    |  |+ aiRationale     |
     | merged)  |  |-> Tinder Card    |
     +----------+  +--------+---------+
                            | User swipes
                            v
              +-------------------------+
              | Swipe Right:            |
              |  newClaim -> APPROVED   |
              |  oldClaim -> SUPERSEDED |
              |  (bidirektionale Links) |
              +-------------------------+
              | Swipe Left:             |
              |  newClaim -> REJECTED   |
              |  oldClaim bleibt        |
              +-------------------------+
```

---

## 5. Provenance-Chain (Zeitstempel-System)

Jeder Claim trägt drei Zeitebenen:

| Feld | Bedeutung |
|------|-----------|
| `sourceTimestamp` | Wann wurde das **Quelldokument** zuletzt geändert (File Modified) |
| `extractedAt` | Wann wurde der **Claim extrahiert** (Pipeline-Lauf) |
| `supersedes` / `supersededById` | **Ablösungs-Kette**: Wer hat wen ersetzt |

Die Timeline zeigt diese drei Ebenen visuell: Source-Datum, Extraktions-Datum, und Supersession-Links.

---

## 6. Bekannte Limitierungen & offene Arbeit

| # | Issue | Impact | Vorgeschlagene Lösung |
|---|-------|--------|----------------------|
| 1 | **Pipeline-Concurrency** | Parallele Runs für verschiedene Files könnten Claims verpassen | Serialize Pipeline-Runs oder Re-Read approved Claims per Iteration |
| 2 | **Cluster-scoped Detection** | Claims mit unterschiedlichen Cluster-Labels vom LLM werden nicht verglichen | Cross-Cluster-Vergleich mit Embedding-Similarity als Vorfilter |
| 3 | **LLM-Provider gebunden bei App-Start** | Wechsel braucht App-Neustart | Hot-swap via `@Inject Provider<LlmGateway>` |
| 4 | **Kein OCR** | PDFs mit Bildern/Scans nicht lesbar | Gate PH-011 (ML Kit on-device OCR) |
| 5 | **Desktop-Modul nicht aktualisiert** | `desktop/` hat keine Claims-UI | Port der Compose-Screens nach Desktop |
| 6 | **Keine Embeddings** | Conflict Detection nur via LLM-Prompt (langsam, teuer) | Lokale Embedding-Modelle für schnellen Vorfilter |
| 7 | **Kein Export** | SSOT nur in App sichtbar | Markdown-/JSON-Export der SSOT |

---

## 7. Nächste sinnvolle Schritte (priorisiert)

1. **Gate PH-011: OCR Pipeline** -- Damit auch gescannte Dokumente in die Claims-Pipeline können
2. **Embedding-basierter Vorfilter** -- Cross-Cluster Conflict Detection ohne teuren LLM-Call für jedes Paar
3. **Desktop-Parity** -- Claims/Conflicts/Timeline-Screens im Windows-Desktop-Modul
4. **Batch-Import** -- Ganzen Ordner auf einmal durch die Pipeline jagen (aktuell: File-by-File)
5. **SSOT-Export** -- Markdown-Masterfile oder JSON-Dump der approved Claims mit Provenance

---

## 8. Build & Test Commands

```bash
# Build
cd /projects/sandbox/PHAROS && ./gradlew assembleDebug --no-daemon

# Tests
cd /projects/sandbox/PHAROS && ./gradlew testDebugUnitTest --no-daemon

# Nur Claim-Tests
cd /projects/sandbox/PHAROS && ./gradlew :app:testDebugUnitTest --tests "com.flow.pharos.ClaimExtractionParsingTest" --no-daemon
```

---

## 9. Konfiguration (Runtime)

Die App nutzt `SettingsRepository` (EncryptedSharedPreferences) für:

| Key | Default | Zweck |
|-----|---------|-------|
| `llm_provider` | `"ollama"` | Welcher LLM-Provider für Claims-Pipeline |
| `llm_model` | `null` -> fallback `"gemma3"` | Welches Modell für Extraktion + Conflict Check |
| `ai_api_key` | `null` | API-Key für Perplexity (File-Analyse) |

Provider-Auswahl in `AppModule.provideLlmGateway()`:
- `"ollama"` -> `OllamaProvider` (lokal, `OLLAMA_BASE_URL` aus `local.properties`)
- `"custom_openai"` -> `CustomOpenAiProvider` (eigener Endpoint)

---

## 10. Abhängigkeiten zwischen Repos

| Repo | Status nach PR |
|------|---------------|
| **PHAROS** | Lead-Repo, enthält jetzt alles von NUGGETZ + mehr |
| **NUGGETZ** | Kann archiviert werden -- alle relevanten Konzepte sind portiert |

Was übernommen wurde:
- Claim-Datenmodell (erweitert um Provenance-Felder)
- SwipeCard-Composable (erweitert: zeigt beide Claims + AI-Rationale)
- Undo-Stack-Logik
- LLM-Analyse pro Claim
- Progress-Tracking

Was PHAROS zusätzlich hat:
- Auto-Merge (NUGGETZ zeigte ALLE Claims als Karten)
- Transaktionale DB-Operationen
- Provenance-Chain mit Supersession-Links
- Timeline-View mit Filter-Chips
- Multi-LLM-Provider (Ollama, Custom OpenAI, Perplexity)
- Room Migration (kein Datenverlust)
- Desktop-ready Architektur (shared `core/`)
