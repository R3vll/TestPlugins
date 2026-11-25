package com.r3vll.vegaport

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities

fun mapType(type: String?): ExtractorLinkType = when (type?.trim()?.lowercase()) {
    "m3u8" -> ExtractorLinkType.M3U8
    "dash", "mpd" -> ExtractorLinkType.DASH
    else -> ExtractorLinkType.VIDEO
}

fun mapQuality(quality: String?): Int {
    val cleaned = quality?.trim()?.lowercase()?.removeSuffix("p")
        ?: return Qualities.Unknown.value
    return when (cleaned) {
        "4k", "2160" -> Qualities.P2160.value
        "1440" -> Qualities.P1440.value
        "1080" -> Qualities.P1080.value
        "720" -> Qualities.P720.value
        "480" -> Qualities.P480.value
        "360" -> Qualities.P360.value
        "240" -> Qualities.P240.value
        "144" -> Qualities.P144.value
        else -> cleaned.toIntOrNull() ?: Qualities.Unknown.value
    }
}

fun mapSubtitles(stream: VegaStream): List<SubtitleFile> =
    stream.subtitles.mapNotNull { subtitle ->
        val url = subtitle.uri?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val language = (subtitle.language ?: subtitle.title ?: "en").take(2).lowercase()
        SubtitleFile(lang = language, url = url)
    }

fun mapStreams(
    source: String,
    streams: List<VegaStream>,
    referer: String = "",
): List<ExtractorLink> = streams.mapNotNull { stream ->
    val url = stream.link?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    ExtractorLink(
        source = source,
        name = stream.server?.takeIf { it.isNotBlank() } ?: source,
        url = url,
        referer = referer,
        quality = mapQuality(stream.quality),
        type = mapType(stream.type),
    )
}
