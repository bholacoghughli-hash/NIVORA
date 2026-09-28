package com.nivora.browser.translation

import android.webkit.WebView
import java.util.Locale

data class Language(val code: String, val displayName: String)

object TranslationEngine {

    val supportedLanguages = listOf(
        Language("en", "English"),
        Language("es", "Spanish (Español)"),
        Language("fr", "French (Français)"),
        Language("de", "German (Deutsch)"),
        Language("hi", "Hindi (हिन्दी)"),
        Language("ja", "Japanese (日本語)"),
        Language("zh-CN", "Chinese (Simplified)"),
        Language("ar", "Arabic (العربية)"),
        Language("pt", "Portuguese (Português)"),
        Language("ru", "Russian (Русский)"),
        Language("it", "Italian (Italiano)"),
        Language("ko", "Korean (한국어)")
    )

    fun detectLanguage(sampleText: String): Language {
        // Fast script/character heuristic
        val hasHindi = sampleText.any { it in '\u0900'..'\u097F' }
        val hasJapanese = sampleText.any { it in '\u3040'..'\u30FF' }
        val hasChinese = sampleText.any { it in '\u4E00'..'\u9FFF' }
        val hasArabic = sampleText.any { it in '\u0600'..'\u06FF' }
        val hasCyrillic = sampleText.any { it in '\u0400'..'\u04FF' }

        return when {
            hasHindi -> supportedLanguages.first { it.code == "hi" }
            hasJapanese -> supportedLanguages.first { it.code == "ja" }
            hasChinese -> supportedLanguages.first { it.code == "zh-CN" }
            hasArabic -> supportedLanguages.first { it.code == "ar" }
            hasCyrillic -> supportedLanguages.first { it.code == "ru" }
            else -> supportedLanguages.first { it.code == "en" }
        }
    }

    /**
     * Injects client-side translation widget into the current WebView document
     */
    fun translateWebPage(webView: WebView, targetLanguageCode: String) {
        val js = """
            (function() {
                var existingScript = document.getElementById('google-translate-script');
                if (!existingScript) {
                    var script = document.createElement('script');
                    script.id = 'google-translate-script';
                    script.type = 'text/javascript';
                    script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                    document.body.appendChild(script);
                    
                    window.googleTranslateElementInit = function() {
                        new google.translate.TranslateElement({
                            pageLanguage: 'auto',
                            includedLanguages: '$targetLanguageCode',
                            layout: google.translate.TranslateElement.InlineLayout.SIMPLE
                        }, 'google_translate_element');
                    };
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun restoreOriginal(webView: WebView) {
        webView.reload()
    }
}
