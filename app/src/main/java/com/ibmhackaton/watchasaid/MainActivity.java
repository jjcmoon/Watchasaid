package com.ibmhackaton.watchasaid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends WearableActivity {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private Thread processThread = null;
    private boolean recording = true;

    private int bufferSize = 2048;

    private int BYTES_PER_SECONDS = RECORDER_SAMPLERATE * 2;
    private int RECSS = 30;

    private RecordingProcessor recordingProcessor;


    private static final String CONSUMER_KEY = "jeroenvanderdonckt";
    private static final String CONSUMER_SECRET = "5066ecbcdaf45c34";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        recordingProcessor = new RecordingProcessor();
        requestPermissions();
        startRecording();

        //Timer timer = new Timer();
        //TimerTask clamping = new ClamperTask(new File(getPath("test.pcm")), RECSS*BYTES_PER_SECONDS);
        //timer.scheduleAtFixedRate(clamping, 100, RECSS*1000);

        /*
        noteSession = new EvernoteSession.Builder(this)
                .setEvernoteService(EVERNOTE_SERVICE)
                .setSupportAppLinkedNotebooks(false)
                .build(CONSUMER_KEY, CONSUMER_SECRET)
                .asSingleton();

        noteSession.authenticate(this);

         */



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }


    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234
            );
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.INTERNET},
                    1234
            );
        }
    }

    /**
     * Called when the user touches the button
     */
    public void saveRecording(View view) {
        // Do something in response to button click
        TextView v = findViewById(R.id.editText2);
        v.setText("File saved!");

        try {
            File from = new File(getPath("test.pcm"));
            clamp_pcm(from);
            File to = new File(getPath("recording.wav"));
            rawToWave(from, to);
            System.out.println("Made WAV");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("FAILED WAV");
            return;
        }

        recordingProcessor.process(getPath("recording.wav"), this );

    }

    private String parseIBM_data(String json) {
        System.out.println(json);
        try {
            final JSONObject obj = new JSONObject(json);
            JSONArray array = obj.getJSONArray("results");
            JSONArray result = new JSONArray();
            for (int i=0; i<array.length(); i++) {
                String t = array.getJSONObject(i).getJSONArray("alternatives").getJSONObject(0).getString("transcript");
                System.out.println(t);
                result.put(t);
            }
            return result.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void sendToSlack(String text) {
        OkHttpClient client2 = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");

        String content = "{\"text\" : \"" + StringEscapeUtils.escapeJson(parseIBM_data(text)) +"\"}";
        RequestBody body = RequestBody.create(mediaType, content);
        Request request2 = new Request.Builder()
                .url("https://hooks.slack.com/services/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")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "*/*")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Host", "hooks.slack.com")
                .addHeader("accept-encoding", "gzip, deflate")
                .addHeader("Connection", "keep-alive")
                .addHeader("cache-control", "no-cache")
                .build();
        try {
            Response response2 = client2.newCall(request2).execute();
            System.out.println(response2);
            System.out.println(content);
            System.out.println(request2.toString());
            System.out.println(response2.message());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void saveRecordingText(String text) {
        try {
            PrintWriter out = new PrintWriter(getPath("recording.txt"));
            out.print(text);
            out.close();

            //parseIBM_data(text);
            sendToSlack(text);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);

        recording = true;
        recorder.startRecording();
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }


    public void setRecorderLength(int s) {
        this.RECSS = s;
    }


    public String getPath(String app) {
        return MainActivity.this.getApplicationContext().getFilesDir().getPath().toString() + "/" + app;
    }


    private void writeAudioDataToFile() {
        // Write the output audio in byte

        String filePath = getPath("test.pcm");
        System.out.println(filePath);
        byte[] bData = new byte[bufferSize];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read;

        while (recording) {
            // gets the voice output from microphone to byte format

            read = recorder.read(bData, 0, bufferSize);
            //System.out.println("Short writing to file" + bData);
            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer
                    assert os != null;
                    os.write(bData);
                } catch (IOException e) {
                    System.out.println("WRITING FAILED.....");
                    e.printStackTrace();
                }
            }
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            recorder.stop();
            recorder.release();
            recorder = null;
            recording = false;
            recordingThread = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    private void clamp_pcm(File rawFile) throws IOException {
        ClamperTask.clamp_pcm(rawFile, RECSS * BYTES_PER_SECONDS);
    }


    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, RECORDER_SAMPLERATE); // sample rate
            writeInt(output, BYTES_PER_SECONDS); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}