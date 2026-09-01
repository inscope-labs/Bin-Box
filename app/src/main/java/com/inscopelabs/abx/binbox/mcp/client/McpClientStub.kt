package com.inscopelabs.abx.binbox.mcp.client

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.mcp.model.McpPrompt
import com.inscopelabs.abx.binbox.mcp.model.McpResource
import com.inscopelabs.abx.binbox.mcp.model.McpServerInfo
import com.inscopelabs.abx.binbox.mcp.model.McpSessionState
import com.inscopelabs.abx.binbox.mcp.model.McpTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class McpClientStub : McpClient {
    private val tag = "McpClientStub"
    private val _state = MutableStateFlow<McpSessionState>(McpSessionState.Disconnected)
    override val state: StateFlow<McpSessionState> = _state.asStateFlow()

    private val stubTools = listOf(
        McpTool("terminal_exec", "Execute command in the active BinBox terminal session"),
        McpTool("read_file", "Read a file from the local container sandbox"),
        McpTool("list_directory", "List files in the local container directory")
    )

    override suspend fun connect(endpoint: String): Result<McpServerInfo> {
        BinBoxLogger.i(tag, "Connecting to MCP endpoint: $endpoint")
        _state.value = McpSessionState.Connecting
        return try {
            val serverInfo = McpServerInfo(
                name = "BinBox-Local-MCP-Server",
                version = "0.1.0"
            )
            _state.value = McpSessionState.Connected(serverInfo, stubTools.size)
            BinBoxLogger.i(tag, "Successfully connected to MCP server: ${serverInfo.name}")
            Result.success(serverInfo)
        } catch (e: Exception) {
            BinBoxLogger.e(tag, "Failed to connect to MCP server", e)
            _state.value = McpSessionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        BinBoxLogger.i(tag, "Disconnecting MCP client")
        _state.value = McpSessionState.Disconnected
    }

    override suspend fun listTools(): Result<List<McpTool>> {
        BinBoxLogger.d(tag, "Listing MCP tools (${stubTools.size} available)")
        return Result.success(stubTools)
    }

    override suspend fun callTool(name: String, arguments: Map<String, String>): Result<String> {
        BinBoxLogger.i(tag, "Calling MCP tool: $name with args: $arguments")
        return Result.success("Tool '$name' executed successfully (Stub response)")
    }

    override suspend fun listResources(): Result<List<McpResource>> {
        BinBoxLogger.d(tag, "Listing MCP resources")
        return Result.success(emptyList())
    }

    override suspend fun listPrompts(): Result<List<McpPrompt>> {
        BinBoxLogger.d(tag, "Listing MCP prompts")
        return Result.success(emptyList())
    }
}
