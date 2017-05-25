package com.cmps115.public_defender;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by mwglynn on 5/20/17.
 */

public class PDUnitTests {

    @Test
    public void conversionPCMtoWAV() throws Exception {
        byte [] test = PDAudioRecordingManager.generateHeader(new byte[] {0}, 1, 8000);

        byte [] expected_output = {
                82,
                73,
                70,
                70,
                37,
                0,
                0,
                0,
                87,
                65,
                86,
                69,
                102,
                109,
                116,
                32,
                16,
                0,
                0,
                0,
                1,
                0,
                1,
                0,
                64,
                31,
                0,
                0,
                64,
                31,
                0,
                0,
                4,
                0,
                16,
                0,
                100,
                97,
                116,
                97,
                1,
                0,
                0,
                0
        };

        assertArrayEquals (expected_output, test);

    }

    @Test
    public void createFile() throws Exception {
        assertTrue(PDAudioRecordingManager.createPcmFile(System.getProperty("java.io.tmpdir")).exists());
    }

}
