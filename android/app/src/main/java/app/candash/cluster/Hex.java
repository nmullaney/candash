package app.candash.cluster;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class Hex {
    private final byte[] mByteArray;

    public Hex(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }
        int i = 0;
        int size = hexString.length() / 2;
        mByteArray = new byte[size];
        while (i <= hexString.length() - 1) {
            String twoChar = hexString.substring(i, i + 2);
            byte b = getByteValue(twoChar);
            mByteArray[i / 2] = b;
            i += 2;
        }
    }

    private byte getByteValue(String twoChar) {
        if (twoChar.length() != 2) {
            throw new IllegalStateException("Unexpected input for twoChar: " + twoChar);
        }
        char[] charArray = twoChar.toCharArray();
        int value = hxCharToInt(charArray[0]) * 16 + hxCharToInt(charArray[1]);
        return (byte) value;
    }


    private int hxCharToInt(char hxChar) {
        if (hxChar >= '0' && hxChar <= '9') {
            return hxChar - '0';
        } else if (hxChar >= 'a' && hxChar <= 'f') {
            return hxChar - 'a' + 10;
        } else if (hxChar >= 'A' && hxChar <= 'F') {
            return hxChar - 'A' + 10;
        } else {
            throw new IllegalStateException("Unexpected input for hxCharToInt: " + hxChar);
        }
    }

    public Hex(byte[] byteArray) {
        mByteArray = byteArray;
    }

    public Hex(int i) {
        this(Long.toHexString(i));
    }

    public Hex(long l) {
        this(Long.toHexString(l));
    }

    public String getString() {
        StringBuilder buffer = new StringBuilder();
        for (byte b : mByteArray) {
            buffer.append(String.format("%x", b));
        }
        return buffer.toString();
    }

    public String getHexString() {
        return "0x" + getString();
    }

    public byte[] getByteArray() {
        return mByteArray;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mByteArray);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Hex))
            return false;
        return Arrays.equals(mByteArray, ((Hex) obj).getByteArray());
    }
}
