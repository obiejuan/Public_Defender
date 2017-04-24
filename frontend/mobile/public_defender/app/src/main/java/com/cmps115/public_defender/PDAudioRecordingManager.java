package com.cmps115.public_defender;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.util.Log;

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

    private short[] recordingData;

    private ArrayList<short[]> samples;

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
        Log.d("Recorded # samples", "" + samples.size());
    }

    private void recordThread() {
        while (shouldRecord) {
            recordingData = new short[bufferSize];
            recorder.read(recordingData, 0, recordingData.length);
            samples.add(recordingData);
            //Log.d("Sample: ", Arrays.toString(recordingData));

        }
    }
}
