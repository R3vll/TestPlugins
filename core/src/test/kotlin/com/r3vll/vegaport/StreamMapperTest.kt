package com.r3vll.vegaport

import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamMapperTest {
    @Test
    fun `maps m3u8 stream with 1080 quality and server name`() {
        val link = mapStreams(
            source = "Vega",
            streams = listOf(
                VegaStream(
                    server = "Server A",
                    link = "https://example.com/master.m3u8",
                    type = "m3u8",
                    quality = "1080",
                ),
            ),
        ).single()

        assertEquals("Vega", link.source)
        assertEquals("Server A", link.name)
        assertEquals("https://example.com/master.m3u8", link.url)
        assertEquals(ExtractorLinkType.M3U8, link.type)
        assertEquals(Qualities.P1080.value, link.quality)
    }

    @Test
    fun `maps quality with p suffix and 4k alias`() {
        assertEquals(Qualities.P720.value, mapQuality("720p"))
        assertEquals(Qualities.P2160.value, mapQuality("4K"))
        assertEquals(Qualities.Unknown.value, mapQuality("ultra"))
    }

    @Test
    fun `drops streams with blank links`() {
        val links = mapStreams(
            source = "Vega",
            streams = listOf(VegaStream(server = "Broken", link = "  ")),
        )
        assertTrue(links.isEmpty())
    }

    @Test
    fun `maps subtitles using language prefix and ignores blank urls`() {
        val subs = mapSubtitles(
            VegaStream(
                subtitles = listOf(
                    VegaSubtitle(language = "en-US", uri = "https://example.com/en.vtt"),
                    VegaSubtitle(title = "Français", uri = "https://example.com/fr.vtt"),
                    VegaSubtitle(language = "de", uri = "  "),
                ),
            ),
        )

        assertEquals(2, subs.size)
        assertEquals("en", subs[0].lang)
        assertEquals("https://example.com/en.vtt", subs[0].url)
        assertEquals("fr", subs[1].lang)
    }
}
