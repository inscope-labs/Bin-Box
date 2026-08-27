package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Result of parsing an OCI configuration snippet or INI block.
 */
data class ParsedOciConfig(
    val tenancyOcid: String? = null,
    val userOcid: String? = null,
    val fingerprint: String? = null,
    val region: String? = null,
    val keyFile: String? = null,
    val domainUrl: String? = null,
    val regionalUrl: String? = null
) {
    val hasAnyField: Boolean
        get() = tenancyOcid != null || userOcid != null || fingerprint != null || region != null
}

/**
 * Parses OCI configuration previews, ~/.oci/config INI files, or pasted credentials.
 */
object OciConfigParser {
    private const val TAG = "OciConfigParser"

    private val FINGERPRINT_REGEX = Regex("([0-9a-fA-F]{2}:){15}[0-9a-fA-F]{2}")
    private val USER_OCID_REGEX = Regex("ocid1\\.user\\.oc[0-9]*\\.\\.[a-z0-9]+")
    private val TENANCY_OCID_REGEX = Regex("ocid1\\.tenancy\\.oc[0-9]*\\.\\.[a-z0-9]+")
    private val DOMAIN_OCID_REGEX = Regex("ocid1\\.domain\\.oc[0-9]*\\.\\.[a-z0-9]+")
    private val URL_REGEX = Regex("https?://[a-zA-Z0-9.-]+(:[0-9]+)?")

    fun parse(rawText: String): ParsedOciConfig {
        if (rawText.isBlank()) return ParsedOciConfig()
        BinBoxLogger.d(TAG, "Parsing OCI config snippet of length ${rawText.length}")

        var user: String? = null
        var tenancy: String? = null
        var fingerprint: String? = null
        var region: String? = null
        var keyFile: String? = null
        var domainUrl: String? = null
        var regionalUrl: String? = null

        val lines = rawText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) continue
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) continue

            val separatorIndex = trimmed.indexOfFirst { it == '=' || it == ':' }
            if (separatorIndex != -1) {
                val key = trimmed.substring(0, separatorIndex).trim().lowercase()
                val rawVal = trimmed.substring(separatorIndex + 1).trim()
                val value = rawVal.split("#", ";")[0].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")

                when (key) {
                    "user", "user_ocid", "userocid" -> user = value
                    "tenancy", "tenancy_ocid", "tenancyocid" -> tenancy = value
                    "fingerprint" -> fingerprint = value
                    "region" -> region = OciRegionHelper.normalizeRegion(value)
                    "key_file", "keyfile" -> keyFile = value
                    "domain_url", "domain url", "domainurl" -> domainUrl = value
                    "regional_url", "regional url", "regionalurl" -> regionalUrl = value
                }
            }
        }

        // Regex fallbacks for freeform text or partial pastes
        if (user == null) {
            user = USER_OCID_REGEX.find(rawText)?.value
        }
        if (tenancy == null) {
            tenancy = TENANCY_OCID_REGEX.find(rawText)?.value
        }
        if (fingerprint == null) {
            fingerprint = FINGERPRINT_REGEX.find(rawText)?.value
        }
        if (region == null) {
            region = OciRegionHelper.detectRegionFromText(rawText)
        }

        val result = ParsedOciConfig(
            tenancyOcid = tenancy,
            userOcid = user,
            fingerprint = fingerprint,
            region = region,
            keyFile = keyFile,
            domainUrl = domainUrl,
            regionalUrl = regionalUrl
        )
        BinBoxLogger.i(TAG, "Parsed config result: tenancy=${tenancy != null}, user=${user != null}, fp=${fingerprint != null}, region=$region")
        return result
    }
}
