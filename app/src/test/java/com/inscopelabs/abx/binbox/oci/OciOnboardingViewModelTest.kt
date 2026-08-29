package com.inscopelabs.abx.binbox.oci

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.oci.identity.OciCredentialsStore
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningRepository
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState
import com.inscopelabs.abx.binbox.oci.wizard.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OciOnboardingViewModelTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        OciProvisioningRepository(application).clear()
        OciCredentialsStore(application, com.inscopelabs.abx.binbox.security.SecureStorageService(application)).clear()
    }

    @After
    fun tearDown() {
        OciProvisioningRepository(application).clear()
        OciCredentialsStore(application, com.inscopelabs.abx.binbox.security.SecureStorageService(application)).clear()
    }

    @Test
    fun testConstructorWithSingleApplicationParameterExists() {
        val constructor = OciOnboardingViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull("Constructor taking Application must exist for ViewModelProvider", constructor)
        val instance = constructor.newInstance(application)
        assertNotNull("Instance created via Application constructor must not be null", instance)
    }

    @Test
    fun testViewModelProviderFactoryInstantiation() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)
        assertNotNull("ViewModel created via AndroidViewModelFactory must not be null", viewModel)
        assertEquals(OciOnboardingStage.WELCOME, viewModel.stage.value)
    }

    @Test
    fun testGetStartedAdvancesStage() {
        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)
        viewModel.onEvent(OciOnboardingEvent.GetStarted)
        assertEquals(OciOnboardingStage.ACCOUNT_INFORMATION, viewModel.stage.value)
    }

    @Test
    fun testNavigationBackAndStartOver() {
        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)

        viewModel.onEvent(OciOnboardingEvent.GetStarted)
        assertEquals(OciOnboardingStage.ACCOUNT_INFORMATION, viewModel.stage.value)

        viewModel.onEvent(OciOnboardingEvent.SubmitAccountInfo(
            tenancyOcid = "ocid1.tenancy.oc1..aaaaaaaaompgipsy3qst3b3ecscsgr73ov6fvzxb72ozjgq4lmcexeizbrwq",
            userOcid = "ocid1.user.oc1..aaaaaaaazcbems22m5rrebu3zhpzegqgxplr7gsrfcppgmojxdgmqjwfihxq",
            region = "sa-saopaulo-1"
        ))
        assertEquals(OciOnboardingStage.API_KEY_GENERATION, viewModel.stage.value)

        // Go Back to ACCOUNT_INFORMATION
        viewModel.onEvent(OciOnboardingEvent.GoBack)
        assertEquals(OciOnboardingStage.ACCOUNT_INFORMATION, viewModel.stage.value)

        // Go Back to WELCOME
        viewModel.onEvent(OciOnboardingEvent.GoBack)
        assertEquals(OciOnboardingStage.WELCOME, viewModel.stage.value)

        // Advance again and test StartOver
        viewModel.onEvent(OciOnboardingEvent.GetStarted)
        viewModel.onEvent(OciOnboardingEvent.StartOver)
        assertEquals(OciOnboardingStage.WELCOME, viewModel.stage.value)
    }

    @Test
    fun testContinueButtonAdvancesPastApiKeyGenerationOnReturn() {
        // Regression test for the step-2 dead end: navigating back to API_KEY_GENERATION after
        // a key already exists must not strand the user with no way forward. The manual
        // ContinueToKeyRegistration event (wired to the stage's new "Continue" button) must
        // advance the wizard regardless of how the stage was reached.
        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)

        viewModel.onEvent(OciOnboardingEvent.GetStarted)
        viewModel.onEvent(OciOnboardingEvent.SubmitAccountInfo(
            tenancyOcid = "ocid1.tenancy.oc1..aaaaaaaaompgipsy3qst3b3ecscsgr73ov6fvzxb72ozjgq4lmcexeizbrwq",
            userOcid = "ocid1.user.oc1..aaaaaaaazcbems22m5rrebu3zhpzegqgxplr7gsrfcppgmojxdgmqjwfihxq",
            region = "sa-saopaulo-1"
        ))
        assertEquals(OciOnboardingStage.API_KEY_GENERATION, viewModel.stage.value)

        viewModel.onEvent(OciOnboardingEvent.ContinueToKeyRegistration)
        assertEquals(OciOnboardingStage.API_KEY_REGISTRATION, viewModel.stage.value)
    }

    @Test
    fun testResumePromptShownForPersistedInProgressSession() {
        val repo = OciProvisioningRepository(application)
        val now = System.currentTimeMillis()
        val saveRes = repo.save(
            OciProvisioningSession(
                sessionId = UUID.randomUUID().toString(),
                state = OciProvisioningState.CONTEXT_DISCOVERED,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        val loadRes = repo.load()
        assertTrue("Save must succeed: $saveRes", saveRes is com.inscopelabs.abx.binbox.core.result.AppResult.Success)
        assertTrue("Load must succeed: $loadRes", loadRes is com.inscopelabs.abx.binbox.core.result.AppResult.Success)
        assertEquals(OciProvisioningState.CONTEXT_DISCOVERED, (loadRes as com.inscopelabs.abx.binbox.core.result.AppResult.Success).data?.state)

        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)

        assertEquals(OciOnboardingStage.HOST_CONFIGURATION, viewModel.uiState.value.pendingResumeStage)
        // Stage navigation itself must stay put (WELCOME) until the user chooses.
        assertEquals(OciOnboardingStage.WELCOME, viewModel.stage.value)
    }

    @Test
    fun testStartOverFromResumePromptDiscardsSessionAndClearsPrompt() {
        val now = System.currentTimeMillis()
        OciProvisioningRepository(application).save(
            OciProvisioningSession(
                sessionId = UUID.randomUUID().toString(),
                state = OciProvisioningState.CONTEXT_DISCOVERED,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )

        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)
        assertNotNull(viewModel.uiState.value.pendingResumeStage)

        viewModel.onEvent(OciOnboardingEvent.StartOver)

        assertNull(viewModel.uiState.value.pendingResumeStage)
        assertEquals(OciOnboardingStage.WELCOME, viewModel.stage.value)
    }

    @Test
    fun testOciConfigParserWithUserFeedbackSnippet() {
        val snippet = """
            [DEFAULT]
            user=ocid1.user.oc1..aaaaaaaazcbems22m5rrebu3zhpzegqgxplr7gsrfcppgmojxdgmqjwfihxq
            fingerprint=8c:00:4d:b0:7e:87:43:a2:dc:d0:2c:ac:b2:42:48:5a
            tenancy=ocid1.tenancy.oc1..aaaaaaaaompgipsy3qst3b3ecscsgr73ov6fvzxb72ozjgq4lmcexeizbrwq
            region=sa-saopaulo-1
            key_file=<path to your private keyfile> # TODO
        """.trimIndent()

        val parsed = OciConfigParser.parse(snippet)
        assertEquals("ocid1.user.oc1..aaaaaaaazcbems22m5rrebu3zhpzegqgxplr7gsrfcppgmojxdgmqjwfihxq", parsed.userOcid)
        assertEquals("ocid1.tenancy.oc1..aaaaaaaaompgipsy3qst3b3ecscsgr73ov6fvzxb72ozjgq4lmcexeizbrwq", parsed.tenancyOcid)
        assertEquals("8c:00:4d:b0:7e:87:43:a2:dc:d0:2c:ac:b2:42:48:5a", parsed.fingerprint)
        assertEquals("sa-saopaulo-1", parsed.region)

        val factory = ViewModelProvider.AndroidViewModelFactory(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)
        viewModel.onEvent(OciOnboardingEvent.ImportConfig(snippet))

        val state = viewModel.uiState.value
        assertEquals("ocid1.user.oc1..aaaaaaaazcbems22m5rrebu3zhpzegqgxplr7gsrfcppgmojxdgmqjwfihxq", state.userOcid)
        assertEquals("ocid1.tenancy.oc1..aaaaaaaaompgipsy3qst3b3ecscsgr73ov6fvzxb72ozjgq4lmcexeizbrwq", state.tenancyOcid)
        assertEquals("sa-saopaulo-1", state.region)
        assertEquals("8c:00:4d:b0:7e:87:43:a2:dc:d0:2c:ac:b2:42:48:5a", state.pendingFingerprint)
    }

    @Test
    fun testRegionNormalization() {
        assertEquals("sa-saopaulo-1", OciRegionHelper.normalizeRegion("Brazil East (Sao Paulo)"))
        assertEquals("sa-saopaulo-1", OciRegionHelper.normalizeRegion("sa-saopaulo-1"))
        assertEquals("us-ashburn-1", OciRegionHelper.normalizeRegion("US East (Ashburn)"))
        assertEquals("us-ashburn-1", OciRegionHelper.normalizeRegion("us-ashburn-1"))
    }

    @Test
    fun testVerificationDiagnosticsReportFormatting() {
        val diagnostics = OciVerificationDiagnostics(
            endpointUrl = "https://identity.sa-saopaulo-1.oraclecloud.com/20160918/users/ocid1.user.oc1...",
            region = "sa-saopaulo-1",
            tenancyOcid = "ocid1.tenancy.oc1..test",
            userOcid = "ocid1.user.oc1..test",
            fingerprint = "8c:00:4d:b0:7e:87:43:a2:dc:d0:2c:ac:b2:42:48:5a",
            httpStatusCode = 401,
            ociErrorCode = "NotAuthenticated",
            ociErrorMessage = "The required information to complete authentication was not provided or was incorrect"
        )
        val report = diagnostics.toFormattedReport()
        assertTrue(report.contains("Endpoint: GET https://identity.sa-saopaulo-1.oraclecloud.com/20160918/users/ocid1.user.oc1..."))
        assertTrue(report.contains("HTTP Status: 401"))
        assertTrue(report.contains("Troubleshooting Guidance"))
        assertTrue(diagnostics.isAuthError)
    }
}
