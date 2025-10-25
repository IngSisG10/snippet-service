package com.ingsis.grupo10.snippet.util

import java.util.UUID

/**
 * Utility class to manage user context in the application.
 * Currently provides a default user ID since auth-service is not yet implemented.
 * This class is designed to be easily extended when authentication is added.
 */
object UserContext {
    /**
     * Default user ID used when authentication is not available.
     * This represents a system/default user.
     */
    private val DEFAULT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    /**
     * Gets the current user ID.
     * Currently returns the default user ID since authentication is not implemented.
     *
     * TODO: When auth-service is implemented, this method should:
     * 1. Extract user ID from JWT token
     * 2. Validate the token
     * 3. Return the authenticated user's ID
     *
     * @return The current user's UUID
     */
    fun getCurrentUserId(): UUID {
        // TODO: Implement JWT token extraction and validation
        // For now, return the default user ID
        return DEFAULT_USER_ID
    }

    /**
     * Gets the current user ID as a string.
     *
     * @return The current user's ID as a string
     */
    fun getCurrentUserIdAsString(): String = getCurrentUserId().toString()

    /**
     * Validates if a given user ID is valid.
     *
     * @param userId The user ID to validate
     * @return true if the user ID is valid, false otherwise
     */
    fun isValidUserId(userId: String): Boolean =
        try {
            UUID.fromString(userId)
            true
        } catch (e: IllegalArgumentException) {
            false
        }

    /**
     * Converts a string to UUID, throwing an exception if invalid.
     *
     * @param userId The user ID string to convert
     * @param errorMessage The error message to use if conversion fails
     * @return The UUID representation of the user ID
     * @throws IllegalArgumentException if the user ID format is invalid
     */
    fun toUuidOrThrow(
        userId: String,
        errorMessage: String = "Invalid user ID format",
    ): UUID =
        try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(errorMessage)
        }
}
