package com.example.familyhome;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView batteryText;
    private int tapCount = 0;
    private long firstTapTime = 0;
    private boolean callStarted = false;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("family_home", MODE_PRIVATE);

        getWindow().setStatusBarColor(Color.rgb(24, 45, 65));
        getWindow().setNavigationBarColor(Color.rgb(24, 45, 65));

        buildHome();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs != null && batteryText != null) {
            updateBattery();
            buildHome();
        }
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private TextView makeText(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.setBackgroundColor(Color.rgb(240, 245, 248));

        batteryText = makeText("", 18, Color.WHITE);
        batteryText.setPadding(dp(12), dp(10), dp(12), dp(10));
        batteryText.setBackground(rounded(Color.rgb(37, 75, 105), 14));
        batteryText.setOnClickListener(v -> batteryTapped());

        root.addView(batteryText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView hint = makeText("برای تماس، عکس فرد را چند لحظه نگه دارید", 15,
                Color.rgb(40, 60, 75));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        hintParams.topMargin = dp(4);
        root.addView(hint, hintParams);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setRowCount(4);
        grid.setUseDefaultMargins(false);

        for (int i = 0; i < 12; i++) {
            grid.addView(makeContactTile(i), new GridLayout.LayoutParams());
        }

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        updateBattery();
    }

    private View makeContactTile(int index) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(5), dp(3), dp(5));
        tile.setBackground(rounded(Color.WHITE, 16));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(index % 3, 1f);
        params.rowSpec = GridLayout.spec(index / 3, 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.setLayoutParams(params);

        String name = prefs.getString("name_" + index, "");
        String photo = prefs.getString("photo_" + index, "");

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(rounded(Color.rgb(218, 231, 239), 50));
        image.setClipToOutline(true);

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                dp(68), dp(68));
        imageParams.gravity = Gravity.CENTER;

        boolean hasPhoto = false;

        if (photo != null && !photo.isEmpty()) {
            try {
                image.setImageURI(Uri.parse(photo));
                hasPhoto = image.getDrawable() != null;
            } catch (Exception ignored) {
                hasPhoto = false;
            }
        }

        if (!hasPhoto) {
            TextView initials = makeText(
                    name.trim().isEmpty() ? String.valueOf(index + 1)
                            : name.trim().substring(0, 1),
                    30, Color.rgb(35, 75, 105));
            initials.setBackground(rounded(Color.rgb(218, 231, 239), 50));
            tile.addView(initials, imageParams);
        } else {
            tile.addView(image, imageParams);
        }

        String displayName = name.trim().isEmpty()
                ? "مخاطب " + (index + 1) : name.trim();

        TextView label = makeText(displayName, 14, Color.rgb(30, 45, 55));
        label.setMaxLines(2);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        labelParams.topMargin = dp(4);
        tile.addView(label, labelParams);

        tile.setOnTouchListener(new View.OnTouchListener() {
            private final Handler touchHandler = new Handler(Looper.getMainLooper());
            private boolean triggered = false;
            private Runnable longPress;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    triggered = false;

                    longPress = () -> {
                        triggered = true;
                        showCallDialog(index);
                    };

                    int duration = prefs.getInt("hold_ms", 1800);
                    touchHandler.postDelayed(longPress, Math.max(700, duration));
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (longPress != null) {
                        touchHandler.removeCallbacks(longPress);
                    }

                    if (!triggered && event.getAction() == MotionEvent.ACTION_UP) {
                        Toast.makeText(MainActivity.this,
                                "برای تماس، کمی بیشتر نگه دارید",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                return true;
            }
        });

        return tile;
    }

    private void batteryTapped() {
        long now = System.currentTimeMillis();

        if (now - firstTapTime > 3000) {
            tapCount = 0;
        }

        if (tapCount == 0) {
            firstTapTime = now;
        }

        tapCount++;

        if (tapCount >= 5) {
            tapCount = 0;
            showPasswordDialog();
        }
    }

    private void showPasswordDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(8));

        TextView passwordHint = makeText("رمز تنظیمات: 0000", 19,
                Color.rgb(35, 75, 105));
        box.addView(passwordHint);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("رمز را وارد کنید");
        input.setInputType( android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setGravity(Gravity.CENTER);
        box.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تنظیمات")
                .setView(box)
                .setPositiveButton("ورود", null)
                .setNegativeButton("بازگشت", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            final Runnable timeout = dialog::dismiss;
            handler.postDelayed(timeout, 15000);

            dialog.setOnDismissListener(x -> handler.removeCallbacks(timeout));

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if ("0000".equals(input.getText().toString())) {
                    handler.removeCallbacks(timeout);
                    dialog.dismiss();
                    startActivity(new Intent(MainActivity.this,
                            SettingsActivity.class));
                } else {
                    input.setError("رمز اشتباه است");
                    input.setText("");
                }
            });
        });

        dialog.show();
    }

    private void showCallDialog(int index) {
        String name = prefs.getString("name_" + index, "");
        String phone = prefs.getString("phone_" + index, "");

        if (name.trim().isEmpty()) {
            name = "مخاطب " + (index + 1);
        }

        if (phone.trim().isEmpty()) {
            Toast.makeText(this, "ابتدا شماره این مخاطب را در تنظیمات وارد کنید",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("تماس با " + name)
                .setMessage("آیا می‌خواهید تماس بگیرید؟")
                .setNegativeButton("لغو", null)
                .setPositiveButton("تماس", (dialog, which) -> makeCall(phone))
                .show();
    }

    private void makeCall(String phone) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + Uri.encode(phone)));

            if (Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(Manifest.permission.CALL_PHONE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.CALL_PHONE}, 100);
                Toast.makeText(this,
                        "اجازه تماس را تأیید کنید و دوباره تلاش کنید",
                        Toast.LENGTH_LONG).show();
                return;
            }

            startActivity(intent);
        } catch (Exception e) {
            Intent dial = new Intent(Intent.ACTION_DIAL);
            dial.setData(Uri.parse("tel:" + Uri.encode(phone)));
            startActivity(dial);
        }
    }

    private void updateBattery() {
        if (batteryText == null) return;

        BatteryManager bm = (BatteryManager)
                getSystemService(Context.BATTERY_SERVICE);
        int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if (level < 0 || level > 100) {
            level = 0;
        }

        int threshold = prefs.getInt("battery_threshold", 20);

        batteryText.setText("باتری گوشی: " + level + "%");

        if (level <= threshold) {
            batteryText.setBackground(rounded(Color.rgb(180, 45, 45), 14));
        } else {
            batteryText.setBackground(rounded(Color.rgb(37, 75, 105), 14));
        }
    }
}