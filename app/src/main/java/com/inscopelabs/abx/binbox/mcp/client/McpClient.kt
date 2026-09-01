package com.inscopelabs.abx.binbox.mcp.client

import com.inscopelabs.abx.binbox.mcp.model.McpPrompt
import com.inscopelabs.abx.binbox.mcp.model.McpResource
import com.inscopelabs.abx.binbox.mcp.model.McpServerInfo
import com.inscopelabs.abx.binbox.mcp.model.McpSessionState
import com.inscopelabs.abx.binbox.mcp.model.McpTool
import kotlinx.coroutines.flow.StateFlow

interface McpClient {
    val state: StateFlow<McpSessionState>

    suspend fun connect(endpoint: String): Result<McpServerInfo>
    suspend fun disconnect()
    suspend fun listTools(): Result<List<McpTool>>
    suspend fun callTool(name: String, arguments: Map<String, String>): Result<String>
    suspend fun listResources(): Result<List<McpResource>>
    suspend fun listPrompts(): Result<List<McpPrompt>>
}
