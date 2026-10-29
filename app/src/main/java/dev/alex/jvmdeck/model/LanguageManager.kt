package dev.alex.jvmdeck.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

object LanguageManager {
    var currentLanguage by mutableStateOf("uk")
        private set

    fun setLanguage(lang: String) {
        if (lang in listOf("uk", "en")) {
            currentLanguage = lang
        }
    }

    fun getString(key: String, vararg args: Any): String {
        val map = AppStrings.strings[currentLanguage] ?: AppStrings.strings["uk"]!!
        val template = map[key] ?: AppStrings.strings["uk"]?.get(key) ?: key
        return if (args.isNotEmpty()) {
            runCatching { String.format(Locale.getDefault(), template, *args) }.getOrDefault(template)
        } else {
            template
        }
    }
}
