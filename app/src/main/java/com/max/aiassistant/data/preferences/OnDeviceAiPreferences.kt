package com.max.aiassistant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.max.aiassistant.data.local.DEFAULT_MAX_CONTEXT_TOKENS
import com.max.aiassistant.data.local.DEFAULT_MODEL_VARIANT
import com.max.aiassistant.data.local.OnDeviceAiSettings
import com.max.aiassistant.data.local.OnDeviceModelVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onDeviceAiDataStore by preferencesDataStore(name = "on_device_ai_prefs")

class OnDeviceAiPreferences(private val context: Context) {

    private val MODEL_VARIANT_KEY = stringPreferencesKey("model_variant")
    private val MAX_CONTEXT_TOKENS_KEY = intPreferencesKey("max_context_tokens")
    private val SYSTEM_PROMPT_KEY = stringPreferencesKey("system_prompt")

    val settings: Flow<OnDeviceAiSettings> = context.onDeviceAiDataStore.data
        .map { prefs ->
            OnDeviceAiSettings(
                modelVariant = OnDeviceModelVariant.fromStorageName(prefs[MODEL_VARIANT_KEY]),
                maxContextTokens = prefs[MAX_CONTEXT_TOKENS_KEY] ?: DEFAULT_MAX_CONTEXT_TOKENS,
                systemPrompt = prefs[SYSTEM_PROMPT_KEY] ?: DEFAULT_SHARED_SYSTEM_PROMPT
            )
        }

    suspend fun save(settings: OnDeviceAiSettings) {
        context.onDeviceAiDataStore.edit { prefs ->
            prefs[MODEL_VARIANT_KEY] = settings.modelVariant.storageFileName
            prefs[MAX_CONTEXT_TOKENS_KEY] = settings.maxContextTokens
            prefs[SYSTEM_PROMPT_KEY] = settings.systemPrompt
        }
    }

    companion object {
        val DEFAULT_SETTINGS = OnDeviceAiSettings(
            modelVariant = DEFAULT_MODEL_VARIANT,
            maxContextTokens = DEFAULT_MAX_CONTEXT_TOKENS,
            systemPrompt = DEFAULT_SHARED_SYSTEM_PROMPT
        )
    }
}
