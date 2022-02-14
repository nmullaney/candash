package app.candash.cluster

class ElmFrame(rawData: String) {

    private val headerString = rawData.substring(0,3)
    private val payloadString = rawData.substring(4).filterNot { it.isWhitespace() }

    val frameId = rawData.substring(0, 3)
    val frameIdHex
        get() = Hex(frameId)
    private var payloadByteArray = byteArrayOf()
    fun frameLength():Int {
        return payloadString.length
    }
    fun getCANValue(canSignal: CANSignal): Float? {
        payloadByteArray = getByteArray(payloadString)
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
                (payloadByteArray?.get(byteIndex).toLong() shr (8 - endBitForByte) and rightMask(
                    8 - bitLengthForByte
                ).toLong())
            value += valueForByte shl (previousByteBitLength)
            startBit += bitLengthForByte
            totalLength -= bitLengthForByte
            previousByteBitLength += bitLengthForByte
        }
        return if (canSignal.signed) {
            (valueToSigned(value, canSignal.bitLength) * canSignal.factor) + canSignal.offset
        } else {
            (value * canSignal.factor) + canSignal.offset
        }
    }
    fun isZero(start: Int = 0, end: Int = (payloadByteArray.size - 1)): Boolean {
        for (i in start..end) {
            if (payloadByteArray[i].toInt() != 0) {
                return false
            }
        }
        return true
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
    fun getByteArray(hexString: String): ByteArray {
        val frameData = ByteArray(hexString.length / 2)
        for (i in frameData.indices) {
            val index = i * 2

            // Using parseInt() method of Integer class
            val `val` = hexString.substring(index, index + 2).toInt(16)
            frameData[i] = `val`.toByte()
        }
        return frameData
    }
}

