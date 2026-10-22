package com.example.familyhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {

private SharedPreferences prefs;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    prefs = getSharedPreferences("family_home", MODE_PRIVATE);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(12, 12, 12, 12);
    root.setBackgroundColor(Color.rgb(245, 245, 235));

    TextView battery = new TextView(this);
    battery.setTextSize(20);
    battery.setGravity(Gravity.CENTER);
    battery.setTextColor(Color.rgb(0, 100, 0));
    battery.setPadding(8, 8, 8, 16);

    int level = 100;
    android.content.IntentFilter filter =
            new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent status = registerReceiver(null, filter);
    if (status != null) {
        level = status.getIntExtra("level", 100);
    }

    battery.setText("شارژ گوشی: " + level + "٪");
    if (level <= 20) {
        battery.setTextColor(Color.RED);
    }
    root.addView(battery);

    TextView title = new TextView(this);
    title.setText("تماس با خانواده");
    title.setTextSize(26);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);
    root.addView(title);

    GridLayout grid = new GridLayout(this);
    grid.setColumnCount(3);
    grid.setRowCount(4);

    for (int i = 0; i < 12; i++) {
        final int index = i;
        String name = prefs.getString("name" + i,
                new String[]{"مامان", "بابا", "فرزند ۱", "فرزند ۲",
                "نوه ۱", "نوه ۲", "نوه ۳", "نوه ۴",
                "خانواده ۹", "خانواده ۱۰", "خانواده ۱۱",
                "خانواده ۱۲"}[i]);

        Button button = new Button(this);
        button.setText(name);
        button.setTextSize(20);
        button.setAllCaps(false);
        button.setMinHeight(100);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(i % 3, 1f);
        params.rowSpec = GridLayout.spec(i / 3, 1f);
        params.setMargins(4, 4, 4, 4);
        grid.addView(button, params);

        button.setOnTouchListener(new View.OnTouchListener() {
            private android.os.Handler handler = new android.os.Handler();
            private Runnable callAction;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    callAction = new Runnable() {
                        @Override
                        public void run() {
                            showCallDialog(index);
                        }
                    };
                    handler.postDelayed(callAction, 2000);
                    return true;
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP
                        || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    if (callAction != null) {
                        handler.removeCallbacks(callAction);
                    }
                    return true;
                }
                return true;
            }
        });
    }

    root.addView(grid, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

    Button settings = new Button(this);
    settings.setText("تنظیمات");
    settings.setTextSize(18);
    settings.setOnClickListener(v -> {
        startActivity(new Intent(this, SettingsActivity.class));
    });
    root.addView(settings);

    setContentView(root);
}

private void showCallDialog(int index) {
    String name = prefs.getString("name" + index, "عضو خانواده");
    String phone = prefs.getString("phone" + index, "");

    if (phone.trim().isEmpty()) {
        new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage("ابتدا شماره این فرد را در تنظیمات وارد کنید.")
                .setPositiveButton("باشه", null)
                .show();
        return;
    }

    new AlertDialog.Builder(this)
            .setTitle("تماس با " + name)
            .setMessage("با این شماره تماس گرفته شود؟\n" + phone)
            .setNegativeButton("لغو", null)
            .setPositiveButton("تماس", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + phone));
                try {
                    startActivity(intent);
                } catch (SecurityException e) {
                    Intent dial = new Intent(Intent.ACTION_DIAL);
                    dial.setData(Uri.parse("tel:" + phone));
                    startActivity(dial);
                }
            })
            .show();
}

                                 }
