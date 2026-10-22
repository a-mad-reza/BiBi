package com.example.familyhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

public class CallActivity extends Activity {

    private static CallActivity activeActivity;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private ImageView photoView;
    private TextView statusText;
    private Button answerButton;
    private Button endButton;
    private Button speakerButton;

    private String phoneNumber = "";

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activeActivity = this;

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        buildScreen();
        refreshScreen();
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private TextView makeText(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button makeButton(String text, int color, int height) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(21);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackground(rounded(color, 22));

        button.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(height)));

        return button;
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(22), dp(20), dp(22), dp(20));
        root.setBackgroundColor(Color.rgb(20, 30, 40));

        photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setBackground(rounded(Color.rgb(65, 85, 100), 100));
        photoView.setClipToOutline(true);

        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                dp(210), dp(210));
        photoParams.gravity = Gravity.CENTER;
        photoParams.bottomMargin = dp(25);

        root.addView(photoView, photoParams);

        statusText = makeText("در حال برقراری تماس...", 22, Color.WHITE);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(65));
        statusParams.bottomMargin = dp(20);
        root.addView(statusText, statusParams);

        answerButton = makeButton("🟢 پاسخ", Color.rgb(25, 155, 75), 76);
        root.addView(answerButton);

        answerButton.setOnClickListener(v ->
                FamilyInCallService.answerCall());

        endButton = makeButton("🔴 پایان تماس", Color.rgb(190, 40, 45), 76);
        root.addView(endButton);

        endButton.setOnClickListener(v ->
                FamilyInCallService.endCall());

        speakerButton = makeButton("🔊 بلندگو", Color.rgb(45, 95, 145), 64);
        LinearLayout.LayoutParams speakerParams =
                (LinearLayout.LayoutParams) speakerButton.getLayoutParams();
        speakerParams.topMargin = dp(12);
        root.addView(speakerButton, speakerParams);

        speakerButton.setOnClickListener(v ->
                FamilyInCallService.toggleSpeaker());

        setContentView(root);
    }

    private void refreshScreen() {
        Call call = FamilyInCallService.getCurrentCall();

        if (call == null) {
            statusText.setText("تماس پایان یافته");
            handler.postDelayed(this::finish, 1200);
            return;
        }

        int state = call.getState();

        if (state == Call.STATE_RINGING) {
            statusText.setText("تماس ورودی");
            answerButton.setVisibility(View.VISIBLE);
            endButton.setVisibility(View.GONE);
            speakerButton.setVisibility(View.GONE);
        } else if (state == Call.STATE_DIALING
                || state == Call.STATE_CONNECTING
                || state == Call.STATE_SELECT_PHONE_ACCOUNT) {
            statusText.setText("در حال برقراری تماس...");
            answerButton.setVisibility(View.GONE);
            endButton.setVisibility(View.VISIBLE);
            speakerButton.setVisibility(View.GONE);
        } else if (state == Call.STATE_ACTIVE) {
            statusText.setText("تماس برقرار است");
            answerButton.setVisibility(View.GONE);
            endButton.setVisibility(View.VISIBLE);
            speakerButton.setVisibility(View.VISIBLE);

            speakerButton.setText(
                    FamilyInCallService.isSpeakerOn()
                            ? "🔊 خاموش کردن بلندگو"
                            : "🔊 روشن کردن بلندگو");
        } else if (state == Call.STATE_DISCONNECTED) {
            statusText.setText("تماس پایان یافت");
            answerButton.setVisibility(View.GONE);
            endButton.setVisibility(View.GONE);
            speakerButton.setVisibility(View.GONE);
            handler.postDelayed(this::finish, 1200);
        }

        updateContactPhoto();
    }

    private void updateContactPhoto() {
        Call call = FamilyInCallService.getCurrentCall();
        if (call == null) return;

        String number = "";

        try {
            if (call.getDetails() != null
                    && call.getDetails().getHandle() != null) {
                number = call.getDetails().getHandle().getSchemeSpecificPart();
            }
        } catch (Exception ignored) {}

        phoneNumber = number;

        android.content.SharedPreferences prefs =
                getSharedPreferences("family_home", MODE_PRIVATE);

        for (int i = 0; i < 12; i++) {
            String savedNumber = prefs.getString("phone_" + i, "");

            if (!number.isEmpty() && number.equals(savedNumber)) {
                String photo = prefs.getString("photo_" + i, "");

                if (!photo.isEmpty()) {
                    try {
                        photoView.setImageURI(Uri.parse(photo));
                        if (photoView.getDrawable() != null) {
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                break;
            }
        }

        photoView.setImageResource(android.R.drawable.sym_def_app_icon);
    }

    public static void refreshFromService() {
        if (activeActivity != null) {
            activeActivity.runOnUiThread(
                    activeActivity::refreshScreen);
        }
    }

    public static void closeCallScreen() {
        if (activeActivity != null) {
            activeActivity.runOnUiThread(() -> {
                activeActivity.statusText.setText("تماس پایان یافت");
                activeActivity.handler.postDelayed(
                        activeActivity::finish, 500);
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (activeActivity == this) {
            activeActivity = null;
        }
        super.onDestroy();
    }
}