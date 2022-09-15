package app.candash.cluster

class NewPandaFrame(wholeByteArray: ByteArray) {

    private val headerByteArray = wholeByteArray.sliceArray(0 until 8)
    private val payloadByteArray = wholeByteArray.sliceArray(8 until 16)
    private val firstHeaderValue = getHeaderValue(0, 4)
    private val secondHeaderValue = getHeaderValue(4, 4)
    val frameId = firstHeaderValue shr 21
    val frameIdHex
        get() = Hex(frameId)
    val frameLength = secondHeaderValue and 0x0F
    val busId = secondHeaderValue shr 4

    private fun getHeaderValue(start: Int, length: Int): Long {
        return headerByteArray.sliceArray(start until (start + length))
            .foldIndexed(0L) { index, value, byte ->
                value + (byte.toUByte().toLong() shl (index * 8))
            }
    }
    // for checking if the bytes in the range specified are zero (useful for tossing out unwanted frames from the wrong bus)
    fun isZero(start: Int = 0, end: Int = (payloadByteArray.size - 1)): Boolean {
        for (i in start..end) {
            if (payloadByteArray[i].toInt() != 0) {
                return false
            }
        }
        return true
    }

    fun getCANValue(canSignal: CANSignal): Float? {
        if (!isCorrectMux(canSignal)) {
            return null
        }
        var value = 0L
        var previousByteBitLength = 0
        var startBit = canSignal.startBit
        var totalLength = canSignal.bitLength
        while (totalLength > 0) {
            val byteIndex = startBit / 8
            val endBitForByte = 8 - (startBit % 8)
            val bitLengthForByte = endBitForByte.coerceAtMost(totalLength)
            val valueForByte: Long =
                (payloadByteArray[byteIndex].toLong() shr (8 - endBitForByte) and rightMask(
                    8 - bitLengthForByte
                ).toLong())
            value += valueForByte shl (previousByteBitLength)
            startBit += bitLengthForByte
            totalLength -= bitLengthForByte
            previousByteBitLength += bitLengthForByte
        }
        val result = if (canSignal.signed) {
            (valueToSigned(value, canSignal.bitLength) * canSignal.factor) + canSignal.offset
        } else {
            (value * canSignal.factor) + canSignal.offset
        }
        return if (result == canSignal.sna) null else result
    }

    private fun valueToSigned(value: Long, bits: Int): Long {
        val msbMask: Long = 1L shl (bits - 1)
        return (value xor msbMask) - msbMask
    }

    private fun isCorrectMux(canSignal: CANSignal): Boolean {
        // Not Multiplexed
        return if (canSignal.serviceIndex == 0) {
            true
        } else {
            payloadByteArray[0].toInt() and rightMask(8 - canSignal.serviceIndex) == canSignal.muxIndex
        }
    }

    private fun rightMask(length: Int): Int {
        return (0xff shr length)
    }
}

fun ByteArray.getPayloadBinaryString(): String {
    val sb = StringBuilder()
    for (i in indices) {
        val shortBinaryString = this[i].toUByte().toString(radix = 2)
        val binaryString = StringUtils.prepadZeroes(shortBinaryString, 8)
        sb.append(binaryString)
    }
    return sb.toString()
}