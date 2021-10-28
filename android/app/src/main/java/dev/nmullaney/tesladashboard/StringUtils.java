package dev.nmullaney.tesladashboard;

public class StringUtils {

    public static String prepadZeroes(String str, int strLength) {
        return repeat("0", strLength - str.length()) + str;
    }

    public static String prepadNZeroes(String str, int numZeroes) {
        return repeat("0", numZeroes) + str;
    }

    public static String repeat(String substring, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(substring);
        }
        return sb.toString();
    }
    public static String outputBinaryText(byte[] bytes){
        String output = "";
        for (byte b : bytes) {
            output = output + Integer.toBinaryString(b & 255 | 256).substring(1);
        }
        return output;
    }

}
