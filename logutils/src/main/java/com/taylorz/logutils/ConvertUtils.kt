package com.taylorz.logutils


/**
 * <pre>
 * author: Blankj
 * blog  : http://blankj.com
 * time  : 2016/08/13
 * desc  : utils about convert
</pre> *
 */
object ConvertUtils {
    private val HEX_DIGITS_UPPER = charArrayOf(
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'A',
        'B',
        'C',
        'D',
        'E',
        'F'
    )
    private val HEX_DIGITS_LOWER = charArrayOf(
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'a',
        'b',
        'c',
        'd',
        'e',
        'f'
    )

    /**
     * Bytes to hex string.
     *
     * e.g. bytes2HexString(new byte[] { 0, (byte) 0xa8 }) returns "00A8"
     *
     * @param bytes The bytes.
     * @return hex string
     */
    fun bytes2HexString(bytes: ByteArray?): String {
        return bytes2HexString(bytes, true)
    }

    /**
     * Bytes to hex string.
     *
     * e.g. bytes2HexString(new byte[] { 0, (byte) 0xa8 }, true) returns "00A8"
     *
     * @param bytes       The bytes.
     * @param isUpperCase True to use upper case, false otherwise.
     * @return hex string
     */
    fun bytes2HexString(bytes: ByteArray?, isUpperCase: Boolean): String {
        if (bytes == null) return ""
        val hexDigits = if (isUpperCase) HEX_DIGITS_UPPER else HEX_DIGITS_LOWER
        val len = bytes.size
        if (len <= 0) return ""
        val ret = CharArray(len shl 1)
        var i = 0
        var j = 0
        while (i < len) {
            ret[j++] = hexDigits[bytes[i].toInt() shr 4 and 0x0f]
            ret[j++] = hexDigits[bytes[i].toInt() and 0x0f]
            i++
        }
        return String(ret)
    }
}
