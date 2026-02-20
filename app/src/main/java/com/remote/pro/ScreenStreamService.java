package com.remote.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ScreenStreamService extends Service {
    private MediaProjection mp;
    private VirtualDisplay vd;
    private ImageReader ir;
    private boolean isStreaming = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("SC", "Screen Capture", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        startForeground(1, new Notification.Builder(this, "SC").setContentTitle("Screen Sharing is Active").build());

        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mp = mpm.getMediaProjection(intent.getIntExtra("RESULT_CODE", -1), intent.getParcelableExtra("DATA"));
        startServer();
        return START_NOT_STICKY;
    }

    private void startServer() {
        int w = 720; int h = 1280;
        ir = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
        vd = mp.createVirtualDisplay("Capture", w, h, 300, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, ir.getSurface(), null, null);

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(8080)) {
                while (isStreaming) {
                    Socket socket = server.accept();
                    OutputStream os = socket.getOutputStream();
                    os.write(("HTTP/1.0 200 OK\r\nContent-Type: multipart/x-mixed-replace; boundary=--BoundaryString\r\n\r\n").getBytes());

                    while (isStreaming && !socket.isClosed()) {
                        Image image = ir.acquireLatestImage();
                        if (image != null) {
                            Image.Plane[] planes = image.getPlanes();
                            ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * w;

                            Bitmap bitmap = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(buffer);
                            image.close();

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            // JPEG Quality 30 for max speed and zero latency
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
                            byte[] imgData = baos.toByteArray();

                            os.write(("--BoundaryString\r\nContent-Type: image/jpeg\r\nContent-Length: " + imgData.length + "\r\n\r\n").getBytes());
                            os.write(imgData);
                            os.write("\r\n".getBytes());
                            os.flush();
                        }
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isStreaming = false; if(vd!=null) vd.release(); if(mp!=null) mp.stop(); super.onDestroy(); }
}
