package com.max.aiassistant.data.local

import com.max.aiassistant.data.preferences.DEFAULT_SHARED_SYSTEM_PROMPT

enum class OnDeviceModelVariant(
    val storageFileName: String,
    val downloadUrl: String,
    val expectedSha256: String,
    val displayName: String
) {
    GEMMA_4_E2B(
        storageFileName = "gemma-4-E2B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
        expectedSha256 = "AB7838CDFC8F77E54D8CA45EADCEB20452D9F01E4BFADE03E5DCE27911B27E42",
        displayName = "Gemma 4 E2B"
    ),
    GEMMA_4_E4B(
        storageFileName = "gemma-4-E4B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true",
        expectedSha256 = "F335F2BFD1B758DC6476DB16C0F41854BD6237E2658D604CBE566BCEFD00A7BC",
        displayName = "Gemma 4 E4B"
    );

    companion object {
        fun fromStorageName(value: String?): OnDeviceModelVariant {
            return entries.firstOrNull { it.storageFileName == value } ?: DEFAULT_MODEL_VARIANT
        }
    }
}

data class OnDeviceAiSettings(
    val modelVariant: OnDeviceModelVariant = DEFAULT_MODEL_VARIANT,
    val maxContextTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
    val systemPrompt: String = DEFAULT_SHARED_SYSTEM_PROMPT
)

val SUPPORTED_MAX_CONTEXT_TOKENS = listOf(2048, 4096, 8192, 16_384, 32_768, 65_536)
const val DEFAULT_MAX_CONTEXT_TOKENS = 4096
val DEFAULT_MODEL_VARIANT = OnDeviceModelVariant.GEMMA_4_E2B
