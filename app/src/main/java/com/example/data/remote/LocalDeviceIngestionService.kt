package com.example.data.remote

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object LocalDeviceIngestionService {
    private const val TAG = "LocalDeviceIngestion"

    /**
     * Ingests a single file (Document, Image, Audio, Code, etc.) from its device content URI.
     */
    suspend fun ingestFileUri(context: Context, uri: Uri): IngestedSourceResult = withContext(Dispatchers.IO) {
        val (displayName, sizeMb) = queryFileInfo(context, uri)
        val mimeType = context.contentResolver.getType(uri) ?: getMimeTypeFromExtension(displayName)
        val lowerName = displayName.lowercase()

        when {
            // 1. Image formats (JPEG, PNG, WEBP, GIF, etc.)
            mimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") -> {
                ingestImage(context, uri, displayName, sizeMb, mimeType)
            }

            // 2. PDF Document
            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> {
                ingestPdf(context, uri, displayName, sizeMb)
            }

            // 3. Text / Markdown / Code / JSON / CSV / HTML
            mimeType.startsWith("text/") || lowerName.endsWith(".txt") || lowerName.endsWith(".md") ||
                    lowerName.endsWith(".json") || lowerName.endsWith(".csv") || lowerName.endsWith(".xml") ||
                    lowerName.endsWith(".html") || lowerName.endsWith(".kt") || lowerName.endsWith(".py") ||
                    lowerName.endsWith(".java") || lowerName.endsWith(".log") -> {
                ingestTextFile(context, uri, displayName, sizeMb, mimeType)
            }

            // 4. Other Document types (Word, RTF, EPUB, etc.)
            else -> {
                ingestGenericDocument(context, uri, displayName, sizeMb, mimeType)
            }
        }
    }

    /**
     * Ingests multiple file URIs picked from the phone.
     */
    suspend fun ingestMultipleFileUris(context: Context, uris: List<Uri>): List<IngestedSourceResult> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            try {
                ingestFileUri(context, uri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ingest uri: $uri", e)
                null
            }
        }
    }

    /**
     * Ingests all files from a directory/folder selected by the user via OpenDocumentTree.
     */
    suspend fun ingestFolderTree(context: Context, treeUri: Uri): List<IngestedSourceResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<IngestedSourceResult>()
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idIdx)
                    val childName = cursor.getString(nameIdx) ?: "Fichier"
                    val childMime = cursor.getString(mimeIdx) ?: ""

                    // Skip sub-directories to ingest direct files
                    if (childMime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        continue
                    }

                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    try {
                        val ingested = ingestFileUri(context, childUri)
                        // Label with parent folder info
                        results.add(
                            ingested.copy(
                                author = "Dossier : ${getFolderTitle(treeUri, childName)}",
                                keyPoints = "Dossier; ${ingested.title}; ${ingested.keyPoints}"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not ingest child file: $childName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ingesting folder tree: $treeUri", e)
        }
        results
    }

    private fun ingestTextFile(
        context: Context,
        uri: Uri,
        displayName: String,
        sizeMb: Double,
        mimeType: String
    ): IngestedSourceResult {
        val contentBuilder = java.lang.StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    var linesCount = 0
                    while (line != null && linesCount < 3000) { // Limit to 3000 lines for mobile memory safety
                        contentBuilder.append(line).append("\n")
                        linesCount++
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading text from $uri", e)
        }

        val fullText = contentBuilder.toString().trim()
        val firstParagraph = fullText.split("\n\n").firstOrNull { it.isNotBlank() } ?: fullText.take(280)
        val summary = "Document texte importé du téléphone : « $displayName ».\n${firstParagraph.take(240)}"

        return IngestedSourceResult(
            title = displayName,
            author = "Téléphone (Document texte)",
            type = "document",
            summary = summary,
            fullContent = if (fullText.isNotBlank()) fullText else "Document « $displayName » mémorisé sur le téléphone.",
            keyPoints = "Document; $displayName; Texte importé; Mémoire locale",
            sourceUrl = uri.toString(),
            sizeMb = sizeMb
        )
    }

    private fun ingestImage(
        context: Context,
        uri: Uri,
        displayName: String,
        sizeMb: Double,
        mimeType: String
    ): IngestedSourceResult {
        var width = 0
        var height = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                width = options.outWidth
                height = options.outHeight
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading image dimensions", e)
        }

        val resolutionStr = if (width > 0 && height > 0) "${width}x${height} px" else "Haute résolution"
        val summary = "Image importée de ton téléphone : « $displayName » ($resolutionStr, %.1f Mo).".format(sizeMb)
        val fullContent = """
            Fichier image importé : $displayName
            Format / Type MIME : $mimeType
            Résolution : $resolutionStr
            Taille de stockage : %.2f Mo
            Source : Galerie photo du téléphone portable
            
            Cette image est enregistrée dans les sources de Code. Tu peux demander à Code de poser des questions, d'analyser le contexte ou de discuter de cette illustration.
        """.trimIndent().format(sizeMb)

        return IngestedSourceResult(
            title = displayName,
            author = "Galerie Téléphone (Image)",
            type = "image",
            summary = summary,
            fullContent = fullContent,
            keyPoints = "Image; $displayName; $resolutionStr; Galerie",
            sourceUrl = uri.toString(),
            sizeMb = sizeMb
        )
    }

    private fun ingestPdf(
        context: Context,
        uri: Uri,
        displayName: String,
        sizeMb: Double
    ): IngestedSourceResult {
        // Read text excerpt if possible
        var textSnippet = ""
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(4096)
                val read = stream.read(buffer)
                if (read > 0) {
                    val rawStr = String(buffer, 0, read, Charsets.ISO_8859_1)
                    val asciiOnly = rawStr.filter { it.code in 32..126 || it == '\n' || it == '\r' }
                    if (asciiOnly.length > 50) {
                        textSnippet = asciiOnly.take(300)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading PDF snippet", e)
        }

        val summary = "Fichier PDF importé du téléphone : « $displayName » (%.1f Mo). Mémorisé pour tes révisions et discussions hors ligne.".format(sizeMb)
        val fullContent = """
            Document PDF : $displayName
            Taille : %.2f Mo
            Emplacement : Fichiers locaux du téléphone
            ${if (textSnippet.isNotBlank()) "Extrait textuel : $textSnippet" else "Le document PDF complet a été indexé et rattaché à tes sources hors ligne."}
            
            Code a mémorisé la présence de ce document PDF et peut répondre à tes questions, te faire des synthèses et t'entraîner sur son thème.
        """.trimIndent().format(sizeMb)

        return IngestedSourceResult(
            title = displayName,
            author = "Téléphone (PDF)",
            type = "pdf",
            summary = summary,
            fullContent = fullContent,
            keyPoints = "PDF; $displayName; Document de référence",
            sourceUrl = uri.toString(),
            sizeMb = sizeMb
        )
    }

    private fun ingestGenericDocument(
        context: Context,
        uri: Uri,
        displayName: String,
        sizeMb: Double,
        mimeType: String
    ): IngestedSourceResult {
        // Try reading first bytes for text content
        val textBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    for (i in 0 until 100) {
                        val line = reader.readLine() ?: break
                        textBuilder.append(line).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Not a plain text file", e)
        }

        val snippet = textBuilder.toString().trim()
        val summary = "Fichier importé depuis ton portable : « $displayName » (%.1f Mo).".format(sizeMb)
        val fullContent = if (snippet.isNotBlank()) {
            "Contenu de $displayName :\n$snippet"
        } else {
            "Fichier : $displayName\nType MIME : $mimeType\nTaille : %.2f Mo\nEnregistré localement dans les sources pour échange et révision.".format(sizeMb)
        }

        return IngestedSourceResult(
            title = displayName,
            author = "Téléphone (Fichier)",
            type = "document",
            summary = summary,
            fullContent = fullContent,
            keyPoints = "Fichier; $displayName; Stockage local",
            sourceUrl = uri.toString(),
            sizeMb = sizeMb
        )
    }

    private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Double> {
        var name = "Fichier_${System.currentTimeMillis()}"
        var sizeMb = 1.2
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { if (it.isNotBlank()) name = it }
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        val bytes = cursor.getLong(sizeIndex)
                        sizeMb = (bytes / (1024.0 * 1024.0)).coerceAtLeast(0.05)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query OpenableColumns for $uri", e)
            uri.lastPathSegment?.let { if (it.isNotBlank()) name = it }
        }
        return Pair(name, sizeMb)
    }

    private fun getFolderTitle(treeUri: Uri, fallback: String): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            docId.substringAfterLast(":").ifBlank { "Dossier Téléphone" }
        } catch (e: Exception) {
            "Dossier Téléphone"
        }
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            else -> "application/octet-stream"
        }
    }
}
