package com.example.familyhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.os.Handler;
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
    private LinearLayout root;
    private static CallActivity currentActivity;

    private int matchedContact = -1;
    private boolean wasActive = false;
    private boolean wasDisconnected = false;

    private final Handler handler =
            new Handler(android.os.Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateCallScreen();
            handler.postDelayed(this, 500);
        }
    };

    private final Runnable closeAfterDisconnect = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentActivity = this;

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.rgb(20, 70, 50));
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        buildScreen();
        updateCallScreen();
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(12), dp(22), 0);
        root.setBackgroundColor(Color.rgb(240, 246, 242));

        contactImage = new ImageView(this);
        contactImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        contactImage.setBackgroundColor(Color.rgb(218, 231, 239));
        contactImage.setImageResource(
                android.R.drawable.ic_menu_camera);

        // نسبت تصویر 3:4؛ ارتفاع 4/3 عرض است.
        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(dp(225), dp(300));

        imageParams.gravity = Gravity.CENTER;
        imageParams.bottomMargin = dp(10);

        root.addView(contactImage, imageParams);

        nameText = new TextView(this);
        nameText.setText("مخاطب");
        nameText.setTextSize(28);
        nameText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameText.setGravity(Gravity.CENTER);
        nameText.setTextColor(Color.BLACK);

        root.addView(nameText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(48)));

        statusText = new TextView(this);
        statusText.setText("📞");
        statusText.setTextSize(34);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusText.setTextColor(Color.rgb(35, 85, 60));
        statusText.setContentDescription("وضعیت تماس");

        root.addView(statusText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52)));

        // دکمه پاسخ سبز؛ فقط هنگام تماس ورودی نمایش داده می‌شود.
        answerButton = new Button(this);
        answerButton.setText("📞");
        answerButton.setTextSize(48);
        answerButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        answerButton.setContentDescription("پاسخ به تماس؛ لمس و نگه‌داشتن");
        answerButton.setTextColor(Color.WHITE);
        answerButton.setBackground(
                rounded(Color.rgb(35, 150, 75), 18));

        setHoldToActivate(answerButton, 700,
                () -> FamilyInCallService.answerCall());

        LinearLayout.LayoutParams answerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(82));

        answerParams.bottomMargin = dp(10);
        root.addView(answerButton, answerParams);

        // فضای انعطاف‌پذیر؛ دکمه قطع تماس را به پایین صفحه می‌برد.
        View spacer = new View(this);

        root.addView(spacer,
                new LinearLayout.LayoutParams(
                        1, 0, 1f));

        // دکمه قطع قرمز؛ کاملاً جدا از دکمه پاسخ و چسبیده به پایین.
        hangupButton = new Button(this);
        hangupButton.setText("📴");
        hangupButton.setTextSize(48);
        hangupButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hangupButton.setContentDescription("قطع تماس؛ لمس و نگه‌داشتن");
        hangupButton.setTextColor(Color.WHITE);
        hangupButton.setBackground(
                rounded(Color.rgb(190, 45, 45), 18));

        setHoldToActivate(hangupButton, 1000,
                () -> FamilyInCallService.endCall());

        LinearLayout.LayoutParams hangupParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(86));

        hangupParams.topMargin = dp(8);
        hangupParams.bottomMargin = 0;

        root.addView(hangupButton, hangupParams);

        setContentView(root);
    }

    private void setHoldToActivate(
            Button button, long holdMs, Runnable action) {

        Handler holdHandler = new Handler(
                android.os.Looper.getMainLooper());

        final boolean[] triggered = {false};

        final Runnable trigger = () -> {
            triggered[0] = true;
            button.setAlpha(0.65f);
            action.run();
        };

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    triggered[0] = false;
                    button.setAlpha(0.8f);
                    holdHandler.removeCallbacks(trigger);
                    holdHandler.postDelayed(trigger, holdMs);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holdHandler.removeCallbacks(trigger);
                    button.setAlpha(1f);
                    return true;

                default:
                    return true;
            }
        });
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String result = value.replaceAll("[^0-9]", "");

        if (result.startsWith("0098")) {
            result = "0" + result.substring(4);
        } else if (result.startsWith("98") && result.length() >= 12) {
            result = "0" + result.substring(2);
        }

        return result;
    }

    private int findContact(
            SharedPreferences prefs, String number) {

        String normalizedNumber = normalize(number);

        if (normalizedNumber.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < 12; i++) {
            String savedNumber =
                    normalize(prefs.getString("phone_" + i, ""));

            if (savedNumber.isEmpty()) {
                continue;
            }

            if (savedNumber.equals(normalizedNumber)) {
                return i;
            }

            if (savedNumber.length() >= 10
                    && normalizedNumber.length() >= 10
                    && (savedNumber.endsWith(normalizedNumber)
                    || normalizedNumber.endsWith(savedNumber))) {
                return i;
            }
        }

        return -1;
    }

    private void showDefaultImage() {
        contactImage.setImageDrawable(null);
        contactImage.setColorFilter(null);
        contactImage.setScaleType(ImageView.ScaleType.CENTER);
        contactImage.setImageResource(
                android.R.drawable.ic_menu_camera);
    }

    private void loadContactForNumber(String number) {
        SharedPreferences prefs =
                getSharedPreferences("family_home", MODE_PRIVATE);

        int found = findContact(prefs, number);

        if (found == matchedContact) {
            return;
        }

        matchedContact = found;

        if (found >= 0) {
            String name = prefs.getString("name_" + found, "");

            nameText.setText(
                    name == null || name.trim().isEmpty()
                            ? "مخاطب" : name.trim());

            String photo = prefs.getString("photo_" + found, "");

            contactImage.setImageDrawable(null);
            contactImage.setColorFilter(null);
            contactImage.setScaleType(
                    ImageView.ScaleType.CENTER_CROP);

            boolean loaded = false;

            if (photo != null && !photo.trim().isEmpty()) {
                try {
                    contactImage.setImageURI(Uri.parse(photo));
                    loaded = contactImage.getDrawable() != null;
                } catch (Exception ignored) {
                    loaded = false;
                }
            }

            if (!loaded) {
                showDefaultImage();
            }

        } else {
            // اگر مخاطب ناشناس باشد، شماره نمایش داده می‌شود.
            nameText.setText(
                    number == null || number.trim().isEmpty()
                            ? "شماره ناشناس" : number);

            showDefaultImage();
        }
    }

    private void updateCallScreen() {
        Call call = FamilyInCallService.getCurrentCall();

        if (call == null) {
            if (wasActive && !wasDisconnected) {
                showDisconnectedState();
            } else if (!wasDisconnected) {
                finish();
            }
            return;
        }

        String number = "";

        if (call.getDetails() != null
                && call.getDetails().getHandle() != null) {

            number = call.getDetails()
                    .getHandle()
                    .getSchemeSpecificPart();
        }

        loadContactForNumber(number);

        int state = call.getState();

        if (state == Call.STATE_RINGING) {
            wasDisconnected = false;
            statusText.setText("🔔");
            statusText.setTextColor(Color.rgb(35, 85, 60));
            root.setBackgroundColor(Color.rgb(240, 246, 242));
            answerButton.setVisibility(View.VISIBLE);
            hangupButton.setVisibility(View.GONE);

        } else if (state == Call.STATE_ACTIVE) {
            wasActive = true;
            wasDisconnected = false;
            statusText.setText("📞");
            statusText.setTextColor(Color.WHITE);
            root.setBackgroundColor(Color.rgb(40, 135, 65));

            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.VISIBLE);
            hangupButton.setText("📴");

        } else if (state == Call.STATE_DIALING
                || state == Call.STATE_CONNECTING) {

            statusText.setText("📲");
            statusText.setTextColor(Color.rgb(35, 85, 60));
            root.setBackgroundColor(Color.rgb(240, 246, 242));
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.VISIBLE);
            hangupButton.setText("📴");

        } else if (state == Call.STATE_HOLDING) {
            statusText.setText("⏸️");
            statusText.setTextColor(Color.rgb(35, 85, 60));
            answerButton.setVisibility(View.GONE);
            hangupButton.setVisibility(View.VISIBLE);
            hangupButton.setText("📴");

        } else if (state == Call.STATE_DISCONNECTING
                || state == Call.STATE_DISCONNECTED) {

            wasDisconnected = true;
            showDisconnectedState();
        }
    }

    private void showDisconnectedState() {
        if (wasDisconnected) return;
        wasDisconnected = true;
        statusText.setText("📴");
        statusText.setTextColor(Color.WHITE);
        root.setBackgroundColor(Color.rgb(175, 45, 45));
        answerButton.setVisibility(View.GONE);
        hangupButton.setVisibility(View.GONE);
        handler.removeCallbacks(closeAfterDisconnect);
        handler.postDelayed(closeAfterDisconnect, 2500);
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
        handler.removeCallbacks(closeAfterDisconnect);
        if (currentActivity == this) currentActivity = null;
        super.onDestroy();
    }

    public static void refreshFromService() {
        final CallActivity activity = currentActivity;
        if (activity != null) {
            activity.runOnUiThread(activity::updateCallScreen);
        }
    }

    public static void closeCallScreen() {
        final CallActivity activity = currentActivity;
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (activity.wasActive) {
                    activity.showDisconnectedState();
                } else {
                    activity.finish();
                }
            });
        }
    }
}
