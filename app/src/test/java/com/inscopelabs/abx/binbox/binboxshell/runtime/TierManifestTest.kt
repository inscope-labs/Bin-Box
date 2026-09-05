package com.inscopelabs.abx.binbox.binboxshell.runtime

import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleState
import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleStateManager
import com.inscopelabs.abx.binbox.binboxshell.security.ShellSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TierManifestTest {

    @Test
    fun parseBaseManifest_parsesCorrectly() {
        val json = """
        {
          "tier": "base",
          "version": "1.0.0",
          "source": {
            "project": "BusyBox",
            "license": "GPL-2.0-only",
            "upstreamVersion": "1.36.1",
            "mirrorUrl": "https://github.com/mirror/busybox",
            "commit": "1a64f6a20aaf6ea4dbba68bbfa8cc1ab7e5c57c4"
          },
          "sharedBinaries": [
            {
              "id": "busybox",
              "nativeLibName": "libbusybox.so",
              "dependsOn": [],
              "sha256": {
                "arm64-v8a": "9b99db115c6788450471578b654f8ee8df0c1262dd4350e4263df653ed2927e4",
                "x86_64": "6c5f9f827316c30d9694e9af83d8f7dcf2c230c3142c86080cf544acfae07f29",
                "armeabi-v7a": "b0191a812b46c37b640a1a37b8c42f69ccc8c90388e7f873350a7f63dde324bd"
              }
            }
          ],
          "binaries": [
            { "name": "sh", "description": "POSIX shell", "category": "core", "sharedBinary": "busybox", "required": true },
            { "name": "ls", "description": "List dir", "category": "core", "sharedBinary": "busybox", "required": false }
          ]
        }
        """.trimIndent()

        val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val runtimePaths = RuntimePaths(mockContext)
        val registry = BinaryRegistry(mockContext, runtimePaths)

        val manifest = registry.parseManifest(ShellTier.BASE, json)
        assertEquals(ShellTier.BASE, manifest.tier)
        assertEquals("1.0.0", manifest.version)
        assertEquals("BusyBox", manifest.source?.project)
        assertEquals("1.36.1", manifest.source?.upstreamVersion)
        assertEquals(1, manifest.sharedBinaries.size)
        assertEquals("libbusybox.so", manifest.sharedBinaries[0].nativeLibName)
        assertEquals(3, manifest.sharedBinaries[0].sha256.size)
        assertEquals(2, manifest.binaries.size)
        assertEquals("sh", manifest.binaries[0].name)
        assertTrue(manifest.binaries[0].required)
        assertEquals("busybox", manifest.binaries[0].sharedBinary)
        assertFalse(manifest.binaries[1].required)
    }

    @Test
    fun parseStandardManifest_handlesSharedBinaryReference() {
        val json = """
        {
          "tier": "standard",
          "version": "1.0.0",
          "sharedBinaries": [
            {
              "id": "busybox-standard-delta",
              "nativeLibName": "libbusybox_standard.so",
              "dependsOn": [],
              "sha256": { "arm64-v8a": "abc123" }
            }
          ],
          "binaries": [
            { "name": "grep", "description": "Search text", "category": "text", "sharedBinary": "busybox-standard-delta", "required": false }
          ]
        }
        """.trimIndent()

        val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val runtimePaths = RuntimePaths(mockContext)
        val registry = BinaryRegistry(mockContext, runtimePaths)

        val manifest = registry.parseManifest(ShellTier.STANDARD, json)
        assertEquals("grep", manifest.binaries[0].name)
        assertEquals("busybox-standard-delta", manifest.binaries[0].sharedBinary)
        assertFalse(manifest.binaries[0].required)
        assertNull(manifest.source)
    }

    @Test
    fun resolveBinaryPath_returnsNullWhenSharedBinaryMissing() {
        val mockContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val runtimePaths = RuntimePaths(mockContext)
        val registry = BinaryRegistry(mockContext, runtimePaths)

        // No manifests loaded from real assets in this Robolectric context beyond
        // whatever ships in app/src/main/assets, and no on-disk native lib will
        // exist for a bogus name — resolution must fail closed, not throw.
        assertEquals(null, registry.resolveBinaryPath("definitely-not-a-real-binary-xyz"))
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
