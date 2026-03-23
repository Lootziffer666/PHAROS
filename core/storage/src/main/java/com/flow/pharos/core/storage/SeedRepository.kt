package com.flow.pharos.core.storage

import com.flow.pharos.core.model.ArtifactRecord
import com.flow.pharos.core.model.PharosIndex
import com.flow.pharos.core.model.RelationEdge

class SeedRepository {
    fun archive() = PharosIndex(
        artifacts = listOf(
            ArtifactRecord(
                id = "SSOT-20260306-01",
                title = "CURRENT_SSOT",
                status = "CANONICAL",
                timestamp = "2026-03-06",
                path = "00_CANON/CURRENT_SSOT.md",
                tags = listOf("canon", "system"),
                summary = "Canonical ecosystem state.",
            ),
            ArtifactRecord(
                id = "PHAROS-REL-01",
                title = "PHAROS_RELATION_SCHEMA",
                status = "CANONICAL",
                timestamp = "2026-03-06",
                path = "00_CANON/PHAROS_RELATION_SCHEMA.md",
                tags = listOf("canon", "relations"),
                summary = "Relation schema.",
            ),
            ArtifactRecord(
                id = "ENTRY-20260306-01",
                title = "ENTRYPOINT_NEXT_CHAT",
                status = "ACTIVE",
                timestamp = "2026-03-06",
                path = "ENTRYPOINT_NEXT_CHAT.md",
                tags = listOf("handoff"),
                summary = "Continuation entrypoint.",
            ),
        ),
        relations = listOf(
            RelationEdge(
                fromId = "PHAROS-REL-01",
                toId = "SSOT-20260306-01",
                type = "DOCUMENTS",
                note = "Relation schema describes canon.",
            ),
            RelationEdge(
                fromId = "ENTRY-20260306-01",
                toId = "SSOT-20260306-01",
                type = "RELATED_TO",
                note = "Next chat handoff references canonical state.",
            ),
        ),
    )
}

