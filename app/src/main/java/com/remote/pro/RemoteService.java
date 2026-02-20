package com.remote.pro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(9999)) {
                while (true) {
                    try (Socket s = server.accept(); DataInputStream in = new DataInputStream(s.getInputStream())) {
                        int len = in.readUnsignedShort();
                        byte[] data = new byte[len];
                        in.readFully(data);
                        String cmd = new String(data, "UTF-8");

                        if (cmd.startsWith("CLICK:")) {
                            String[] p = cmd.split(":");
                            DisplayMetrics m = getResources().getDisplayMetrics();
                            // Exact mapping chahe screen seedhi ho ya terhi
                            performClick(Float.parseFloat(p[1]) * m.widthPixels, Float.parseFloat(p[2]) * m.heightPixels);
                        } else if (cmd.startsWith("SWIPE:")) {
                            performSwipe(cmd.split(":")[1]);
                        } else if (cmd.startsWith("CMD:")) {
                            String c = cmd.split(":")[1];
                            if (c.equals("BACK")) performGlobalAction(GLOBAL_ACTION_BACK);
                            if (c.equals("HOME")) performGlobalAction(GLOBAL_ACTION_HOME);
                            if (c.equals("RECENT")) performGlobalAction(GLOBAL_ACTION_RECENTS);
                        } else if (cmd.startsWith("TYPE:")) {
                            inputText(cmd.substring(5));
                        }
                    } catch(Exception ignored){}
                }
            } catch (Exception e) {}
        }).start();
    }

    private void performClick(float x, float y) {
        Path p = new Path(); p.moveTo(x, y);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 50));
        dispatchGesture(b.build(), null, null);
    }

    private void performSwipe(String dir) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        float cx = m.widthPixels / 2f, cy = m.heightPixels / 2f;
        Path p = new Path(); p.moveTo(cx, cy);
        if (dir.equals("UP")) p.lineTo(cx, cy - 500);
        else if (dir.equals("DOWN")) p.lineTo(cx, cy + 500);
        else if (dir.equals("LEFT")) p.lineTo(cx - 400, cy);
        else if (dir.equals("RIGHT")) p.lineTo(cx + 400, cy);
        
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 200));
        dispatchGesture(b.build(), null, null);
    }

    private void inputText(String t) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null) {
                Bundle a = new Bundle(); 
                a.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t);
                focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, a);
            }
        }
    }
}
