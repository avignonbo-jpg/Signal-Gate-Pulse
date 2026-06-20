package com.signalgate.multipoint.data.sources

data class TrustedSource(
    val name: String,
    val url: String?,
    val type: String,
    val defaultEnabled: Boolean = true,
    val defaultPriority: Int = 50
)

object TrustedSourceCatalog {
    val sources = listOf(
        TrustedSource(
            name = "FTC Spam Reports",
            url = "https://www.ftc.gov/policy-research/data/ftc-consumer-complaint-data",
            type = "spam",
            defaultEnabled = true,
            defaultPriority = 90
        ),
        TrustedSource(
            name = "FCC Robocall Data",
            url = "https://www.fcc.gov/ecfs/search",
            type = "robocall",
            defaultEnabled = true,
            defaultPriority = 85
        ),
        // Add more as needed
        TrustedSource(
            name = "Community Blocklist",
            url = "https://raw.githubusercontent.com/community/blocklist/main/numbers.txt",
            type = "community",
            defaultEnabled = false,
            defaultPriority = 60
        )
    )
}