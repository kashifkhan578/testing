package com.remote.pro;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
    @Override protected void onServiceConnected() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(9999)) {
                while (true) {
                    try (Socket s = server.accept(); DataInputStream in = new DataInputStream(s.getInputStream())) {
                        String cmd = in.readUTF();
                        if (cmd.startsWith("CLICK:")) {
                            String[] p = cmd.split(":");
                            performClick(Float.parseFloat(p[1]), Float.parseFloat(p[2]));
                        } else if (cmd.startsWith("TYPE:")) {
                            inputText(cmd.substring(5));
                        }
                    }
                }
            } catch (Exception e) {}
        }).start();
    }
    private void performClick(float x, float y) {
        Path path = new Path(); path.moveTo(x, y);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        dispatchGesture(b.build(), null, null);
    }
    private void inputText(String t) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null) {
                Bundle a = new Bundle(); a.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t);
                focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, a);
            }
        }
    }
}