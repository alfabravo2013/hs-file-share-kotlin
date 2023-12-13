package fileshare.service

import org.springframework.http.MediaType
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

fun ByteArray.isOfMimeType(mimeType: String): Boolean {
    return when (mimeType) {
        MediaType.TEXT_PLAIN_VALUE -> this.isPlainText()
        MediaType.IMAGE_PNG_VALUE -> this.isPngImage()
        MediaType.IMAGE_JPEG_VALUE -> this.isJpegImage()
        else -> false
    }
}

private fun ByteArray.isPlainText(): Boolean {
    try {
        val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
        utf8Decoder.reset()
        utf8Decoder.decode(ByteBuffer.wrap(this))
        return true
    } catch (e: CharacterCodingException) {
        println(e.message)
        println("is not a UTF-8 PLAIN TEXT!")
        return false
    }
}

private fun ByteArray.isPngImage(): Boolean {
    if (this.size < 8) {
        return false
    }
    val prefix = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    for ((idx, byte) in prefix.withIndex()) {
        if (this[idx] != byte) {
            println("is not a PNG!")
            return false
        }
    }
    return true
}

private fun ByteArray.isJpegImage(): Boolean {
    println("is not a JPEG!")
    return this[0] == 0xff.toByte()
            && this[1] == 0xd8.toByte()
            && this[this.size - 2] == 0xff.toByte()
            && this.last() == 0xd9.toByte()
}
