package com.example.familyhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CallActivity extends Activity {

    private TextView statusText;
    private TextView nameText;
    private ImageView contactImage;
    private Button answerButton;
    private Button hangupButton;
    private int matchedContact = -2;

    private final android.os.Handler handler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateCallScreen();
            handler.postDelayed(this, 500);
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
        root.setPadding(dp(22), dp(18), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(240, 246, 242));

        contactImage = new ImageView(this);
        contactImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        contactImage.setBackgroundColor(Color.rgb(218, 231, 239));
        contactImage.setImageResource(android.R.drawable.ic_menu_camera);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(dp(240), dp(240));
        imageParams.gravity = Gravity.CENTER;
        imageParams.bottomMargin = dp(18);
        root.addView(contactImage, imageParams);

        nameText = new TextView(this);
        nameText.setText("مخاطب");
        nameText.setTextSize(28);
        nameText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameText.setGravity(Gravity.CENTER);
        nameText.setTextColor(Color.BLACK);

        root.addView(nameText,
                new LinearLayout.LayoutParams(-1, dp(52)));

        statusText = new TextView(this);
        statusText.setText("وضعیت تماس");
        statusText.setTextSize(20);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.rgb(35, 85, 60));

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(-1, dp(54));
        statusParams.bottomMargin = dp(18);
        root.addView(statusText, statusParams);

        answerButton = new Button(this);
        answerButton.setText("پاسخ دادن");
        answerButton.setTextSize(24);
        answerButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        answerButton.setTextColor(Color.WHITE);
        answerButton.setBackgroundColor(Color.rgb(35, 150, 75));
        answerButton.setOnClickListener(
                v -> FamilyInCallService.answerCall()
        );

        root.addView(answerButton,
                new LinearLayout.LayoutParams(-1, dp(82)));

        hangupButton = new Button(this);
        hangupButton.setText("قطع تماس");
        hangupButton.setTextSize(24);
        hangupButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hangupButton.setTextColor(Color.WHITE);
        hangupButton.setBackgroundColor(Color.rgb(190, 45, 45));
        hangupButton.setOnClickListener(
                v -> FamilyInCallService.endCall()
        );

        LinearLayout.LayoutParams hangupParams =
                new LinearLayout.LayoutParams(-1, dp(82));
        hangupParams.topMargin = dp(12);
        root.addView(hangupButton, hangupParams);

        setContentView(root);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9+]", "");
    }

    private void loadContactForNumber(String number) {
        SharedPreferences prefs =
                getSharedPreferences("family_home", MODE_PRIVATE);

        String normalizedNumber = normalize(number);
        int found = -1;

        for (int i = 0; i < 12; i++) {
            String savedNumber =
                    normalize(prefs.getString("phone_" + i, ""));

            if (!savedNumber.isEmpty()
                    && !normalizedNumber.isEmpty()
                    && (savedNumber.equals(normalizedNumber)
                    || savedNumber.endsWith(normalizedNumber)
                    || normalizedNumber.endsWith(savedNumber))) {
                found = i;
                break;
            }
        }

        if (found == matchedContact) {
            return;
        }

        matchedContact = found;

        if (found >= 0) {
            String name = prefs.getString("name_" + found, "");

            nameText.setText(
                    name == null || name.trim().isEmpty()
                            ? "مخاطب"
                            : name.trim()
            );

            String photo = prefs.getString("photo_" + found, "");

            contactImage.setImageDrawable(null);

            boolean photoLoaded = false;

            if (photo != null && !photo.isEmpty()) {
                try {
                    contactImage.setImageURI(Uri.parse(photo));
                    photoLoaded = contactImage.getDrawable() != null;
                } catch (Exception ignored) {
                    photoLoaded = false;
                }
            }

            if (!photoLoaded) {
                contactImage.setImageResource(
                        android.R.drawable.ic_menu_camera
                );
            }

        } else {
            nameText.setText(
                    number == null || number.isEmpty()
                            ? "شماره ناشناس"
                            : number
            );

            contactImage.setImageResource(
                    android.R.drawable.ic_menu_camera
            );
        }
    }

    private void updateCallScreen() {
        Call call = FamilyInCallService.getCurrentCall();

        if (call == null) {
            finish();
            return;
        }

        String number = "";

        if (call.getDetails() != null
                && call.getDetails().getHandle() != null) {
            number = call.getDetails().getHandle()
                    .getSchemeSpecificPart();
        }

        loadContactForNumber(number);

        int state = call.getState();

        if (state == Call.STATE_RINGING) {
            statusText.setText("تماس ورودی");
            answerButton.setVisibility(View.VISIBLE);
            hangupButton.setVisibility(View.GONE);

        } else if (state == Call.STATE_ACTIVE) {
            statusText.setText("تماس برقرار است");
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.VISIBLE);
            hangupButton.setText("قطع تماس");

        } else if (state == Call.STATE_DIALING
                || state == Call.STATE_CONNECTING) {
            statusText.setText("در حال برقراری تماس...");
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.VISIBLE);
            hangupButton.setText("لغو تماس");

        } else if (state == Call.STATE_HOLDING) {
            statusText.setText("تماس در انتظار است");
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.VISIBLE);
            hangupButton.setText("قطع تماس");

        } else if (state == Call.STATE_DISCONNECTING) {
            statusText.setText("در حال قطع تماس...");
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.GONE);

        } else {
            statusText.setText("پایان تماس");
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.GONE);
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
    }

    public static void closeCallScreen() {
    }
}