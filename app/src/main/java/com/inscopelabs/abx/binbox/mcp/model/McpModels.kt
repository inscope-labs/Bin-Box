package com.inscopelabs.abx.binbox.mcp.model

import kotlinx.serialization.Serializable

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class McpTool(
    val name: String,
    val description: String,
    val parametersJsonSchema: String = "{}"
)

@Serializable
data class McpResource(
    val uri: String,
    val name: String,
    val mimeType: String? = null
)

@Serializable
data class McpPrompt(
    val name: String,
    val description: String? = null
)

sealed class McpSessionState {
    data object Disconnected : McpSessionState()
    data object Connecting : McpSessionState()
    data class Connected(val serverInfo: McpServerInfo, val toolsCount: Int) : McpSessionState()
    data class Error(val message: String) : McpSessionState()
}
