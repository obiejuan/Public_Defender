package com.cmps115.public_defender;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mwglynn on 4/21/17.
 * Additions by bmccoid on 5/5/17
 */

public class PDAudioRecordingManager {

    private boolean shouldRecord = true;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_DEFAULT;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private byte[] recordingData;

    private ArrayList<byte[]> samples; //why do we have this?

    private DataOutputStream dataStream;
    private DataOutputStream servStream;


    int bufferSize = 0;
    File currentFile;

    public PDAudioRecordingManager() {
        samples = new ArrayList<>();
        bufferSize = BufferElements2Rec * BytesPerElement * 2;
    }

    void startRecording(String output_dir, BufferedOutputStream pipeOut) { //pipeOut to null if nothing
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        currentFile = createPcmFile(output_dir);
        boolean success = false;
        try {
            success = currentFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!success) {
            Log.d("Error creating file.", "");
        }
        try {
            OutputStream os = new FileOutputStream(currentFile);
            BufferedOutputStream outStream = new BufferedOutputStream(os);
            if (pipeOut != null) {
                servStream = new DataOutputStream(pipeOut); //for streaming to server
                dataStream = new DataOutputStream(outStream);
            } else {
                dataStream = new DataOutputStream(outStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        shouldRecord = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                recordThread();
            }
        }, "Audio Recording Thread");
        recordingThread.start();
    }

    void stopRecording() {
        shouldRecord = false;
        if(recorder == null) return;

        recorder.stop();
        recorder.release();

        // joining to ensure thread finishes writing before closing file
        try {
            recordingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            dataStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        recorder = null;
        convertToWav(currentFile);
    }

    private void recordThread() {
        recorder.startRecording(); //don't start until thread is ready. Frames lost otherwise.
        while (shouldRecord) {
            recordingData = new byte[bufferSize];
            recorder.read(recordingData, 0, recordingData.length);
            try {
                if (servStream != null) {
                    servStream.write(recordingData);
                }
                dataStream.write(recordingData);
            } catch (Exception e) {
                e.printStackTrace();
                stopRecording();
            }
            //samples.add(recordingData); why?
            //Log.d("Sample: ", Arrays.toString(recordingData));


        }
    }

    private void convertToWav(File pcmFile) {
        byte[] pcmData = new byte[(int) pcmFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(pcmFile));
            input.read(pcmData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        int byteRate = RECORDER_SAMPLERATE * RECORDER_CHANNELS * bufferSize / 2;

        byte[] waveHeader = generateHeader(pcmData, RECORDER_CHANNELS, byteRate);

        DataOutputStream wavStream;
        try {
            File wavFile = new File(pcmFile.getPath().substring(0, pcmFile.getPath().length() - 4) + ".wav");
            wavFile.createNewFile();
            OutputStream os = new FileOutputStream(wavFile);
            BufferedOutputStream outStream = new BufferedOutputStream(os);
            wavStream = new DataOutputStream(outStream);

            wavStream.write(waveHeader);
            wavStream.write(pcmData);
            wavStream.close();
            pcmFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static byte[] generateHeader(byte[] pcmData, int rec_channels, int byteRate)
    {
        return new byte[]
            {
                    'R',
                    'I',
                    'F',
                    'F',
                    (byte) (36 + (pcmData.length & 0xff)),
                    (byte) ((pcmData.length >> 8) & 0xff),
                    (byte) ((pcmData.length >> 16) & 0xff),
                    (byte) ((pcmData.length >> 24) & 0xff),
                    'W',
                    'A',
                    'V',
                    'E',
                    'f',
                    'm',
                    't',
                    ' ',
                    16,
                    0,
                    0,
                    0,
                    1,
                    0,
                    (byte) rec_channels,
                    0,
                    (byte) (RECORDER_SAMPLERATE & 0xff),
                    (byte) ((RECORDER_SAMPLERATE >> 8) & 0xff),
                    (byte) ((RECORDER_SAMPLERATE >> 16) & 0xff),
                    (byte) ((RECORDER_SAMPLERATE >> 24) & 0xff),
                    (byte) (byteRate & 0xff),
                    (byte) ((byteRate >> 8) & 0xff),
                    (byte) ((byteRate >> 16) & 0xff),
                    (byte) ((byteRate >> 24) & 0xff),
                    (byte) (2 * 16 / 8),
                    0,
                    16,
                    0,
                    'd',
                    'a',
                    't',
                    'a',
                    (byte) (pcmData.length & 0xff),
                    (byte) ((pcmData.length >> 8) & 0xff),
                    (byte) ((pcmData.length >> 16) & 0xff),
                    (byte) ((pcmData.length >> 24) & 0xff)
            };
    }

    /* Returns a file located at our app's external cache directory */
    public static File createPcmFile(String output_dir) {
        //File file = null;
        String timeStamp = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.US).format(new Date());
        File createdFile = new File(output_dir + "/" + timeStamp + ".pcm");
        try {
            createdFile.createNewFile();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
        return createdFile;
    }
}
