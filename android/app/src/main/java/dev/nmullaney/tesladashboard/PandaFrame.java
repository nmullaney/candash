package dev.nmullaney.tesladashboard;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.util.Log;
public class PandaFrame {
    private static final String TAG = PandaFrame.class.getSimpleName();
    private static final Integer INT_BYTES = 8;
    private static final int PAYLOAD_OFFSET = 8;
    private final byte[] mBuf;
    private final long mFrameId;
    private final long mBusId;
    private final long mFrameLength;

    private static final byte[] EMPTY = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public PandaFrame(byte[] buf) {
        mBuf = buf;
        mFrameId = getUnsignedInt(0, 4) >> 21;

        long secondUnsignedInt = getUnsignedInt(4, 4);
        mBusId = secondUnsignedInt >> 4;
        mFrameLength = secondUnsignedInt & 0x0F;
    }

    public long getUnsignedInt(int offset, int length) {
        ByteBuffer frameBuffer = ByteBuffer.allocate(INT_BYTES);
        frameBuffer.order(ByteOrder.LITTLE_ENDIAN);
        frameBuffer.put(mBuf, offset, length);
        frameBuffer.put(EMPTY, 0, INT_BYTES - length);
        frameBuffer.rewind();
        return frameBuffer.getLong();
    }


    private static String signExtend(String str){
        //TODO add bounds checking
        int n=32-str.length();
        char[] sign_ext = new char[n];
        Arrays.fill(sign_ext, str.charAt(0));

        return new String(sign_ext)+str;
    }

    /**
     * Returns the payload as a binary string.
     */
    public String getPayloadBinaryString() {
        StringBuilder sb = new StringBuilder();
        for (int i = PAYLOAD_OFFSET; i < mBuf.length; i++) {
            // AND-ing the byte converts it to an unsigned byte
            String binaryString = StringUtils.prepadZeroes(Long.toBinaryString(mBuf[i] & 0xFF), 8);
            sb.append(binaryString);
        }
        return sb.toString();
    }


    /**
     * Returns the value for this startBit and bitLength as a long.
     * Assumes the payload is littleEndian.
     */
    public String getPayloadValue(int startBit, int bitLength) {
        String fullPayloadBinaryString = getPayloadBinaryString();
        return getValueString(fullPayloadBinaryString, startBit, bitLength);
        //return Long.parseLong(valueString, 2);
    }
    /**
     * Returns the value for this startBit and bitLength as a long.
     * Assumes the payload is littleEndian. Adds filtering for multiplexed data
     */
    public String getPayloadValue(int startBit, int bitLength, int serviceIndex, int muxIndex) {
        String fullPayloadBinaryString = getPayloadBinaryString();
        return getValueString(fullPayloadBinaryString, startBit, bitLength, serviceIndex, muxIndex);
        //return Long.parseLong(valueString, 2);
    }

    /**
     * Pulls a binary string from the fullPayload (binary String) for the given startBit
     * and bitLength.  The window for the value goes right to left for each 8 bits.  So for
     * example, if the bitLength was 7 and the startBit was 0 and the payload was
     * 10110101, we should return 0110101, not 1011010.
     * Also, this assumes the payload is in littleEndian, and returns a value that is
     * parsable in bigEndian (i.e. Java).
     */
    private String getValueString(String fullPayload, int startBit, int bitLength) {
        StringBuilder result = new StringBuilder();
        while (bitLength > 0) {
            int length = Math.min(bitLength, 8);
            int next8 = startBit + 8 - (startBit % 8);
            int start = next8 - length - startBit % 8;
            result.insert(0, fullPayload.substring(start, start + length));
            startBit += length;
            bitLength -= 8;
        }
        return result.toString();
    }
    /**
     * Overloaded method for getValueString to add optional serviceIndex and muxIndex parameters
     */
    private String getValueString(String fullPayload, int startBit, int bitLength, int serviceIndex, int muxIndex) {
        StringBuilder result = new StringBuilder();
        /*
        Log.d(TAG, "muxIndex: " + muxIndex);
        Log.d(TAG, "GVSMUXPayload: " + fullPayload);
        Log.d(TAG, "muxIndex: " + muxIndex);
        Log.d(TAG, "serviceIndex: " + serviceIndex);
 */

        long muxValue = Long.parseLong(getValueString(fullPayload,0,3),2);

        if (muxValue == muxIndex) {
            //Log.d(TAG, "muxValue: " + muxValue+ " GVSMUXPayload: " + fullPayload + " unparsedMux" + fullPayload.substring(0,serviceIndex) + "result " + Long.parseLong(result.toString(),2));

            return getValueString(fullPayload,startBit,bitLength);


        } else {
            return "";
        }
    }
    public long getFrameId() {
        return mFrameId;
    }

    public Hex getFrameIdHex() {
        return new Hex(mFrameId);
    }

    public long getBusId() {
        return mBusId;
    }

    public long frameLength() {
        return mFrameLength;
    }
}
