package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.runtime.compositionLocalOf
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * CompositionLocal providing a globally accessible lambda to launch the
 * Oracle Cloud (OCI) onboarding and provisioning wizard from any UI layer.
 */
val LocalOciWizardLauncher = compositionLocalOf<() -> Unit> {
    {
        BinBoxLogger.w("LocalOciWizardLauncher", "No OCI Wizard launcher provided in current composition hierarchy")
    }
}
