package com.inscopelabs.abx.binbox.oci.wizard

data class OciRegionItem(
    val code: String,
    val displayName: String
)

/**
 * Helper for OCI region resolution, mapping friendly names to region codes,
 * and presenting region options in assisted wizards.
 */
object OciRegionHelper {

    val POPULAR_REGIONS = listOf(
        OciRegionItem("sa-saopaulo-1", "Brazil East (Sao Paulo)"),
        OciRegionItem("us-ashburn-1", "US East (Ashburn)"),
        OciRegionItem("us-phoenix-1", "US West (Phoenix)"),
        OciRegionItem("us-sanjose-1", "US West (San Jose)"),
        OciRegionItem("us-chicago-1", "US Midwest (Chicago)"),
        OciRegionItem("eu-frankfurt-1", "Germany Central (Frankfurt)"),
        OciRegionItem("eu-amsterdam-1", "Netherlands Northwest (Amsterdam)"),
        OciRegionItem("uk-london-1", "UK South (London)"),
        OciRegionItem("ca-toronto-1", "Canada Southeast (Toronto)"),
        OciRegionItem("ap-tokyo-1", "Japan East (Tokyo)"),
        OciRegionItem("ap-seoul-1", "South Korea Central (Seoul)"),
        OciRegionItem("ap-singapore-1", "Singapore (Singapore)"),
        OciRegionItem("ap-sydney-1", "Australia East (Sydney)"),
        OciRegionItem("ap-mumbai-1", "India West (Mumbai)"),
        OciRegionItem("me-jeddah-1", "Saudi Arabia West (Jeddah)"),
        OciRegionItem("sa-santiago-1", "Chile Central (Santiago)"),
        OciRegionItem("mx-queretaro-1", "Mexico Central (Queretaro)")
    )

    fun normalizeRegion(raw: String): String {
        val trimmed = raw.trim().lowercase()
        // If it's already a standard code like sa-saopaulo-1 or us-ashburn-1
        if (trimmed.matches(Regex("^[a-z]{2}-[a-z0-9-]+-[0-9]+$"))) {
            return trimmed
        }

        // Check against known display names
        for (item in POPULAR_REGIONS) {
            if (trimmed == item.code.lowercase() ||
                trimmed == item.displayName.lowercase() ||
                item.displayName.lowercase().contains(trimmed) ||
                trimmed.contains(item.displayName.lowercase()) ||
                trimmed.contains(item.code.lowercase())
            ) {
                return item.code
            }
        }

        // Specific alias checks
        if (trimmed.contains("sao paulo") || trimmed.contains("brazil")) return "sa-saopaulo-1"
        if (trimmed.contains("ashburn")) return "us-ashburn-1"
        if (trimmed.contains("phoenix")) return "us-phoenix-1"
        if (trimmed.contains("frankfurt")) return "eu-frankfurt-1"
        if (trimmed.contains("london")) return "uk-london-1"
        if (trimmed.contains("tokyo")) return "ap-tokyo-1"

        return raw.trim()
    }

    fun detectRegionFromText(text: String): String? {
        val regionCodeRegex = Regex("\\b[a-z]{2}-[a-z0-9-]+-[0-9]+\\b", RegexOption.IGNORE_CASE)
        val match = regionCodeRegex.find(text)
        if (match != null) {
            return match.value.lowercase()
        }
        for (item in POPULAR_REGIONS) {
            if (text.contains(item.displayName, ignoreCase = true) || text.contains(item.code, ignoreCase = true)) {
                return item.code
            }
        }
        return null
    }

    fun getDisplayName(code: String): String {
        return POPULAR_REGIONS.firstOrNull { it.code.equals(code, ignoreCase = true) }?.displayName ?: code
    }
}
