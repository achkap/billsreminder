package gr.logariasmoi.data

import java.util.Locale

/**
 * Tiny two-language switch. Texts are written inline as tr("Ελληνικά", "English") so each
 * screen stays readable; the active language comes from Settings.language.
 */
object I18n {
    @Volatile
    var english: Boolean = false
        private set

    fun apply(language: String) {
        english = when (language) {
            "en" -> true
            "el" -> false
            else -> Locale.getDefault().language != "el"
        }
    }

    val locale: Locale get() = if (english) Locale.forLanguageTag("en-IE") else Locale.forLanguageTag("el-GR")
}

fun tr(el: String, en: String): String = if (I18n.english) en else el
