package com.enigma.aminlivetv

internal object MediaSample {
    fun transportVideo(bytes: ByteArray): Boolean {
        val packet = 188
        val start = (0 until minOf(packet, bytes.size - 2 * packet))
            .firstOrNull { bytes.u(it) == 0x47 && bytes.u(it + packet) == 0x47 && bytes.u(it + 2 * packet) == 0x47 }
            ?: return false
        for (position in start..(bytes.size - packet) step packet) {
            if (bytes.u(position) != 0x47 || bytes.u(position + 1) and 0x40 == 0) continue
            val control = (bytes.u(position + 3) ushr 4) and 3
            val payload = when (control) {
                1 -> position + 4
                3 -> position + 5 + bytes.u(position + 4)
                else -> continue
            }
            if (payload + 4 > position + packet) continue
            if (bytes.u(payload) == 0 && bytes.u(payload + 1) == 0 && bytes.u(payload + 2) == 1 &&
                bytes.u(payload + 3) in 0xe0..0xef
            ) return true
        }
        return false
    }

    fun fragment(bytes: ByteArray): Boolean =
        bytes.hasBox("moof") && bytes.hasBox("mdat", minimumPayload = 8)

    fun progressiveVideo(bytes: ByteArray): Boolean =
        bytes.hasBox("ftyp") && videoTrack(bytes) && bytes.hasBox("mdat", minimumPayload = 8)

    fun videoTrack(bytes: ByteArray): Boolean {
        for (index in 4 until bytes.size - 16) {
            if (bytes.asciiAt(index, "hdlr") && bytes.asciiAt(index + 12, "vide")) return true
        }
        return false
    }

    private fun ByteArray.hasBox(kind: String, minimumPayload: Int = 0): Boolean {
        var cursor = 0
        while (cursor <= size - 8) {
            val length = (u(cursor).toLong() shl 24) + (u(cursor + 1) shl 16) + (u(cursor + 2) shl 8) + u(cursor + 3)
            if (length < 8) return false
            if (asciiAt(cursor + 4, kind)) return length >= 8 + minimumPayload && cursor + 8 + minimumPayload <= size
            if (length > size - cursor) return false
            cursor += length.toInt()
        }
        return false
    }

    private fun ByteArray.asciiAt(index: Int, text: String): Boolean =
        text.indices.all { u(index + it) == text[it].code }

    private fun ByteArray.u(index: Int): Int = this[index].toInt() and 0xff
}
