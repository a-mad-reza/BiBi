package com.example.familyhome;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private TextView batteryText;

    private int tapCount = 0;
    private long firstTapTime = 0;

    private String pendingPhone = null;

    private static final int CALL_PERMISSION_REQUEST = 100;

    private int dp(float value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "family_home", MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(
                    Color.rgb(24, 45, 65));

            getWindow().setNavigationBarColor(
                    Color.rgb(24, 45, 65));
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemBars();
        buildHome();
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideSystemBars();

        if (prefs != null && batteryText != null) {
            buildHome();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == CALL_PERMISSION_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                if (pendingPhone != null) {
                    String phone = pendingPhone;
                    pendingPhone = null;
                    makeCall(phone);
                }

            } else {
                pendingPhone = null;

                Toast.makeText(
                        this,
                        "برای تماس باید اجازه تماس را فعال کنید.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private GradientDrawable rounded(
            int color, int radius) {

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));

        return bg;
    }

    private TextView makeText(
            String text, int size, int color) {

        TextView v = new TextView(this);

        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD);

        return v;
    }

    private void buildHome() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(3), dp(3), dp(3), dp(3));
        root.setBackgroundColor(Color.BLACK);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        batteryText = makeText("", 16, Color.WHITE);
        batteryText.setPadding(dp(8), 0, dp(8), 0);
        batteryText.setBackground(rounded(Color.rgb(37, 75, 105), 8));
        batteryText.setOnClickListener(v -> batteryTapped());
        topBar.addView(batteryText, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView lockButton = makeText("🔒", 25, Color.WHITE);
        lockButton.setContentDescription("قفل صفحه");
        lockButton.setBackground(rounded(Color.rgb(37, 75, 105), 50));
        LinearLayout.LayoutParams lockParams = new LinearLayout.LayoutParams(dp(58), dp(46));
        lockParams.leftMargin = dp(8);
        topBar.addView(lockButton, lockParams);
        lockButton.setOnClickListener(v -> lockScreen());

        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setRowCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);

        LinearLayout.LayoutParams gridParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f);

        gridParams.topMargin = dp(2);

        root.addView(grid, gridParams);

        for (int i = 0; i < 12; i++) {
            grid.addView(makeContactTile(i));
        }

        setContentView(root);
        updateBattery();
    }

    private View makeContactTile(int index) {

        LinearLayout tile = new LinearLayout(this);

        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(0, 0, 0, 0);
        tile.setBackground(
                rounded(Color.rgb(25, 25, 25), 3));

        if (Build.VERSION.SDK_INT >= 21) {
            tile.setClipToOutline(true);
        }

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 0;
        params.height = 0;

        params.columnSpec =
                GridLayout.spec(index % 3, 1f);

        params.rowSpec =
                GridLayout.spec(index / 3, 1f);

        params.setMargins(
                dp(2), dp(2), dp(2), dp(2));

        tile.setLayoutParams(params);

        String photo = prefs.getString(
                "photo_" + index, "");

        ImageView image = new ImageView(this);

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP);

        image.setBackgroundColor(
                Color.rgb(218, 231, 239));

        tile.addView(
                image,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

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
            image.setImageDrawable(null);

            image.setBackground(
                    rounded(Color.rgb(218, 231, 239), 3));

            image.setScaleType(
                    ImageView.ScaleType.CENTER);

            image.setImageResource(
                    android.R.drawable.ic_menu_camera);

            image.setColorFilter(
                    Color.rgb(35, 75, 105));
        }

        tile.setOnTouchListener(
                new View.OnTouchListener() {

            private final Handler touchHandler =
                    new Handler(Looper.getMainLooper());

            private Runnable longPress;

            @Override
            public boolean onTouch(
                    View v, MotionEvent event) {

                if (event.getAction()
                        == MotionEvent.ACTION_DOWN) {

                    longPress = () -> startContactCall(index);

                    int duration = prefs.getInt(
                            "hold_ms", 1800);

                    touchHandler.postDelayed(
                            longPress,
                            Math.max(700, duration));

                    return true;
                }

                if (event.getAction()
                        == MotionEvent.ACTION_UP
                        || event.getAction()
                        == MotionEvent.ACTION_CANCEL) {

                    if (longPress != null) {
                        touchHandler.removeCallbacks(
                                longPress);
                    }

                    return true;
                }

                return true;
            }
        });

        return tile;
    }

    private void lockScreen() {
        ComponentName admin = new ComponentName(this, FamilyDeviceAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm != null && dpm.isAdminActive(admin)) {
            try {
                dpm.lockNow();
            } catch (SecurityException e) {
                Toast.makeText(this, "قفل صفحه در این گوشی مجاز نیست.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("فعال‌کردن قفل صفحه")
                .setMessage("برای خاموش‌کردن صفحه با این دکمه، یک‌بار اجازهٔ مدیریت قفل صفحه را فعال کن. بعد از آن با دکمهٔ فیزیکی گوشی می‌توانی صفحه را روشن کنی.")
                .setNegativeButton("لغو", null)
                .setPositiveButton("فعال‌سازی", (dialog, which) -> {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "این اجازه فقط برای قفل‌کردن صفحه با دکمهٔ داخل برنامه استفاده می‌شود.");
                    startActivity(intent);
                }).show();
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
        box.setPadding(
                dp(20), dp(12), dp(20), dp(8));

        TextView passwordHint = makeText(
                "رمز تنظیمات: 0000",
                19,
                Color.rgb(35, 75, 105));

        box.addView(passwordHint);

        EditText input = new EditText(this);

        input.setHint("رمز را وارد کنید");

        input.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType
                        .TYPE_NUMBER_VARIATION_PASSWORD);

        input.setGravity(Gravity.CENTER);

        box.addView(input);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("تنظیمات")
                        .setView(box)
                        .setPositiveButton("ورود", null)
                        .setNegativeButton(
                                "بازگشت",
                                (d, w) -> d.dismiss())
                        .create();

        dialog.setOnShowListener(d -> {

            final Runnable timeout = dialog::dismiss;

            handler.postDelayed(timeout, 15000);

            dialog.setOnDismissListener(
                    x -> handler.removeCallbacks(timeout));

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if ("0000".equals(
                        input.getText().toString())) {

                    handler.removeCallbacks(timeout);
                    dialog.dismiss();

                    startActivity(new Intent(
                            MainActivity.this,
                            SettingsActivity.class));

                } else {
                    input.setError("رمز اشتباه است");
                    input.setText("");
                }
            });
        });

        dialog.show();
    }

    private void startContactCall(int index) {

        String phone = prefs.getString(
                "phone_" + index, "").trim();

        if (phone.isEmpty()) {
            Toast.makeText(
                    this,
                    "شماره این مخاطب هنوز تنظیم نشده است",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        makeCall(phone);
    }

    private void makeCall(String phone) {

        if (phone == null || phone.trim().isEmpty()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            pendingPhone = phone;

            requestPermissions(
                    new String[]{
                            Manifest.permission.CALL_PHONE
                    },
                    CALL_PERMISSION_REQUEST);

            return;
        }

        try {
            Intent intent =
                    new Intent(Intent.ACTION_CALL);

            intent.setData(
                    Uri.fromParts(
                            "tel", phone.trim(), null));

            startActivity(intent);

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "اجازه تماس داده نشده است.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "شروع تماس ممکن نشد. شماره و تنظیمات تماس را بررسی کنید.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void updateBattery() {

        if (batteryText == null) {
            return;
        }

        BatteryManager bm = (BatteryManager)
                getSystemService(Context.BATTERY_SERVICE);

        int level = 0;

        if (bm != null) {
            level = bm.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }

        if (level < 0 || level > 100) {
            level = 0;
        }

        int threshold = prefs.getInt(
                "battery_threshold", 20);

        batteryText.setText("🔋 " + level + "%");

        if (level <= threshold) {
            batteryText.setBackground(
                    rounded(Color.rgb(180, 45, 45), 8));
        } else {
            batteryText.setBackground(
                    rounded(Color.rgb(37, 75, 105), 8));
        }
    }
}