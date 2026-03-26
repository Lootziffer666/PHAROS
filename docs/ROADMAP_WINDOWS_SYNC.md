# Roadmap: Windows-Build & Sync-Anbindung

Chat-ID: CH-20260308-04

---

## 1 Ziel

Pharos soll neben Android auch unter **Windows** lauffähig sein, mit **Datei-Synchronisation** (lokal oder Google Drive). Dieses Dokument beschreibt die Optionen und empfohlene Reihenfolge.

---

## 2 Windows-Build: Optionen

### Option A: Kotlin Multiplatform (KMP) + Compose Multiplatform ⭐ Empfohlen

| Aspekt | Details |
|--------|---------|
| **Prinzip** | Bestehender Kotlin-Code wird in `commonMain` verschoben, plattformspezifischer Code in `androidMain`/`desktopMain` |
| **UI** | Compose Multiplatform (Desktop) – gleicher Compose-Code für Android und Windows |
| **Aufwand** | Mittel – bestehende Module müssen refactored, aber nicht neugeschrieben werden |
| **Vorteile** | Maximale Code-Wiederverwendung, gleiche Sprache, gleiche UI-API |
| **Nachteile** | Hilt nicht für Desktop verfügbar → Koin oder manuelles DI nötig |
| **Build-Output** | `.exe` / `.msi` via `jpackage` oder Conveyor |

**Migrationsschritte:**
1. `core/model` → `commonMain` (reine Kotlin-Datenklassen, kein Android-Import)
2. `core/llm` → `commonMain` (OkHttp durch Ktor ersetzen für Multiplatform-HTTP)
3. `core/storage` → `commonMain` mit `expect/actual` (DataStore auf Android, Preferences-File auf Desktop)
4. `feature/*` → `commonMain` (Compose Multiplatform statt `androidx.compose`)
5. `provider/*` → `commonMain` (HTTP-Client austauschen)
6. `app` → `androidMain` + `desktopMain` mit jeweiligem Entry-Point
7. DI: Hilt → Koin (Multiplatform-fähig) oder manuelles DI

### Option B: Separater Desktop-Client (Compose for Desktop)

| Aspekt | Details |
|--------|---------|
| **Prinzip** | Neues Gradle-Modul `desktop/` neben `app/`, shared Code in `core/` |
| **Aufwand** | Gering–Mittel – wenn `core/` bereits plattformunabhängig ist |
| **Vorteile** | Schneller Start, kein Full-KMP-Refactoring nötig |
| **Nachteile** | Code-Duplikation bei UI, zwei getrennte Build-Targets |

### Option C: Electron / Web-App

| Aspekt | Details |
|--------|---------|
| **Prinzip** | Separate Web-App (wie DocuPilot UI) mit Kotlin-Backend oder Python-CLI |
| **Aufwand** | Hoch – komplett neuer Tech-Stack |
| **Vorteile** | Maximale Plattform-Abdeckung (Windows, macOS, Linux, Browser) |
| **Nachteile** | Kein Code-Sharing mit Android, zwei Codebasen |

**Empfehlung:** Option A (KMP) für langfristige Strategie, Option B als schneller Prototyp.

---

## 3 Sync-Anbindung: Optionen

### Option 1: Lokaler Ordner-Sync ⭐ Einfachster Start

| Aspekt | Details |
|--------|---------|
| **Prinzip** | Pharos liest/schreibt in einen konfigurierbaren lokalen Ordner |
| **Android** | Storage Access Framework (SAF) für Ordner-Zugriff |
| **Windows** | Direkter Dateisystem-Zugriff via `java.nio.file` |
| **Sync-Mechanismus** | SHA-1-Manifest (wie DocuPilot) für Delta-Erkennung |
| **Vorteile** | Keine Cloud-Dependency, volle Kontrolle, DSGVO-konform |
| **Nachteile** | Kein automatischer Multi-Geräte-Sync |

**Umsetzung:**
```
core/sync/
├── SyncEngine.kt        # Manifest-basierte Delta-Berechnung
├── FileHasher.kt        # SHA-1 Hashing
├── SyncState.kt         # JSON-State-Persistenz
└── SyncRepository.kt    # Room/File-basierte State-Speicherung
```

### Option 2: Google Drive via rclone (wie DocuPilot)

| Aspekt | Details |
|--------|---------|
| **Prinzip** | rclone als externes Tool für Upload/Download zu Google Drive |
| **Android** | rclone als eingebetteter Binary (Termux-Ansatz) oder via REST-API |
| **Windows** | rclone CLI direkt aufrufbar |
| **Vorteile** | Bewährt (DocuPilot nutzt es), viele Cloud-Provider unterstützt |
| **Nachteile** | Externe Dependency, rclone-Konfiguration nötig, auf Android kompliziert |

### Option 3: Google Drive REST API (nativ)

| Aspekt | Details |
|--------|---------|
| **Prinzip** | Google Drive API v3 direkt via HTTP-Client |
| **Android** | Google Play Services oder REST-only |
| **Windows** | OAuth2 + REST-Aufrufe |
| **Vorteile** | Keine externe Dependency, volle Kontrolle |
| **Nachteile** | OAuth2-Flow, Google Cloud Console Setup, API-Quota |

### Option 4: Hybrid (Lokal + Optional Cloud)  ⭐ Empfohlen

| Aspekt | Details |
|--------|---------|
| **Prinzip** | Lokal als Standard, Cloud-Sync als optionales Feature |
| **Phase 1** | Lokaler Ordner-Sync mit Delta-Tracking |
| **Phase 2** | Google Drive als optionaler Remote-Sync |
| **Vorteile** | Schrittweise Umsetzung, sofort nutzbar ohne Cloud |
| **Nachteile** | Mehr Code für zwei Sync-Backends |

**Empfehlung:** Option 4 – Lokal zuerst, Cloud später.

---

## 4 Empfohlene Reihenfolge

### Phase 1: Pharos verbessern (Android)
- [ ] Unit-Tests für bestehende Repositories und ViewModels
- [ ] Datei-Scanner implementieren (SAF-basiert)
- [ ] Delta-Tracking (SHA-1-Manifest in Room-DB)
- [ ] Topic-Clustering (Keyword-basiert, analog DocuPilot)
- [ ] LLM-Chat-Completions tatsächlich aufrufen (nicht nur Ping)
- [ ] Widerspruchs-Erkennung (Regex-basiert)

### Phase 2: Lokaler Sync
- [ ] `core/sync/` Modul erstellen
- [ ] `SyncEngine` mit Manifest-Vergleich
- [ ] Android: SAF-Ordner als Sync-Ziel
- [ ] Sync-Status-UI in Settings-Screen

### Phase 3: Windows-Build
- [ ] `core/model` nach `commonMain` migrieren (KMP)
- [ ] HTTP-Client: OkHttp → Ktor (Multiplatform)
- [ ] DI: Hilt → Koin (Multiplatform)
- [ ] `core/storage` mit `expect/actual` für DataStore/Preferences
- [ ] `desktopMain` Entry-Point mit Compose for Desktop
- [ ] Windows-Installer via jpackage oder Conveyor
- [ ] CI/CD: GitHub Actions für Windows-Build (.exe/.msi)

### Phase 4: Google Drive (optional)
- [ ] Google Drive REST API v3 Integration
- [ ] OAuth2-Flow für Android + Desktop
- [ ] Sync-Konfigurations-UI
- [ ] Conflict Resolution (Last-Write-Wins oder manuelle Auflösung)

---

## 5 Technische Entscheidungen (noch offen)

| Entscheidung | Optionen | Empfehlung |
|--------------|----------|------------|
| **Multiplatform-Strategie** | KMP vs. separater Desktop-Client | KMP |
| **HTTP-Client** | OkHttp vs. Ktor | Ktor (KMP-fähig) |
| **DI-Framework** | Hilt vs. Koin vs. manuell | Koin (KMP-fähig) |
| **Serialisierung** | org.json vs. kotlinx.serialization vs. Gson | kotlinx.serialization (KMP-fähig) |
| **Sync-Backend** | Lokal-only vs. rclone vs. Drive API | Lokal zuerst, Drive API später |
| **Datenbank** | Room vs. SQLDelight | SQLDelight (KMP-fähig) |
| **Windows-Installer** | jpackage vs. Conveyor vs. WiX | Conveyor (einfach) |

---

## 6 Risiken

| Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|--------|---------------------|--------|------------|
| KMP-Migration bricht bestehenden Android-Code | Mittel | Hoch | Schrittweise migrieren, Android-Build nach jedem Schritt testen |
| Hilt → Koin Migration komplex | Gering | Mittel | Koin hat ähnliche API, Module 1:1 übertragbar |
| Google Drive OAuth auf Desktop umständlich | Hoch | Gering | Lokal-Sync als Fallback, Drive ist optional |
| Compose for Desktop Bugs/Inkompatibilitäten | Mittel | Mittel | JetBrains-Support, Community aktiv |

---

*Erstellt: 2026-03-24 | Chat-ID: CH-20260308-04*
