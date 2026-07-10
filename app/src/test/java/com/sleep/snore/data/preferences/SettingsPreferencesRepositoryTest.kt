package com.sleep.snore.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val repository = createFixture().repository

        repository.setDynamicColorEnabled(true)
        repository.setAccentColor(AccentColor.GREEN)

        val settings = repository.settings.first()
        assertThat(settings.dynamicColorEnabled).isFalse()
        assertThat(settings.customAccentColorArgb).isEqualTo(AccentColor.GREEN.defaultArgb)
    }

    @Test
    fun setCustomAccentColorArgb_disablesDynamicColorAndKeepsAlpha() = runTest {
        val repository = createFixture().repository

        repository.setDynamicColorEnabled(true)
        repository.setCustomAccentColorArgb(0x00123456)

        val settings = repository.settings.first()
        assertThat(settings.dynamicColorEnabled).isFalse()
        assertThat(settings.customAccentColorArgb).isEqualTo(0xFF123456.toInt())
        assertThat(repository.accentColor.first()).isEqualTo(AccentColor.CUSTOM)
    }

    @Test
    fun setDeepSeekApiKey_storesEncryptedValueAndClearsPlaintext() = runTest {
        val fixture = createFixture()
        val repository = fixture.repository

        repository.setDeepSeekApiKey("  sk-test  ")

        val preferences = fixture.dataStore.data.first()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key")]).isNull()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key_encrypted")]).isEqualTo("enc:sk-test")
        assertThat(repository.settings.first().deepSeekApiKey).isEqualTo("sk-test")
    }

    @Test
    fun settings_readsLegacyPlaintextDeepSeekApiKey() = runTest {
        val fixture = createFixture()
        val repository = fixture.repository
        fixture.dataStore.editForTest("deepseek_api_key", "legacy-key")

        assertThat(repository.settings.first().deepSeekApiKey).isEqualTo("legacy-key")
        val preferences = fixture.dataStore.data.first()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key")]).isNull()
        assertThat(preferences[stringPreferencesKey("deepseek_api_key_encrypted")]).isEqualTo("enc:legacy-key")
    }

    private fun createFixture(): RepositoryFixture {
        val dataStoreFile = File.createTempFile("sleep-settings", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { dataStoreFile }
        )
        return RepositoryFixture(
            repository = SettingsPreferencesRepository(dataStore, FakeSecretTextCipher),
            dataStore = dataStore
        )
    }

    private suspend fun DataStore<Preferences>.editForTest(
        key: String,
        value: String
    ) {
        edit { preferences -> preferences[stringPreferencesKey(key)] = value }
    }

    private object FakeSecretTextCipher : SecretTextCipher {
        override fun encrypt(plainText: String): String = "enc:$plainText"
        override fun decrypt(cipherText: String): String? = cipherText.removePrefix("enc:")
    }

    private data class RepositoryFixture(
        val repository: SettingsPreferencesRepository,
        val dataStore: DataStore<Preferences>
    )
}
