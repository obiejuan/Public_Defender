package cs115.PublicDefender;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;

import java.nio.ByteBuffer;

/**
 * Created by mwglynn on 4/21/17.
 */

public class PDAudioRecordingManager {

    private boolean shouldRecord = true;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void StartRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        shouldRecord = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                recordThread();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    public void StopRecording() {


    }

    private void recordThread()
    {
        while (shouldRecord)
        {

        }
    }
}
