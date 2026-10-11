package com.example.data.model

import java.util.Locale

data class AppLanguage(
    val code: String,          // e.g. "fr-FR", "en-US", "es-ES"
    val displayName: String,   // e.g. "Français", "English"
    val flagEmoji: String,     // e.g. "🇫🇷", "🇺🇸"
    val locale: Locale,
    val defaultGreeting: String
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            AppLanguage(
                code = "fr-FR",
                displayName = "Français",
                flagEmoji = "🇫🇷",
                locale = Locale.FRANCE,
                defaultGreeting = "Salut mon ami ! Je suis Lilian, ton ami omniscient et compagnon personnel qui vit sur ton téléphone. De quoi as-tu envie de discuter aujourd'hui ?"
            ),
            AppLanguage(
                code = "en-US",
                displayName = "English",
                flagEmoji = "🇺🇸",
                locale = Locale.US,
                defaultGreeting = "Hey my friend! I am Lilian, your omniscient friend and companion living on your phone. What would you like to talk about today?"
            ),
            AppLanguage(
                code = "es-ES",
                displayName = "Español",
                flagEmoji = "🇪🇸",
                locale = Locale("es", "ES"),
                defaultGreeting = "¡Hola amigo! Soy Lilian, tu amigo omnisciente y compañero personal en tu teléfono. ¿De qué te gustaría hablar hoy?"
            ),
            AppLanguage(
                code = "zh-CN",
                displayName = "中文",
                flagEmoji = "🇨🇳",
                locale = Locale.CHINA,
                defaultGreeting = "你好朋友！我是Lilian，你的全知好友与手机私人语音伙伴。今天想和我聊些什么？"
            ),
            AppLanguage(
                code = "ja-JP",
                displayName = "日本語",
                flagEmoji = "🇯🇵",
                locale = Locale.JAPAN,
                defaultGreeting = "こんにちは！私はLilian、あなたのスマホにいる物知りな友達です。今日は何について話しましょうか？"
            ),
            AppLanguage(
                code = "de-DE",
                displayName = "Deutsch",
                flagEmoji = "🇩🇪",
                locale = Locale.GERMANY,
                defaultGreeting = "Hallo mein Freund! Ich bin Lilian, dein allwissender Freund und Sprachbegleiter auf deinem Handy. Worüber möchtest du heute sprechen?"
            ),
            AppLanguage(
                code = "it-IT",
                displayName = "Italiano",
                flagEmoji = "🇮🇹",
                locale = Locale.ITALY,
                defaultGreeting = "Ciao amico mio! Sono Lilian, il tuo amico onnisciente e compagno personale sul tuo smartphone. Di cosa vorresti parlare oggi?"
            ),
            AppLanguage(
                code = "pt-PT",
                displayName = "Português",
                flagEmoji = "🇵🇹",
                locale = Locale("pt", "PT"),
                defaultGreeting = "Olá amigo! Sou o Lilian, o teu amigo omnisciente e companheiro pessoal no teu telemóvel. Sobre o que gostarias de conversar hoje?"
            ),
            AppLanguage(
                code = "ar-SA",
                displayName = "العربية",
                flagEmoji = "🇸🇦",
                locale = Locale("ar", "SA"),
                defaultGreeting = "مرحبًا يا صديقي! أنا ليليان، صديقك العليم ومساعدك الصوتي الشخصي على هاتفك. عن ماذا تريد أن نتحدث اليوم؟"
            ),
            AppLanguage(
                code = "ru-RU",
                displayName = "Русский",
                flagEmoji = "🇷🇺",
                locale = Locale("ru", "RU"),
                defaultGreeting = "Привет, друг! Я Лилиан, твой всезнающий друг и спутник на твоем телефоне. О чем ты хочешь поговорить сегодня?"
            )
        )

        fun findByCode(code: String): AppLanguage {
            val normalized = code.replace("_", "-").lowercase()
            return SUPPORTED_LANGUAGES.firstOrNull {
                it.code.lowercase() == normalized ||
                        it.locale.language.equals(normalized.take(2), ignoreCase = true)
            } ?: SUPPORTED_LANGUAGES.first() // default to French
        }
    }
}
