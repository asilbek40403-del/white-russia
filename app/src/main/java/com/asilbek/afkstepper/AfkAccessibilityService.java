package com.asilbek.afkstepper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;

public class AfkAccessibilityService extends AccessibilityService {

    private static AfkAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!prefs.getBoolean("running", false)) return;
            performSingleStep();
            int seconds = prefs.getInt("interval", 120);
            handler.postDelayed(this, Math.max(10, seconds) * 1000L);
        }
    };

    public static AfkAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = getSharedPreferences("afk", MODE_PRIVATE);
        if (prefs.getBoolean("running", false)) startLoop();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        stopLoop();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public void startLoop() {
        prefs.edit().putBoolean("running", true).apply();
        handler.removeCallbacks(loop);
        int seconds = prefs.getInt("interval", 120);
        handler.postDelayed(loop, Math.max(10, seconds) * 1000L);
    }

    public void stopLoop() {
        if (prefs != null) prefs.edit().putBoolean("running", false).apply();
        handler.removeCallbacks(loop);
    }

    public void performSingleStep() {
        if (prefs == null) prefs = getSharedPreferences("afk", MODE_PRIVATE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float width = dm.widthPixels;
        float height = dm.heightPixels;

        int xp = prefs.getInt("x", 12);
        int yp = prefs.getInt("y", 78);
        int dist = prefs.getInt("distance", 10);

        float startX = width * (xp / 100f);
        float startY = height * (yp / 100f);
        float endY = Math.max(0, startY - height * (dist / 100f));

        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(startX, endY);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 160);

        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke).build();

        dispatchGesture(gesture, null, null);
    }
}
