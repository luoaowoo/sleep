package com.sleep.snore.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.sleep.snore.data.model.AccentColor
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPreferencesRepositoryTest {

    @Test
    fun setAccentColor_disablesDynamicColorAndStoresPresetArgb() = runTest {
        val repository = createRepository()

        repository.setDynamicColorEnabled(true)
        repository.setAccentColor(AccentColor.GREEN)

        val settings = repository.settings.first()
        assertThat(settings.dynamicColorEnabled).isFalse()
        assertThat(settings.customAccentColorArgb).isEqualTo(AccentColor.GREEN.defaultArgb)
    }

    @Test
    fun setCustomAccentColorArgb_disablesDynamicColorAndKeepsAlpha() = runTest {
        val repository = createRepository()

        repository.setDynamicColorEnabled(true)
        repository.setCustomAccentColorArgb(0x00123456)

        val settings = repository.settings.first()
        assertThat(settings.dynamicColorEnabled).isFalse()
        assertThat(settings.customAccentColorArgb).isEqualTo(0xFF123456.toInt())
        assertThat(repository.accentColor.first()).isEqualTo(AccentColor.CUSTOM)
    }

    private fun createRepository(): SettingsPreferencesRepository {
        val dataStoreFile = File.createTempFile("sleep-settings", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { dataStoreFile }
        )
        return SettingsPreferencesRepository(dataStore)
    }
}
