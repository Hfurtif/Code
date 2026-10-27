package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Intelligent Android App & URL launcher inspired by JARVIS open_app and open_url tools.
 * Resolves application packages dynamically and provides fallback web navigation.
 */
object AppLauncherHelper {

    data class LaunchResult(
        val success: Boolean,
        val targetName: String,
        val feedback: String,
        val openedUrl: String? = null
    )

    fun openUrl(context: Context, rawUrl: String): LaunchResult {
        return try {
            val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                "https://$rawUrl"
            } else rawUrl
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult(
                success = true,
                targetName = formattedUrl,
                feedback = "Ouverture du site web, monsieur.",
                openedUrl = formattedUrl
            )
        } catch (e: Exception) {
            LaunchResult(
                success = false,
                targetName = rawUrl,
                feedback = "Impossible d'ouvrir l'adresse URL demandée : ${e.localizedMessage ?: "erreur"}"
            )
        }
    }

    fun openApp(context: Context, appName: String): LaunchResult {
        val clean = appName.trim().lowercase()
        val pm = context.packageManager

        // Explicit URL patterns
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.contains(".com") || clean.contains(".org") || clean.contains(".net") || clean.contains(".fr")) {
            return openUrl(context, appName)
        }

        // Common known applications on Android
        val packageCandidates = when {
            clean.contains("youtube") -> listOf("com.google.android.youtube")
            clean.contains("spotify") -> listOf("com.spotify.music")
            clean.contains("discord") -> listOf("com.discord")
            clean.contains("chrome") || clean.contains("navigateur") -> listOf("com.android.chrome")
            clean.contains("maps") || clean.contains("carte") || clean.contains("itineraire") -> listOf("com.google.android.apps.maps")
            clean.contains("mail") || clean.contains("email") || clean.contains("courriel") || clean.contains("gmail") -> listOf("com.google.android.gm")
            clean.contains("parametre") || clean.contains("settings") || clean.contains("reglage") -> listOf("com.android.settings")
            clean.contains("whatsapp") -> listOf("com.whatsapp")
            clean.contains("telegram") -> listOf("org.telegram.messenger")
            clean.contains("calcul") -> listOf("com.google.android.calculator")
            clean.contains("agenda") || clean.contains("calendrier") -> listOf("com.google.android.calendar")
            clean.contains("photos") || clean.contains("galerie") -> listOf("com.google.android.apps.photos")
            clean.contains("play store") || clean.contains("magasin") -> listOf("com.android.vending")
            clean.contains("horloge") || clean.contains("reveil") || clean.contains("alarme") -> listOf("com.google.android.deskclock")
            else -> emptyList()
        }

        // 1. Try launching through known packages
        for (pkg in packageCandidates) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val prettyName = appName.replaceFirstChar { it.uppercase() }
                return LaunchResult(
                    success = true,
                    targetName = prettyName,
                    feedback = "Ouverture de $prettyName, monsieur."
                )
            }
        }

        // 2. Query installed apps by label matching
        try {
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(clean) || clean.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        val appLabel = pm.getApplicationLabel(app).toString()
                        return LaunchResult(
                            success = true,
                            targetName = appLabel,
                            feedback = "Lancement de $appLabel, monsieur."
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // ignore
        }

        // 3. Fallback to appropriate web service
        val webFallback = when {
            clean.contains("youtube") -> "https://www.youtube.com"
            clean.contains("spotify") -> "https://open.spotify.com"
            clean.contains("discord") -> "https://discord.com"
            clean.contains("mail") || clean.contains("gmail") -> "https://mail.google.com"
            clean.contains("maps") -> "https://maps.google.com"
            clean.contains("wikipedia") -> "https://fr.wikipedia.org"
            clean.contains("github") -> "https://github.com"
            clean.contains("twitter") || clean.contains("x.com") -> "https://x.com"
            else -> "https://www.google.com/search?q=${Uri.encode(appName)}"
        }

        return openUrl(context, webFallback)
    }
}
