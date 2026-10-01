package com.asilbek.afkstepper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText xPercent, yPercent, distancePercent, intervalSeconds;
    private TextView status;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("afk", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(box);

        TextView title = new TextView(this);
        title.setText("AFK Stepper");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("\nРаз в заданный интервал приложение делает короткий свайп вверх в области виртуального джойстика.\n\n1) Включи службу специальных возможностей.\n2) Сохрани координаты.\n3) Проверь тестовый шаг.\n4) Нажми «Старт / Пауза».\n");
        info.setTextSize(16);
        box.addView(info);

        xPercent = addField(box, "X джойстика, % ширины", "12");
        yPercent = addField(box, "Y джойстика, % высоты", "78");
        distancePercent = addField(box, "Длина движения вверх, % высоты", "10");
        intervalSeconds = addField(box, "Интервал, секунд", "120");
        loadPrefs();

        Button save = addButton(box, "Сохранить настройки");
        save.setOnClickListener(v -> savePrefs());

        Button accessibility = addButton(box, "Открыть специальные возможности");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button test = addButton(box, "Тестовый шаг");
        test.setOnClickListener(v -> {
            savePrefs();
            AfkAccessibilityService service = AfkAccessibilityService.getInstance();
            if (service == null) {
                Toast.makeText(this, "Сначала включи AFK Stepper в специальных возможностях.", Toast.LENGTH_LONG).show();
            } else {
                service.performSingleStep();
                Toast.makeText(this, "Тестовый жест отправлен.", Toast.LENGTH_SHORT).show();
            }
        });

        Button start = addButton(box, "Старт / Пауза");
        start.setOnClickListener(v -> {
            savePrefs();
            AfkAccessibilityService service = AfkAccessibilityService.getInstance();
            if (service == null) {
                Toast.makeText(this, "Сначала включи AFK Stepper в специальных возможностях.", Toast.LENGTH_LONG).show();
                return;
            }
            boolean next = !prefs.getBoolean("running", false);
            prefs.edit().putBoolean("running", next).apply();
            if (next) service.startLoop(); else service.stopLoop();
            updateStatus();
        });

        status = new TextView(this);
        status.setTextSize(18);
        status.setPadding(0, dp(20), 0, dp(20));
        box.addView(status);

        TextView note = new TextView(this);
        note.setText("Если жест не попадает в джойстик, измени X/Y и проверь через «Тестовый шаг». Автоматизация может быть запрещена правилами конкретной игры.");
        note.setTextSize(14);
        box.addView(note);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    private void loadPrefs() {
        xPercent.setText(String.valueOf(prefs.getInt("x", 12)));
        yPercent.setText(String.valueOf(prefs.getInt("y", 78)));
        distancePercent.setText(String.valueOf(prefs.getInt("distance", 10)));
        intervalSeconds.setText(String.valueOf(prefs.getInt("interval", 120)));
    }

    private void savePrefs() {
        try {
            int x = clamp(Integer.parseInt(xPercent.getText().toString()), 0, 100);
            int y = clamp(Integer.parseInt(yPercent.getText().toString()), 0, 100);
            int d = clamp(Integer.parseInt(distancePercent.getText().toString()), 1, 50);
            int i = clamp(Integer.parseInt(intervalSeconds.getText().toString()), 10, 3600);
            prefs.edit().putInt("x", x).putInt("y", y).putInt("distance", d).putInt("interval", i).apply();
            Toast.makeText(this, "Настройки сохранены.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Проверь числа в настройках.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        boolean enabled = AfkAccessibilityService.getInstance() != null;
        boolean running = prefs.getBoolean("running", false);
        status.setText("Служба: " + (enabled ? "включена" : "выключена") + "\nАвтошаг: " + (running ? "РАБОТАЕТ" : "пауза"));
    }

    private EditText addField(LinearLayout parent, String hint, String fallback) {
        TextView label = new TextView(this);
        label.setText(hint);
        label.setTextSize(15);
        label.setPadding(0, dp(10), 0, dp(4));
        parent.addView(label);

        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setText(fallback);
        edit.setPadding(dp(12), dp(10), dp(12), dp(10));
        parent.addView(edit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return edit;
    }

    private Button addButton(LinearLayout parent, String text) {
        Button b = new Button(this);
        b.setText(text);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(10);
        parent.addView(b, p);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
