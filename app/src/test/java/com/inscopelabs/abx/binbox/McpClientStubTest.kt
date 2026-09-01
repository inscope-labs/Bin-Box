package com.inscopelabs.abx.binbox

import com.inscopelabs.abx.binbox.mcp.client.McpClientStub
import com.inscopelabs.abx.binbox.mcp.model.McpSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class McpClientStubTest {

    @Test
    fun testMcpClientConnectsAndListsTools() = runBlocking {
        val client = McpClientStub()
        assertEquals(McpSessionState.Disconnected, client.state.value)

        val connectResult = client.connect("local://binbox/mcp")
        assertTrue(connectResult.isSuccess)

        val serverInfo = connectResult.getOrThrow()
        assertEquals("BinBox-Local-MCP-Server", serverInfo.name)

        val toolsResult = client.listTools()
        assertTrue(toolsResult.isSuccess)
        val tools = toolsResult.getOrThrow()
        assertTrue(tools.isNotEmpty())
        assertTrue(tools.any { it.name == "terminal_exec" })
    }
}
