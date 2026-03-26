# Vergleich: Pharos Android vs. DocuPilot

Chat-ID: CH-20260308-04

Beide Projekte wurden mit demselben Masterprompt erzeugt.
**Pharos** wurde von Codex generiert, **DocuPilot** von Claude Code.

---

## 1 Überblick

| Aspekt | Pharos Android | DocuPilot |
|--------|---------------|-----------|
| **Generator** | OpenAI Codex | Anthropic Claude Code |
| **Sprache(n)** | Kotlin (100 %) | Python + JavaScript + Kotlin |
| **Plattform** | Android (API 27–35) | Cross-Platform (CLI + Web-UI + Android-Shell) |
| **UI** | Jetpack Compose / Material 3 | Vanilla HTML/CSS/JS (Web-UI) + Android WebView |
| **Architektur** | MVVM, Multi-Module Gradle | Flat-Script-CLI + statischer Web-Client |
| **LOC (ca.)** | ~1 050 Kotlin | ~350 Python + ~100 JS + ~150 HTML/CSS |
| **LLM-Anbindung** | 3 Provider (Perplexity, Ollama, Custom OpenAI) | Vorgesehen in UI, nicht implementiert |
| **Tests** | 132+ Unit-Tests (app + sync + truth) | 2 Unit-Tests (Python unittest) |
| **CI/CD** | GitHub Actions → APK-Artifact | Keine |
| **Sync** | Keiner | Google Drive via rclone + Watch-Mode |
| **Dokumentation** | README + SUPPORT_MATRIX + LOCAL_LLM_API_SPEC | README (ausführlicher) |

---

## 2 Feature-Matrix

| Feature | Pharos | DocuPilot | Gewinner |
|---------|--------|-----------|----------|
| Archiv/Dokument-Browsing | ✅ LazyColumn mit ArtifactCards | ✅ Inventory-Matrix (JSON/CSV) | Gleichauf |
| Relation Mapping | ✅ RelationEdge-Karten | ❌ | Pharos |
| Topic-Clustering | ❌ | ✅ regelbasiert (5 Cluster) | DocuPilot |
| Widerspruchs-Erkennung | ❌ | ✅ Regex-basiert | DocuPilot |
| LLM-Provider-Auswahl | ✅ 3 Provider + Ping-Check | ❌ (nur UI-Platzhalter) | Pharos |
| Budget-/Kosten-Policy | ✅ Daily Limit + DataStore | ❌ | Pharos |
| Lokales Modell-Management | ✅ Ollama Pull + Katalog | ❌ | Pharos |
| Cloud-Sync | ❌ | ✅ Google Drive via rclone | DocuPilot |
| Watch-Mode (Continuous Sync) | ❌ | ✅ Polling-basiert | DocuPilot |
| Web-UI | ❌ | ✅ Tabs, Icons, Glassmorphism | DocuPilot |
| Android-App (nativ) | ✅ Jetpack Compose | ⚠️ WebView-Shell | Pharos |
| Dependency-Injection | ✅ Hilt/Dagger | ❌ | Pharos |
| Modulares Build-System | ✅ 9 Gradle-Module | ❌ (Flat Scripts) | Pharos |
| Tests | ✅ 132+ Unit-Tests | ✅ (2 Unit-Tests) | Pharos |
| CI/CD Pipeline | ✅ GitHub Actions | ❌ | Pharos |
| Verschlüsselte Key-Speicherung | ⚠️ BuildConfig (kompilierzeit) | ❌ | Pharos |
| Datei-Scanner (Filesystem) | ❌ (geplant) | ✅ rglob-basiert | DocuPilot |
| Datei-Hashing / Delta-Erkennung | ❌ | ✅ SHA-1 Manifest | DocuPilot |
| Cross-Platform (Desktop/CLI) | ❌ (nur Android) | ✅ Python überall lauffähig | DocuPilot |

---

## 3 Stärken von Pharos (Pros)

1. **Saubere Architektur** – Multi-Module mit `core/`, `feature/`, `provider/`-Trennung folgt Clean-Architecture-Prinzipien.
2. **Native Android-Erfahrung** – Jetpack Compose + Material 3 gibt eine echte mobile UI, kein WebView-Wrapper.
3. **LLM-Gateway-Abstraktion** – Einheitliches `LlmGateway`-Interface macht neue Provider trivial hinzufügbar.
4. **Budget-Management** – Daily-Limit + "Cheap-first"-Policy für kostenbewussten LLM-Einsatz.
5. **Ollama-Integration** – Lokale Modelle direkt vom Handy aus verwalten (Pull, List, Ping).
6. **CI/CD out-of-the-box** – GitHub Actions baut automatisch die Debug-APK bei jedem Push.
7. **Dependency-Injection** – Hilt macht Austausch und Testing der Abhängigkeiten einfach.
8. **Compile-time Konfiguration** – API-Keys und URLs über `local.properties` → `BuildConfig` injiziert.
9. **API-Spezifikation** – `LOCAL_LLM_API_SPEC.md` definiert klares Gateway-Protokoll für spätere Erweiterung.

---

## 4 Schwächen von Pharos (Stand: Phase 4 – inzwischen teilweise behoben)

1. ~~**Keine Tests**~~ – **Behoben**: 132+ Unit-Tests (28 app + 33 sync + 71 truth).
2. ~~**Kein Datei-Scanner**~~ – **Behoben**: SAF-basierter Datei-Scanner mit SHA-256 Delta-Erkennung.
3. ~~**Kein Sync**~~ – **Teilweise behoben**: Lokaler Ordner-Sync implementiert; Cloud-Sync (Google Drive) fehlt noch.
4. ~~**Nur Android**~~ – **Behoben**: Windows Desktop-Client via Compose for Desktop.
5. **LLM-Aufrufe nur Ping** – Chat-Completions sind implementiert, werden aber nirgends aufgerufen.
6. ~~**Kein Topic-Clustering**~~ – **Behoben**: AI-gestütztes Clustering via `ProjectClusteringUseCase`.
7. ~~**Kein Delta-Tracking**~~ – **Behoben**: SHA-256 Manifest in `core/sync`.
8. ~~**Seed-Daten statisch**~~ – **Behoben**: Echter SAF-Scan ersetzt Seed-Daten.
9. **Kein Web-UI** – Keine browser-basierte Alternative für Desktop-Nutzer.
10. **Manuelles JSON-Parsing** – Nutzt Gson; kotlinx.serialization wäre KMP-konformer.

---

## 5 Stärken von DocuPilot (Pros)

1. **Cross-Platform** – Python-CLI läuft auf Linux, macOS und Windows.
2. **Google-Drive-Sync** – Produktionsreifer rclone-basierter Sync mit Watch-Mode.
3. **Datei-Scanner** – Automatisches Indexieren von Dateien mit Topic-Clustering und Hashing.
4. **Widerspruchs-Erkennung** – Regex-basierte Contradiction-Hints (z.B. "must" vs. "must not").
5. **Delta-Tracking** – SHA-1-basiertes Manifest erkennt Created/Changed/Deleted.
6. **Dependency-Light** – Keine Python-Dependencies für den Core, nur rclone als externes Tool.
7. **Web-UI** – Sofort nutzbar im Browser (kein Build-Schritt nötig).
8. **Tests vorhanden** – Unittest-basierte Tests für Drive-Sync (dry-run, target-change).
9. **Structured Output** – Inventory als JSON + CSV + Modular-Schedule in einem Schritt.

---

## 6 Schwächen von DocuPilot (Cons)

1. **Keine echte Architektur** – Flache Script-Dateien ohne Modularisierung oder DI.
2. **Android nur als WebView-Shell** – Keine native Android-UI, keine Compose-Integration.
3. **Kein LLM-Backend** – LLM-Integration nur als UI-Platzhalter, keine echte Provider-Logik.
4. **Kein Budget-Management** – Keine Kostenkontrolle für LLM-Nutzung.
5. **Kein CI/CD** – Keine automatische Build-Pipeline.
6. **Kein Relation Mapping** – Keine Darstellung von Beziehungen zwischen Dokumenten.
7. **Regelbasiertes Clustering** – Keyword-Matching statt semantischer Analyse (begrenzte Genauigkeit).
8. **Web-UI nicht connected** – UI ist statisch, nicht an Backend angebunden.
9. **Kein Gradle-Multi-Module** – Keine saubere Build-Struktur für die Android-Komponente.

---

## 7 Was Pharos von DocuPilot übernehmen sollte

### 7.1 Priorität HOCH

| Feature | Aufwand | Nutzen | Umsetzungsidee |
|---------|---------|--------|-----------------|
| **Datei-Scanner** | Mittel | Hoch | SAF-basiertes `rglob` → `ArtifactRecord`-Liste statt Seed-Daten |
| **Google-Drive-Sync** (oder lokal) | Mittel–Hoch | Hoch | `drive_sync.py`-Logik nach Kotlin portieren oder rclone als Subprocess |
| **Delta-Tracking** | Gering | Hoch | SHA-1-Manifest in Room-DB, Vergleich bei jedem Scan |
| **Tests** | Mittel | Hoch | Unit-Tests für Repositories, ViewModels; UI-Tests für Compose-Screens |

### 7.2 Priorität MITTEL

| Feature | Aufwand | Nutzen | Umsetzungsidee |
|---------|---------|--------|-----------------|
| **Topic-Clustering** | Gering | Mittel | Keyword-basiert wie DocuPilot, später LLM-gestützt |
| **Widerspruchs-Erkennung** | Gering | Mittel | Regex-Patterns auf Dokument-Content anwenden |
| **Structured Output (JSON/CSV)** | Gering | Mittel | Export-Funktion für Inventory-Matrix |

### 7.3 Priorität NIEDRIG

| Feature | Aufwand | Nutzen | Umsetzungsidee |
|---------|---------|--------|-----------------|
| **Web-UI** | Hoch | Gering | Erst nach Windows-Build relevant |
| **Watch-Mode** | Mittel | Gering | FileObserver/WorkManager für Background-Sync |

---

## 8 Fazit

**Pharos** hat die bessere **Architektur und das stärkere Fundament** – modularer Aufbau, native Android-UI, LLM-Abstraktion, CI/CD. Mit Phase 2–4 hat Pharos die ursprünglichen Hauptschwächen (keine Tests, kein Datei-Scanner, kein Sync, nur Android) behoben.

**DocuPilot** hat den Vorteil der **plattformunabhängigen Python-CLI** und des integrierten **Google Drive Sync**. Es bleibt sofort nutzbar ohne Build-Schritt.

**Stand heute:** Pharos hat den Feature-Rückstand in den meisten Bereichen aufgeholt und übertrifft DocuPilot in Architektur, Tests, nativer UI und Windows-Desktop-Support. Verbleibende Lücken sind Cloud-Sync (Google Drive), LLM-Chat-Completions und Widerspruchs-Erkennung im UI.

---

*Erstellt: 2026-03-24 | Aktualisiert: 2026-03-26 | Chat-ID: CH-20260308-04*
