package com.cmps115.public_defender;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mwglynn on 4/21/17.
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

    private ArrayList<byte[]> samples;

    DataOutputStream dataStream;

    int bufferSize = 0;

    public PDAudioRecordingManager()
    {
        samples = new ArrayList<>();
        bufferSize = BufferElements2Rec * BytesPerElement * 2;
    }

    void StartRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                +"/outfile.pcm");
        try
        {
            file.createNewFile();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream outStream = new BufferedOutputStream(os);
            dataStream = new DataOutputStream(outStream);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        recorder.startRecording();

        shouldRecord = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                recordThread();
            }
        }, "Audio Recording Thread");
        recordingThread.start();
    }

    void StopRecording() {
        shouldRecord = false;
        recorder.stop();
        recorder.release();
        try
        {
            dataStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.d("Recorded # samples", "" + samples.size());
    }

    private void recordThread() {
        while (shouldRecord) {
            recordingData = new byte[bufferSize];
            recorder.read(recordingData, 0, recordingData.length);
            try
            {
                dataStream.write(recordingData);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            samples.add(recordingData);
            //Log.d("Sample: ", Arrays.toString(recordingData));
            

        }
    }

    private void convertToWav(File pcmFile) {

    }
}
