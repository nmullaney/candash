package app.candash.cluster;

import junit.framework.TestCase;

public class PandaFrameTest extends TestCase {

    public void testGetValueString() {
        String payload = "0110111001000110000111110000000000000000101000000000111100000001";
        PandaFrame.getValueString(payload, 12, 12);
    }
}