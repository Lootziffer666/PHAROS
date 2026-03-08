package com.flow.pharos.core.llm

interface LlmGateway { suspend fun ping(): Result<String> }
