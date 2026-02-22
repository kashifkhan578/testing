package com.remote.pro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
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
                            // Yahan RealMetrics use ki gayi hain exact touch map karne ke liye
                            DisplayMetrics m = new DisplayMetrics();
                            ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(m);
                            performClick(Float.parseFloat(p[1]) * m.widthPixels, Float.parseFloat(p[2]) * m.heightPixels);
                        } else if (cmd.startsWith("SWIPE:")) {
                            String dir = cmd.split(":")[1];
                            boolean handledAsCursor = false;
                            
                            AccessibilityNodeInfo root = getRootInActiveWindow();
                            if (root != null) {
                                AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                                if (focus != null) {
                                    Bundle args = new Bundle();
                                    args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
                                    if (dir.equals("LEFT")) {
                                        focus.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, args);
                                        handledAsCursor = true;
                                    } else if (dir.equals("RIGHT")) {
                                        focus.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, args);
                                        handledAsCursor = true;
                                    }
                                }
                            }
                            
                            if (!handledAsCursor) {
                                performSwipe(dir);
                            }
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
        DisplayMetrics m = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(m);
        float safeX = Math.max(10, Math.min(x, m.widthPixels - 10));
        float safeY = Math.max(10, Math.min(y, m.heightPixels - 10));
        
        Path p = new Path(); 
        p.moveTo(safeX, safeY);
        p.lineTo(safeX + 1, safeY + 1); 
        
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 100)); 
        dispatchGesture(b.build(), null, null);
    }

    private void performSwipe(String dir) {
        DisplayMetrics m = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(m);
        float cx = m.widthPixels / 2f;
        float cy = m.heightPixels / 2f;
        Path p = new Path(); 
        
        // Distance 400 kar diya taake properly scroll/fling ho
        int distance = 400; 
        
        if (dir.equals("UP")) { p.moveTo(cx, cy); p.lineTo(cx, cy + distance); } 
        else if (dir.equals("DOWN")) { p.moveTo(cx, cy); p.lineTo(cx, cy - distance); } 
        else if (dir.equals("LEFT")) { p.moveTo(cx, cy); p.lineTo(cx + distance, cy); } 
        else if (dir.equals("RIGHT")) { p.moveTo(cx, cy); p.lineTo(cx - distance, cy); } 
        
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 150));
        dispatchGesture(b.build(), null, null);
    }

    private void inputText(String newChar) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null) {
                CharSequence current = focus.getText();
                String text = "";
                
                if (current != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!focus.isShowingHintText()) {
                            text = current.toString();
                        }
                    } else {
                        text = current.toString();
                    }
                }
                
                // Cursor ki location pata karo
                int start = focus.getTextSelectionStart();
                if (start < 0 || start > text.length()) start = text.length();
                
                if (newChar.equals("BACKSPACE")) {
                    if (start > 0) {
                        // Sirf cursor se pehle wala character remove karo
                        text = text.substring(0, start - 1) + text.substring(start);
                        start--; // Cursor ko aik step peche lao
                    }
                } else if (newChar.equals("SPACE")) {
                    text = text.substring(0, start) + " " + text.substring(start);
                    start++;
                } else {
                    text = text.substring(0, start) + newChar + text.substring(start);
                    start += newChar.length();
                }

                Bundle a = new Bundle(); 
                a.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, a);
                
                // Type karne ke baad cursor ko wapis sahi jagah set karo
                Bundle selectionArgs = new Bundle();
                selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
                selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, start);
                focus.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs);
            }
        }
    }
}
