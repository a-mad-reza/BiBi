package com.example.familyhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CallActivity extends Activity {

    private TextView statusText;
    private TextView numberText;
    private Button answerButton;
    private Button hangupButton;
    private Button speakerButton;

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateCallScreen();
            handler.postDelayed(this, 700);
        }
    };

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.rgb(20, 70, 50));
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        buildScreen();
        updateCallScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(240, 246, 242));

        statusText = new TextView(this);
        statusText.setText("وضعیت تماس");
        statusText.setTextSize(24);
        statusText.setTypeface(null, Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.rgb(35, 85, 60));

        root.addView(statusText, new LinearLayout.LayoutParams(
                -1, dp(65)));

        numberText = new TextView(this);
        numberText.setText("شماره تماس");
        numberText.setTextSize(25);
        numberText.setGravity(Gravity.CENTER);
        numberText.setTextColor(Color.BLACK);
        numberText.setPadding(0, dp(12), 0, dp(24));

        root.addView(numberText, new LinearLayout.LayoutParams(
                -1, dp(90)));

        answerButton = new Button(this);
        answerButton.setText("پاسخ دادن");
        answerButton.setTextSize(23);
        answerButton.setTextColor(Color.WHITE);
        answerButton.setBackgroundColor(Color.rgb(35, 150, 75));
        answerButton.setOnClickListener(v ->
                FamilyInCallService.answerCall());

        root.addView(answerButton, new LinearLayout.LayoutParams(
                -1, dp(80)));

        speakerButton = new Button(this);
        speakerButton.setText("بلندگو");
        speakerButton.setTextSize(21);
        speakerButton.setOnClickListener(v ->
                FamilyInCallService.toggleSpeaker());

        root.addView(speakerButton, new LinearLayout.LayoutParams(
                -1, dp(65)));

        hangupButton = new Button(this);
        hangupButton.setText("قطع تماس");
        hangupButton.setTextSize(23);
        hangupButton.setTextColor(Color.WHITE);
        hangupButton.setBackgroundColor(Color.rgb(190, 45, 45));
        hangupButton.setOnClickListener(v ->
                FamilyInCallService.endCall());

        root.addView(hangupButton, new LinearLayout.LayoutParams(
                -1, dp(80)));

        setContentView(root);
    }

    private void updateCallScreen() {
        Call call = FamilyInCallService.getCurrentCall();

        if (call == null) {
            finish();
            return;
        }

        int state = call.getState();

        String number = "";
        if (call.getDetails() != null
                && call.getDetails().getHandle() != null) {
            number = call.getDetails().getHandle()
                    .getSchemeSpecificPart();
        }

        numberText.setText(
                number == null || number.isEmpty()
                        ? "شماره ناشناس"
                        : number
        );

        if (state == Call.STATE_RINGING) {
            statusText.setText("تماس ورودی");
            answerButton.setVisibility(android.view.View.VISIBLE);
            speakerButton.setVisibility(android.view.View.GONE);
            hangupButton.setText("رد تماس");

        } else if (state == Call.STATE_DIALING
                || state == Call.STATE_CONNECTING) {
            statusText.setText("در حال برقراری تماس...");
            answerButton.setVisibility(android.view.View.GONE);
            speakerButton.setVisibility(android.view.View.GONE);
            hangupButton.setText("لغو تماس");

        } else if (state == Call.STATE_ACTIVE) {
            statusText.setText("تماس برقرار است");
            answerButton.setVisibility(android.view.View.GONE);
            speakerButton.setVisibility(android.view.View.VISIBLE);
            hangupButton.setText("قطع تماس");

            speakerButton.setText(
                    FamilyInCallService.isSpeakerOn()
                            ? "خاموش کردن بلندگو"
                            : "روشن کردن بلندگو"
            );

        } else if (state == Call.STATE_HOLDING) {
            statusText.setText("تماس در انتظار است");
            answerButton.setVisibility(android.view.View.GONE);
            speakerButton.setVisibility(android.view.View.VISIBLE);
            hangupButton.setText("قطع تماس");

        } else if (state == Call.STATE_DISCONNECTING) {
            statusText.setText("در حال قطع تماس...");
            answerButton.setVisibility(android.view.View.GONE);
            speakerButton.setVisibility(android.view.View.GONE);
            hangupButton.setText("قطع تماس");
        } else {
            statusText.setText("وضعیت تماس");
            answerButton.setVisibility(android.view.View.GONE);
            speakerButton.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refreshRunnable);
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refreshRunnable);
        super.onDestroy();
    }

    public static void refreshFromService() {
        // صفحه وضعیت تماس را به‌صورت دوره‌ای به‌روزرسانی می‌کند.
    }

    public static void closeCallScreen() {
        // صفحه در به‌روزرسانی بعدی بسته می‌شود.
    }
}