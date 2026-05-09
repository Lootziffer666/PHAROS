<h1 align="center">🏛 PHAROS</h1>

<p align="center">
  <strong>Dual-Platform Document Intelligence — Android & Windows Desktop</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-Kotlin%20|%20Compose-green?style=flat-square" />
  <img src="https://img.shields.io/badge/Desktop-Compose%20for%20Desktop-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/Core-Pure%20Kotlin%2FJVM-purple?style=flat-square" />
  <img src="https://img.shields.io/badge/LLM-Ollama%20|%20OpenAI%20Compat-orange?style=flat-square" />
</p>

---

## 🧭 Was ist Pharos?

**Android:** Scannt lokale Ordner, analysiert mit LLM, clustert in Projekte, schreibt Markdown-Masterfiles. Hybrid: leicht auf Phone, schwer auf PC-LLM.

**Desktop (Windows):** Ordner picken, SHA-256 Manifests, Diff-View, Sync ohne Cloud.

---

## 🏗 Module

```
Pharos/
├── app/              # Android (Compose, Hilt, Material 3)
├── desktop/          # Windows (Compose for Desktop)
├── core/
│   ├── sync/         # ✦ Shared: SHA-256 Manifest Sync
│   ├── truth/        # ✦ Shared: Provenance, Trust
│   ├── model/        # Android: Room Entities
│   ├── storage/      # Android: Room DB, DAOs
│   └── llm/          # Android: LLM Gateway
└── provider/
    ├── perplexity/   # Perplexity API
    ├── ollama/       # Local LLM
    └── customopenai/ # Custom Gateway
```

`✦` = Plattformagnostisch, geteilt zwischen Android & Desktop

---

## 🚪 Gates

Siehe [`GATES.md`](GATES.md).

---

## 🔗 Relevante Projekte

| Projekt | Relevanz |
|---------|----------|
| [ollama/ollama](https://github.com/ollama/ollama) | Lokaler LLM-Provider |
| [nomic-ai/gpt4all](https://github.com/nomic-ai/gpt4all) | Lokale LLM-Alternative |
| [mudler/LocalAI](https://github.com/mudler/LocalAI) | Self-hosted AI Backend |

---

<p align="center"><em>Scan. Cluster. Sync. Kein Cloud.</em></p>
