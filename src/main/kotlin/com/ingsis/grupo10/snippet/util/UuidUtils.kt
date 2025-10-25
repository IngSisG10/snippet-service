package com.ingsis.grupo10.snippet.util

import java.util.UUID

fun String.toUuidOrNull(): UUID? =
    try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        null
    }

fun String.toUuidOrThrow(errorMessage: String = "Invalid UUID format"): UUID =
    try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException(errorMessage, e)
    }
