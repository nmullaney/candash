package dev.nmullaney.tesladashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GetFrameValueTest {

    private val fakeHeaderBS = StringUtils.repeat(StringUtils.repeat("0", 8), 8)

    @Test
    fun testAckFrame() {
        val payloadBinaryString = "00000000000000001100000000000000111100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        val byteArray = byteArray(payloadBinaryString)
        val pandaFrame = NewPandaFrame(byteArray)
        assertEquals(6L, pandaFrame.frameId)
        assertEquals(15L, pandaFrame.busId)
    }

    @Test
    fun testBinaryStringConversion() {
        val payloadBinaryString = "0110111001000110000111110000000000000000101000000000111100000001"
        val byteArray = byteArray(payloadBinaryString)
        val binaryFromArray = byteArray.getPayloadBinaryString()
        assertEquals(payloadBinaryString, binaryFromArray)
    }

    @Test
    fun testSpeed() {
        val speedCANSignal = CANSignal(Constants.vehicleSpeed, 0, Hex(0x257), 12, 12, .08f, -40f)
        val payloadBinaryString = "0110111001000110000111110000000000000000101000000000111100000001"
        val byteArray = byteArray(fakeHeaderBS + payloadBinaryString)
        val pandaFrame = NewPandaFrame(byteArray)
        assertEquals(0f, pandaFrame.getCANValue(speedCANSignal))
    }

    @Test
    fun testFrunkState() {
        val frunkCanSignal = CANSignal(Constants.frunkState, 0, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0)
        val payloadBinaryString = "0001000000100001000001000010000010100010100011110010000000001000"
        val byteArray = byteArray(fakeHeaderBS + payloadBinaryString)
        val pandaFrame = NewPandaFrame(byteArray)
        assertEquals(2f, pandaFrame.getCANValue(frunkCanSignal))
    }

    @Test
    fun testFrunkStateWrongMultiplex() {
        val frunkCanSignal = CANSignal(Constants.frunkState, 0, Hex(0x2E1), 3, 4, 1f, 0f, 3, 0)
        val payloadBinaryString = "0001001000100001000001000010000010100010100011110010000000001000"
        val byteArray = byteArray(fakeHeaderBS + payloadBinaryString)
        val pandaFrame = NewPandaFrame(byteArray)
        assertEquals(null, pandaFrame.getCANValue(frunkCanSignal))
    }

    @Test
    fun testBatteryAmps() {
        val batteryCanSignal = CANSignal(Constants.battAmps, 0, Hex(0x132), 16, 16, -.1f, 0f, signed=true)
        val payloadBinaryString = "0011101010011000111000101111111111111101001111111111111100001111"
        val byteArray = byteArray(fakeHeaderBS + payloadBinaryString)
        val pandaFrame = NewPandaFrame(byteArray)
        assertEquals(3f, pandaFrame.getCANValue(batteryCanSignal))
    }

    private fun byteArray(binaryString: String) : ByteArray {
         return binaryString.chunked(8).map {
             binaryStringToByte(it)
        }.toByteArray()
    }

    private fun binaryStringToByte(string: String) : Byte {
        return string.fold(0) { value, char ->
            (value shl 1) + char.digitToInt()
        }.toByte()
    }
}