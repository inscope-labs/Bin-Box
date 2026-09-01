package com.inscopelabs.abx.binbox.binboxshell.runtime

import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleState
import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleStateManager
import com.inscopelabs.abx.binbox.binboxshell.security.ShellSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TierManifestTest {

    @Test
    fun parseBaseManifest_parsesCorrectly() {
        val json = """
        {
          "tier": "base",
          "version": "1.0.0",
          "description": "Base POSIX shell tools",
          "binaries": [
            { "name": "sh", "description": "POSIX shell", "category": "core", "fallbackSystemPath": "/system/bin/sh", "required": true },
            { "name": "ls", "description": "List dir", "category": "core", "fallbackSystemPath": "/system/bin/ls", "required": false }
          ]
        }
        """.trimIndent()

        val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val runtimePaths = RuntimePaths(mockContext)
        val registry = BinaryRegistry(mockContext, runtimePaths)

        val manifest = registry.parseManifest(ShellTier.BASE, json)
        assertEquals(ShellTier.BASE, manifest.tier)
        assertEquals("1.0.0", manifest.version)
        assertEquals(2, manifest.binaries.size)
        assertEquals("sh", manifest.binaries[0].name)
        assertTrue(manifest.binaries[0].required)
        assertEquals("/system/bin/sh", manifest.binaries[0].fallbackSystemPath)
    }

    @Test
    fun parseStandardAndExtendedManifest_handlesNativeLibNames() {
        val json = """
        {
          "tier": "standard",
          "version": "1.0.0",
          "description": "Standard productivity shell tools",
          "binaries": [
            { "name": "curl", "description": "Data transfer", "category": "network", "nativeLibName": "libcurl.so", "required": false }
          ]
        }
        """.trimIndent()

        val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val runtimePaths = RuntimePaths(mockContext)
        val registry = BinaryRegistry(mockContext, runtimePaths)

        val manifest = registry.parseManifest(ShellTier.STANDARD, json)
        assertEquals("libcurl.so", manifest.binaries[0].nativeLibName)
        assertFalse(manifest.binaries[0].required)
    }

    @Test
    fun shellSecurity_stripsRestrictedEnvironmentVariables() {
        val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val runtimePaths = RuntimePaths(mockContext)
        val security = ShellSecurity(runtimePaths)

        val inputEnv = mapOf(
            "PATH" to "/bin:/usr/bin",
            "LD_AUDIT" to "/evil/audit.so",
            "DYLD_INSERT_LIBRARIES" to "/evil/lib.so",
            "USER" to "testuser"
        )

        val sanitized = security.sanitizeEnvironment(inputEnv)
        assertEquals(2, sanitized.size)
        assertTrue(sanitized.containsKey("PATH"))
        assertTrue(sanitized.containsKey("USER"))
        assertFalse(sanitized.containsKey("LD_AUDIT"))
        assertFalse(sanitized.containsKey("DYLD_INSERT_LIBRARIES"))
    }

    @Test
    fun moduleStateManager_transitionsStatesCorrectly() {
        val manager = ModuleStateManager()
        assertEquals(ModuleState.Installed, manager.getState(ShellTier.BASE))
        assertEquals(ModuleState.Available, manager.getState(ShellTier.STANDARD))

        manager.updateState(ShellTier.STANDARD, ModuleState.Installing(0.5f))
        val state = manager.getState(ShellTier.STANDARD)
        assertTrue(state is ModuleState.Installing)
        assertEquals(0.5f, (state as ModuleState.Installing).progress, 0.01f)

        manager.updateState(ShellTier.STANDARD, ModuleState.Installed)
        assertTrue(manager.isInstalled(ShellTier.STANDARD))
    }
}
