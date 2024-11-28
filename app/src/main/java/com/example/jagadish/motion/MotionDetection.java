package com.example.jagadish.motion;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MotionDetection extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final int BUFFER_SIZE = 3; // Number of frames to buffer
    private static final float MOTION_REGION_THRESHOLD = 0.02f; // 2% change required in a region
    private static final int GRID_SIZE = 10; // Split frame into 10x10 grid

    private Queue<int[]> frameBuffer;
    private int frameWidth;
    private int frameHeight;
    private int regionWidth;
    private int regionHeight;
    private boolean[][] activeRegions;

    public MotionDetection(int width, int height) {
        this.frameWidth = width;
        this.frameHeight = height;
        this.regionWidth = width / GRID_SIZE;
        this.regionHeight = height / GRID_SIZE;
        this.frameBuffer = new LinkedList<>();
        this.activeRegions = new boolean[GRID_SIZE][GRID_SIZE];
    }

    public boolean detectMotion(int[] currentFrame) {
        // Add frame to buffer
        frameBuffer.offer(currentFrame);
        if (frameBuffer.size() > BUFFER_SIZE) {
            frameBuffer.poll();
        }

        // Wait until buffer is full
        if (frameBuffer.size() < BUFFER_SIZE) {
            return false;
        }

        // Get average frame from buffer for noise reduction
        int[] averageFrame = getAverageFrame();

        // Reset active regions
        for (int i = 0; i < GRID_SIZE; i++) {
            Arrays.fill(activeRegions[i], false);
        }

        // Detect motion in regions
        int activeRegionCount = 0;

        for (int gridY = 0; gridY < GRID_SIZE; gridY++) {
            for (int gridX = 0; gridX < GRID_SIZE; gridX++) {
                if (detectRegionMotion(averageFrame, currentFrame, gridX, gridY)) {
                    activeRegions[gridY][gridX] = true;
                    activeRegionCount++;
                }
            }
        }

        // Check if connected regions show significant motion
        return hasSignificantMotion(activeRegionCount);
    }

    private int[] getAverageFrame() {
        int[] averageFrame = new int[frameWidth * frameHeight];
        int frames = frameBuffer.size();

        for (int[] frame : frameBuffer) {
            for (int i = 0; i < frame.length; i++) {
                averageFrame[i] += frame[i] / frames;
            }
        }

        return averageFrame;
    }

    private boolean detectRegionMotion(int[] averageFrame, int[] currentFrame, int gridX, int gridY) {
        int startX = gridX * regionWidth;
        int startY = gridY * regionHeight;
        int changedPixels = 0;
        int totalPixels = regionWidth * regionHeight;

        for (int y = startY; y < startY + regionHeight && y < frameHeight; y++) {
            for (int x = startX; x < startX + regionWidth && x < frameWidth; x++) {
                int idx = y * frameWidth + x;
                int diff = Math.abs(averageFrame[idx] - currentFrame[idx]);

                // Extract only luminance for comparison
                int luminanceDiff = (diff >> 16) & 0xff; // Using red channel as approximation
                if (luminanceDiff > 25) { // Threshold for pixel change
                    changedPixels++;
                }
            }
        }

        return (float) changedPixels / totalPixels > MOTION_REGION_THRESHOLD;
    }

    private boolean hasSignificantMotion(int activeRegionCount) {
        // Require at least 3 connected active regions
        if (activeRegionCount < 3) {
            return false;
        }

        // Check for connected regions
        for (int y = 0; y < GRID_SIZE - 1; y++) {
            for (int x = 0; x < GRID_SIZE - 1; x++) {
                if (activeRegions[y][x] &&
                        (activeRegions[y+1][x] || activeRegions[y][x+1])) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }
}