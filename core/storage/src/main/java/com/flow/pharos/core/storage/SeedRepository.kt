package com.flow.pharos.core.storage

import com.flow.pharos.core.model.*

class SeedRepository {
  fun archive() = PharosIndex(
    artifacts=listOf(
      ArtifactRecord("SSOT-20260306-01","CURRENT_SSOT","CANONICAL","2026-03-06","00_CANON/CURRENT_SSOT.md", listOf("canon","system"),"Canonical ecosystem state."),
      ArtifactRecord("PHAROS-REL-01","PHAROS_RELATION_SCHEMA","CANONICAL","2026-03-06","00_CANON/PHAROS_RELATION_SCHEMA.md", listOf("canon","relations"),"Relation schema."),
      ArtifactRecord("ENTRY-20260306-01","ENTRYPOINT_NEXT_CHAT","ACTIVE","2026-03-06","ENTRYPOINT_NEXT_CHAT.md", listOf("handoff"),"Continuation entrypoint.")
    ),
    relations=listOf(
      RelationEdge("PHAROS-REL-01","SSOT-20260306-01","DOCUMENTS","Relation schema describes canon."),
      RelationEdge("ENTRY-20260306-01","SSOT-20260306-01","RELATED_TO","Next chat handoff references canonical state.")
    )
  )
}
