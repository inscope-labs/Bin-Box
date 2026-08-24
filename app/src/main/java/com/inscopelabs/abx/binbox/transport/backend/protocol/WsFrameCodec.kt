package com.inscopelabs.abx.binbox.transport.backend.protocol

import android.util.Base64
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Encoder/Decoder for serializing and deserializing WebSocket terminal frames.
 */
class WsFrameCodec(
    moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    private val adapter = moshi.adapter(WsTerminalFrame::class.java)

    /**
     * Serializes a [WsTerminalFrame] to JSON string.
     */
    fun encode(frame: WsTerminalFrame): String {
        return adapter.toJson(frame)
    }

    /**
     * Deserializes a JSON string into a [WsTerminalFrame].
     * Returns null if JSON parsing fails or text is malformed.
     */
    fun decode(text: String): WsTerminalFrame? {
        return try {
            adapter.fromJson(text)
        } catch (_: Exception) {
            // If raw text was received that isn't JSON, treat it as a direct DATA frame
            WsTerminalFrame.data(payload = text)
        }
    }

    /**
     * Encodes raw byte payload into a Base64 DATA frame JSON string.
     */
    fun encodeBytesToDataFrame(bytes: ByteArray, sessionId: String? = null): String {
        val base64Str = try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
        return encode(WsTerminalFrame.data(payload = base64Str, sessionId = sessionId, isBase64 = true))
    }

    /**
     * Decodes payload bytes from a [WsTerminalFrame], handling UTF-8 text or Base64 binary.
     */
    fun extractPayloadBytes(frame: WsTerminalFrame): ByteArray {
        val payload = frame.payload ?: return ByteArray(0)
        return if (frame.isBase64) {
            try {
                Base64.decode(payload, Base64.DEFAULT)
            } catch (_: Throwable) {
                java.util.Base64.getDecoder().decode(payload)
            }
        } else {
            payload.toByteArray(Charsets.UTF_8)
        }
    }
}
