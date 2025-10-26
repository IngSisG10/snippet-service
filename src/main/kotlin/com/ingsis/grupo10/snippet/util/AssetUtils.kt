package com.ingsis.grupo10.snippet.util

object AssetUtils {
    fun parseCodeUrl(codeUrl: String): Pair<String, String> {
        val parts = codeUrl.split("/")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid codeUrl format: $codeUrl")
        }
        return parts[0] to parts[1]
    }
}
