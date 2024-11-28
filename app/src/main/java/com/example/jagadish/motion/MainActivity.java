package com.example.jagadish.motion;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 10;
    private static final long CAPTURE_DURATION = 4000; // 4 seconds in milliseconds
    private static final String CHANNEL_ID = "MotionDetectionChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_PROGRESS_MAX = 100;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private File videoFile;
    private Uri videoUri;
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private MediaPlayer mediaPlayer;
    private int[] lastPixels;
    private int motionThreshold = 25;
    private int motionCount = 0;
    private static final int MOTION_DETECTION_THRESHOLD = 5;

    private NotificationManager notificationManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusText;
    private Button toggleButton;
    private boolean isDetectionActive = true;
    private MediaPlayer alertSound;

    private Runnable updateRecordingProgress = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsedTime = System.currentTimeMillis() - recordingStartTime;
                int progress = (int) ((elapsedTime * 100) / CAPTURE_DURATION);

                if (progress >= 100) {
                    stopRecording();
                } else {
                    showRecordingProgress(progress);
                    handler.postDelayed(this, 100);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        statusText = findViewById(R.id.status_text);
        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(v -> toggleDetection());

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Initialize alert sound
        // alertSound = MediaPlayer.create(this, R.raw.alert_sound);

        if (checkAndRequestPermissions()) {
            initializeCamera();
        }
    }

    private void toggleDetection() {
        isDetectionActive = !isDetectionActive;

        // Release any existing MediaPlayer instance
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            if (isDetectionActive) {
                // Create and play start sound
                mediaPlayer = MediaPlayer.create(this, R.raw.start);
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
                mediaPlayer.start();

                toggleButton.setText("Stop Detection");
                statusText.setText("Motion Detection Active");
                statusText.setTextColor(Color.GREEN);
                initializeCamera();
            } else {
                // Create and play stop sound
                mediaPlayer = MediaPlayer.create(this, R.raw.stop);
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
                mediaPlayer.start();

                toggleButton.setText("Start Detection");
                statusText.setText("Motion Detection Inactive");
                statusText.setTextColor(Color.RED);
                releaseCamera();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + e.getMessage());
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Motion Detection";
            String description = "Channel for Motion Detection notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Permissions are required for the app to function properly", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeCamera() {
        try {
            releaseCamera();
            camera = Camera.open();
            if (camera == null) {
                Log.e(TAG, "Failed to open camera");
                Toast.makeText(this, "Failed to open camera", Toast.LENGTH_LONG).show();
                return;
            }
            camera.setPreviewCallback(this);
            if (surfaceHolder != null) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    Log.e(TAG, "Error setting camera preview: " + e.getMessage());
                }
            }
            camera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize camera: " + e.getMessage());
            Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Camera setup handled in initializeCamera()
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) return;

        try {
            if (camera != null) {
                camera.stopPreview();
            }
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting camera preview: " + e.getMessage());
        }
    }
    private MotionDetection motionDetectionHelper;
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!isDetectionActive || camera == null) return;

        Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        // Initialize helper if needed
        if (motionDetectionHelper == null) {
            motionDetectionHelper = new MotionDetection(width, height);
        }

        // Convert YUV to RGB
        int[] pixels = new int[width * height];
        decodeYUV420SP(pixels, data, width, height);

        // Detect motion using improved algorithm
        if (motionDetectionHelper.detectMotion(pixels)) {
            motionCount++;
            if (motionCount >= MOTION_DETECTION_THRESHOLD && !isRecording) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Motion Detected!", Toast.LENGTH_SHORT).show();
                    playAlertSound();
                });
                startRecording();
            }
        } else {
            motionCount = Math.max(0, motionCount - 1); // Gradual decrease
        }
    }

    private void playAlertSound() {
        if (alertSound != null) {
            alertSound.start();
        }
    }

    private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            prepareVideoRecorder();
            mediaRecorder.start();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            Log.d(TAG, "Started recording");

            showRecordingProgress(0);
            handler.postDelayed(updateRecordingProgress, 100);

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            handler.removeCallbacks(updateRecordingProgress);

            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            Log.d(TAG, "Stopped recording");

            showCompletionNotification();
            addVideoToMediaStore();

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
            Toast.makeText(this, "Error stopping recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareVideoRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        if (camera != null) {
            camera.unlock();
            mediaRecorder.setCamera(camera);
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280, 720);

        videoFile = createVideoFile();
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setMaxDuration((int)CAPTURE_DURATION);
        mediaRecorder.setMaxFileSize(50000000);

        try {
            mediaRecorder.prepare();
            Log.d(TAG, "MediaRecorder prepared successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error preparing MediaRecorder: " + e.getMessage());
            throw e;
        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = "MOTION_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File video = File.createTempFile(
                videoFileName,
                ".mp4",
                storageDir
        );

        videoUri = FileProvider.getUriForFile(this,
                "com.example.jagadish.motiondetectionjaga.fileprovider",
                video);

        return video;
    }

    private void showRecordingProgress(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Recording Motion")
                .setContentText("Recording: " + progress + "%")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setProgress(NOTIFICATION_PROGRESS_MAX, progress, false);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showCompletionNotification() {
        if (videoUri == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(videoUri, "video/mp4");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Motion Recording Complete")
                .setContentText("Tap to view recorded video")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void addVideoToMediaStore() {
        if (videoFile != null && videoFile.exists()) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.TITLE, videoFile.getName());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
            values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000);

            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                String folderName = "Internal Storage > Android > data > " + getPackageName() + " > files > Movies";
                String message = "Video saved in:\n" + folderName + "\nFile: " + videoFile.getName();
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });

                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(Uri.fromFile(videoFile));
                sendBroadcast(scanIntent);

                Log.d(TAG, "Video added to MediaStore: " + uri.toString());
            } else {
                Log.e(TAG, "Failed to add video to MediaStore");
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving video to gallery", Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDetectionActive) {
            initializeCamera();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing camera: " + e.getMessage());
            } finally {
                camera = null;
            }
            Log.d(TAG, "Camera released");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();

        if (handler != null) {
            handler.removeCallbacks(updateRecordingProgress);
        }

        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder: " + e.getMessage());
            }
            mediaRecorder = null;
        }

        notificationManager.cancel(NOTIFICATION_ID);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}