package pl.stalmasinski.ballpassage.ui.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** QR encoding and decoding with ZXing, fully on the device. */
object QrCodes {
    /**
     * Larger codes get too dense to scan reliably from another phone's screen; such sets are
     * shared as text instead.
     */
    const val MAX_BYTES = 1_200

    private val encodeHints = mapOf(
        EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 2,
    )

    private val decodeHints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
        DecodeHintType.TRY_HARDER to true,
    )

    fun fits(text: String): Boolean = text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES

    /** One cell per module; returns null when [text] does not fit into a QR code. */
    fun encode(text: String): BitMatrix? =
        if (!fits(text)) null else runCatching { QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, encodeHints) }.getOrNull()

    /**
     * Decodes a QR code from a grayscale image, such as the Y plane of a camera frame.
     *
     * @param rowStride bytes per row in [luminance]; may be larger than [width].
     */
    fun decode(luminance: ByteArray, width: Int, height: Int, rowStride: Int = width): String? {
        val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        return try {
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), decodeHints).text
        } catch (_: ReaderException) {
            null
        }
    }
}
