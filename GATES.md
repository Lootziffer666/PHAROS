# 🚪 GATES — PHAROS

---

## 🔜 Nächste Gates

### Gate PH-011: OCR Pipeline
- **Branch:** `gate/ph-011-ocr`
- **To-Dos:**
  - [ ] ML Kit on-device OCR
  - [ ] PDF-Text-Extraktion
  - [ ] Bild-zu-Text
  - [ ] OCR-Result in Masterfile integrieren
- **Akzeptanz:** Gescannte Dokumente analysierbar
- **Kill:** Cloud-OCR-Dependency

### Gate PH-012: Smart Clustering v2
- **Branch:** `gate/ph-012-clustering-v2`
- **To-Dos:**
  - [ ] Embedding-basiertes Clustering
  - [ ] Cluster-Merge/Split-UI
  - [ ] Auto-Labeling per LLM
  - [ ] Cluster-Qualitäts-Score
- **Akzeptanz:** Cluster besser als Ordner-Zuordnung
- **Kill:** Clustering nur per Dateiname

### Gate PH-013: Desktop Sync v2
- **Branch:** `gate/ph-013-desktop-sync`
- **To-Dos:**
  - [ ] Bidirektionaler Sync
  - [ ] Conflict Resolution UI
  - [ ] Selective Sync (Ordner wählen)
  - [ ] Sync-History / Undo
- **Akzeptanz:** Sync in beide Richtungen korrekt
- **Kill:** Daten-Verlust bei Sync

### Gate PH-014: Provenance Chain
- **Branch:** `gate/ph-014-provenance`
- **To-Dos:**
  - [ ] SHA-256 Provenance Trail pro Dokument
  - [ ] Timestamp-basierte Versionshistorie
  - [ ] Tamper-Detection
  - [ ] Provenance-Report exportierbar
- **Akzeptanz:** Herkunft jedes Dokuments nachvollziehbar
- **Kill:** Provenance ohne Timestamp
