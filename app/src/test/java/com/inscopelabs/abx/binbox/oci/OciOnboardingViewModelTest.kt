package com.inscopelabs.abx.binbox.oci

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingEvent
import com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingStage
import com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OciOnboardingViewModelTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testConstructorWithSingleApplicationParameterExists() {
        // Verifies the 1-argument (Application) constructor is present and invokable via reflection
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
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        val viewModel = factory.create(OciOnboardingViewModel::class.java)
        viewModel.onEvent(OciOnboardingEvent.GetStarted)
        assertEquals(OciOnboardingStage.ACCOUNT_INFORMATION, viewModel.stage.value)
    }
}
