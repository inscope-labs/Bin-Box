package com.inscopelabs.abx.binbox.core.error

/**
 * Domain and system-level structured error taxonomy.
 */
sealed class AppError(
    open val userMessage: String,
    override val message: String = userMessage,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    // --- Network & Transport Errors ---
    sealed class NetworkError(
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(userMessage = userMessage, cause = cause) {
        data class Generic(override val userMessage: String, override val cause: Throwable? = null) :
            NetworkError(userMessage, cause)
        data class HostUnreachable(val host: String, override val cause: Throwable? = null) :
            NetworkError("Host $host is unreachable.", cause)
        data class ConnectionTimeout(val host: String, val timeoutMs: Long) :
            NetworkError("Connection to $host timed out after ${timeoutMs}ms.")
        data class SshHandshakeFailed(val detail: String, override val cause: Throwable? = null) :
            NetworkError("SSH handshake negotiation failed: $detail", cause)
        data class DnsResolutionError(val host: String, override val cause: Throwable? = null) :
            NetworkError("Could not resolve host $host.", cause)
        data class ConnectionClosed(val reason: String) :
            NetworkError("Connection closed unexpectedly: $reason")

        companion object {
            operator fun invoke(userMessage: String, cause: Throwable? = null): NetworkError =
                Generic(userMessage, cause)
        }
    }

    // --- Authentication & Security Errors ---
    sealed class AuthError(
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(userMessage = userMessage, cause = cause) {
        data class Generic(override val userMessage: String, override val cause: Throwable? = null) :
            AuthError(userMessage, cause)
        data class InvalidCredentials(val username: String) :
            AuthError("Authentication failed for user '$username'. Check your password or private key.")
        data class KeyRejected(val keyAlias: String) :
            AuthError("SSH key '$keyAlias' was rejected by the remote host.")
        data class PassphraseRequired(val keyAlias: String) :
            AuthError("Passphrase required for encrypted key '$keyAlias'.")
        data class BiometricAuthFailed(val reason: String) :
            AuthError("Biometric authentication failed: $reason")
        data object UserCancelledAuth :
            AuthError("Authentication was cancelled by the user.")

        companion object {
            operator fun invoke(userMessage: String, cause: Throwable? = null): AuthError =
                Generic(userMessage, cause)
        }
    }

    // --- Cryptography & Keystore Errors ---
    sealed class CryptoError(
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(userMessage = userMessage, cause = cause) {
        data class Generic(override val userMessage: String, override val cause: Throwable? = null) :
            CryptoError(userMessage, cause)
        data class KeyGenerationFailed(val algorithm: String, override val cause: Throwable? = null) :
            CryptoError("Failed to generate $algorithm key pair.", cause)
        data class DecryptionFailed(override val cause: Throwable? = null) :
            CryptoError("Failed to decrypt secure payload.", cause)
        data class KeystoreUnavailable(override val cause: Throwable? = null) :
            CryptoError("Android KeyStore hardware provider is unavailable.", cause)
        data class InvalidKeyFormat(val detail: String) :
            CryptoError("Invalid key format: $detail")

        companion object {
            operator fun invoke(userMessage: String, cause: Throwable? = null): CryptoError =
                Generic(userMessage, cause)
        }
    }

    // --- I/O & Local System Errors ---
    sealed class IoError(
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(userMessage = userMessage, cause = cause) {
        data class Generic(override val userMessage: String, override val cause: Throwable? = null) :
            IoError(userMessage, cause)
        data class FileNotFound(val path: String) :
            IoError("File not found at '$path'.")
        data class PermissionDenied(val path: String) :
            IoError("Permission denied accessing '$path'.")
        data class SocketError(override val userMessage: String, override val cause: Throwable? = null) :
            IoError(userMessage, cause)

        companion object {
            operator fun invoke(userMessage: String, cause: Throwable? = null): IoError =
                Generic(userMessage, cause)
        }
    }

    // --- Session & Terminal State Errors ---
    sealed class SessionError(
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(userMessage = userMessage, cause = cause) {
        data class Generic(override val userMessage: String, override val cause: Throwable? = null) :
            SessionError(userMessage, cause)
        data class SessionNotFound(val sessionId: String) :
            SessionError("Session '$sessionId' was not found.")
        data class MaxSessionsExceeded(val maxAllowed: Int) :
            SessionError("Maximum concurrent sessions limit ($maxAllowed) reached.")
        data class SessionAlreadyClosed(val sessionId: String) :
            SessionError("Session '$sessionId' is already terminated.")

        companion object {
            operator fun invoke(userMessage: String, cause: Throwable? = null): SessionError =
                Generic(userMessage, cause)
        }
    }

    // --- General / Unexpected Fallback ---
    data class UnexpectedError(
        override val userMessage: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(userMessage = userMessage, cause = cause)
}
