package com.ibmhackaton.watchasaid;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.TimerTask;

public class ClamperTask extends TimerTask {

    File file;
    int length;

    ClamperTask(File file, int length) {
        super();
        this.file = file;
        this.length = length;
    }

    public void run() {
        try {
            clamp_pcm(file, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clamp_pcm(File rawFile, int length) throws IOException {
        int end = (int) rawFile.length();
        byte[] rawData = new byte[end];
        try (DataInputStream input = new DataInputStream(new FileInputStream(rawFile))) {
            input.read(rawData);
        }

        if (end < length)
            return;

        rawFile.delete();

        byte[] slice = Arrays.copyOfRange(rawData, end-length, end);
        FileOutputStream output = new FileOutputStream(rawFile.getAbsolutePath(), false);
        output.write(slice);
        output.close();
    }

}
