package com.inscopelabs.abx.binbox.binboxshell.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleState
import com.inscopelabs.abx.binbox.binboxshell.runtime.BinaryDescriptor
import com.inscopelabs.abx.binbox.binboxshell.runtime.ShellTier
import com.inscopelabs.abx.binbox.ui.components.TierModuleCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TierModuleCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tierModuleCard_displaysTitleAndBinaries() {
        val binaries = listOf(
            BinaryDescriptor(name = "curl", description = "Data transfer", category = "network", sharedBinary = "test-shared"),
            BinaryDescriptor(name = "tar", description = "Archive tool", category = "archive", sharedBinary = "test-shared")
        )

        composeTestRule.setContent {
            TierModuleCard(
                tier = ShellTier.STANDARD,
                title = "Standard Productivity Pack",
                description = "Standard Linux utilities",
                binaries = binaries,
                state = ModuleState.Available,
                onInstallClick = {}
            )
        }

        composeTestRule.onNodeWithText("Standard Productivity Pack").assertExists()
        composeTestRule.onNodeWithText("Includes: curl, tar").assertExists()
        composeTestRule.onNodeWithText("Install").assertExists()
    }

    @Test
    fun tierModuleCard_displaysReady_whenInstalled() {
        val binaries = listOf(
            BinaryDescriptor(name = "sh", description = "POSIX Shell", category = "core", sharedBinary = "test-shared")
        )

        composeTestRule.setContent {
            TierModuleCard(
                tier = ShellTier.BASE,
                title = "Base POSIX Layer (Built-in)",
                description = "Core shell",
                binaries = binaries,
                state = ModuleState.Installed,
                onInstallClick = {}
            )
        }

        composeTestRule.onNodeWithText("Base POSIX Layer (Built-in)").assertExists()
        composeTestRule.onNodeWithText("Ready").assertExists()
    }
}
