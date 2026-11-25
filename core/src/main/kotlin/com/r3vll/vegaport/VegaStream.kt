package com.r3vll.vegaport

data class VegaSubtitle(
    val language: String? = null,
    val uri: String? = null,
    val type: String? = null,
    val title: String? = null,
)

data class VegaStream(
    val server: String? = null,
    val link: String? = null,
    val type: String? = null,
    val quality: String? = null,
    val subtitles: List<VegaSubtitle> = emptyList(),
)
