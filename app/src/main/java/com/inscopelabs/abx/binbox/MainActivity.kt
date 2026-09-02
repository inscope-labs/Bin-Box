package com.inscopelabs.abx.binbox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.oci.management.OciProvisioningStatus
import com.inscopelabs.abx.binbox.oci.wizard.LocalOciWizardLauncher
import com.inscopelabs.abx.binbox.ui.BinBoxApp
import com.inscopelabs.abx.binbox.ui.i18n.LocalAppStrings
import com.inscopelabs.abx.binbox.ui.theme.BinBoxTheme
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BinBoxLogger.i("MainActivity", "onCreate: initializing BinBox application")
        enableEdgeToEdge()
        setContent {
            val viewModel: BinBoxViewModel = viewModel()
            val strings by viewModel.strings.collectAsStateWithLifecycle()
            val hosts by viewModel.hosts.collectAsStateWithLifecycle()
            var showOciWizard by remember { mutableStateOf(false) }
            var showOciManagement by remember { mutableStateOf(false) }
            val hasCompletedOciProvisioning = OciProvisioningStatus.hasCompletedProvisioning(hosts)

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                BinBoxLogger.i("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        BinBoxLogger.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                // Every OCI entry point (FAB, promo card, quick-action tile, settings/terminal
                // launchers) calls this same lambda — routing to management once a real
                // provisioned host exists is decided once, here, rather than at each call site.
                LocalOciWizardLauncher provides {
                    if (hasCompletedOciProvisioning) {
                        BinBoxLogger.d("MainActivity", "Launching OCI Management Screen")
                        showOciManagement = true
                    } else {
                        BinBoxLogger.d("MainActivity", "Launching OCI Onboarding Wizard")
                        showOciWizard = true
                    }
                }
            ) {
                BinBoxTheme {
                    BinBoxApp(
                        viewModel = viewModel,
                        hosts = hosts,
                        showOciWizard = showOciWizard,
                        onSetShowOciWizard = { showOciWizard = it },
                        showOciManagement = showOciManagement,
                        onSetShowOciManagement = { showOciManagement = it }
                    )
                }
            }
        }
    }
}
