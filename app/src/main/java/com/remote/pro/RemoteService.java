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
        DisplayMetrics m = getResources().getDisplayMetrics();
        float safeX = Math.max(0, Math.min(x, m.widthPixels - 1));
        float safeY = Math.max(0, Math.min(y, m.heightPixels - 1));
        
        Path p = new Path(); p.moveTo(safeX, safeY);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 50));
        dispatchGesture(b.build(), null, null);
    }

    private void performSwipe(String dir) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        float cx = m.widthPixels / 2f, cy = m.heightPixels / 2f;
        Path p = new Path(); 
        
        // Exact 4-Way Scroll Math (Reverse finger movement for correct scroll)
        if (dir.equals("UP")) { p.moveTo(cx, cy - 100); p.lineTo(cx, cy + 400); } // Scroll Up = Finger moves Down
        else if (dir.equals("DOWN")) { p.moveTo(cx, cy + 300); p.lineTo(cx, cy - 300); } // Scroll Down = Finger moves Up
        else if (dir.equals("LEFT")) { p.moveTo(cx - 200, cy); p.lineTo(cx + 300, cy); } // Scroll Left = Finger moves Right
        else if (dir.equals("RIGHT")) { p.moveTo(cx + 300, cy); p.lineTo(cx - 300, cy); } // Scroll Right = Finger moves Left
        
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 150));
        dispatchGesture(b.build(), null, null);
    }

    private void inputText(String newChar) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null) {
                // Sahi Append Logic (Ab pehla word delete nahi hoga)
                CharSequence current = focus.getText();
                String text = (current != null ? current.toString() : "");
                
                if (newChar.equals("BACKSPACE")) {
                    if (text.length() > 0) text = text.substring(0, text.length() - 1);
                } else if (newChar.equals("SPACE")) {
                    text = text + " ";
                } else {
                    text = text + newChar;
                }

                Bundle a = new Bundle(); 
                a.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, a);
            }
        }
    }
}
