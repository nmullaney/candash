package dev.nmullaney.tesladashboard

import kotlin.experimental.and

class NewPandaFrame(wholeByteArray: ByteArray) {

    private val headerByteArray = wholeByteArray.sliceArray(0 until 8)
    private val payloadByteArray = wholeByteArray.sliceArray(8 until 16)
    private val frameId: Long = headerByteArray[0].toLong() shr 21
    private val frameIdHex
        get() = Hex(frameId)
    private val frameLength = headerByteArray[1] and 0x0F
    private val busId = headerByteArray[1].toLong() shr 4

    public fun getCANValue(canSignal: CANSignal): Float? {
        if (!isCorrectMutex(canSignal)) {
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
            val valueForByte : Long =
                (payloadByteArray[byteIndex].toLong() shr (8 - endBitForByte) and rightMask(
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

    private fun valueToSigned(value: Long, bits : Int) : Long {
        val msbMask : Long = 1L shl (bits - 1)
        return (value xor msbMask) - msbMask
    }

    private fun isCorrectMutex(canSignal: CANSignal): Boolean {
        // Not Multiplexed
        return if (canSignal.serviceIndex == 0) {
            true
        } else {
            payloadByteArray[0].toInt() shr (8 - canSignal.serviceIndex) == canSignal.muxIndex
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