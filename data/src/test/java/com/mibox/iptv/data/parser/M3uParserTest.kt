package com.mibox.iptv.data.parser

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {

    private val sample = """
        #EXTM3U
        #EXTINF:-1 tvg-id="hbo.pl" tvg-logo="http://logo/hbo.png" group-title="Film",HBO HD
        http://server/live/hbo.m3u8
        #EXTINF:-1 group-title="Sport",Eurosport
        http://server/live/euro.m3u8
    """.trimIndent()

    @Test
    fun `parses entries with attributes streamed`() = runTest {
        val entries = M3uParser().parse(sample.byteInputStream()).toList()

        assertEquals(2, entries.size)

        val hbo = entries[0]
        assertEquals("HBO HD", hbo.name)
        assertEquals("hbo.pl", hbo.tvgId)
        assertEquals("http://logo/hbo.png", hbo.tvgLogo)
        assertEquals("Film", hbo.groupTitle)
        assertEquals("http://server/live/hbo.m3u8", hbo.url)

        val euro = entries[1]
        assertEquals("Eurosport", euro.name)
        assertNull(euro.tvgId)
        assertEquals("Sport", euro.groupTitle)
    }
}
