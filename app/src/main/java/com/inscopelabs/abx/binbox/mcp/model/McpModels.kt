package com.inscopelabs.abx.binbox.mcp.model

data class McpServerInfo(
    val name: String,
    val version: String
)

data class McpTool(
    val name: String,
    val description: String,
    val parametersJsonSchema: String = "{}"
)

data class McpResource(
    val uri: String,
    val name: String,
    val mimeType: String? = null
)

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

