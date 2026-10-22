package com.example.familyhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private EditText[] names = new EditText[12];
    private EditText[] phones = new EditText[12];
    private TextView[] photoLabels = new TextView[12];

    private int selectedPhotoIndex = -1;
    private int holdDuration = 1800;
    private int batteryThreshold = 20;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("family_home", MODE_PRIVATE);
        holdDuration = prefs.getInt("hold_ms", 1800);
        batteryThreshold = prefs.getInt("battery_threshold", 20);

        buildSettings();
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

    private void buildSettings() {
        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(24));
        root.setBackgroundColor(Color.rgb(240, 245, 248));

        scroll.addView(root);

        TextView title = makeText("تنظیمات خانواده", 24,
                Color.rgb(30, 65, 90));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(55)));

        TextView instruction = makeText(
                "نام و شماره را وارد کنید و برای انتخاب عکس دکمهٔ عکس را بزنید.",
                15, Color.DKGRAY);
        instruction.setTypeface(Typeface.DEFAULT);
        root.addView(instruction);

        for (int i = 0; i < 12; i++) {
            addContactForm(root, i);
        }

        addDivider(root);

        TextView holdTitle = makeText("مدت نگه‌داشتن عکس برای تماس", 19,
                Color.rgb(30, 65, 90));
        root.addView(holdTitle);

        TextView holdValue = makeText("", 17, Color.DKGRAY);
        root.addView(holdValue);

        SeekBar holdSeek = new SeekBar(this);
        holdSeek.setMax(2300);
        holdSeek.setProgress(holdDuration - 700);
        root.addView(holdSeek);

        holdValue.setText((holdDuration / 1000.0) + " ثانیه");

        holdSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                holdDuration = progress + 700;
                holdValue.setText(String.format("%.1f ثانیه",
                        holdDuration / 1000.0));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        addDivider(root);

        TextView batteryTitle = makeText("هشدار کم بودن باتری", 19,
                Color.rgb(30, 65, 90));
        root.addView(batteryTitle);

        TextView batteryValue = makeText("", 17, Color.DKGRAY);
        root.addView(batteryValue);

        SeekBar batterySeek = new SeekBar(this);
        batterySeek.setMax(50);
        batterySeek.setProgress(batteryThreshold);
        root.addView(batterySeek);

        batteryValue.setText("هشدار در " + batteryThreshold + " درصد");

        batterySeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                batteryThreshold = progress;
                batteryValue.setText("هشدار در " + batteryThreshold + " درصد");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button saveButton = new Button(this);
        saveButton.setText("ذخیره همه تنظیمات");
        saveButton.setTextSize(20);
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackground(rounded(Color.rgb(35, 110, 75), 14));

        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        saveParams.topMargin = dp(24);
        root.addView(saveButton, saveParams);

        saveButton.setOnClickListener(v -> saveAll());

        Button backButton = new Button(this);
        backButton.setText("بازگشت");
        root.addView(backButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        backButton.setOnClickListener(v -> finish());

        setContentView(scroll);
    }

    private void addContactForm(LinearLayout root, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(12));
        card.setBackground(rounded(Color.WHITE, 14));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(10);
        root.addView(card, cardParams);

        TextView title = makeText("مخاطب " + (index + 1), 19,
                Color.rgb(35, 75, 105));
        card.addView(title);

        names[index] = new EditText(this);
        names[index].setSingleLine(true);
        names[index].setTextSize(18);
        names[index].setHint("نام مخاطب");
        names[index].setText(prefs.getString("name_" + index, ""));
        card.addView(names[index], new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        phones[index] = new EditText(this);
        phones[index].setSingleLine(true);
        phones[index].setTextSize(18);
        phones[index].setHint("شماره تلفن");
        phones[index].setInputType(3);
        phones[index].setText(prefs.getString("phone_" + index, ""));
        card.addView(phones[index], new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        Button photoButton = new Button(this);
        photoButton.setText("انتخاب عکس از گالری");
        card.addView(photoButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        photoLabels[index] = makeText(
                prefs.getString("photo_" + index, "").isEmpty()
                        ? "عکسی انتخاب نشده"
                        : "برای تغییر عکس، دکمه بالا را بزنید",
                13, Color.DKGRAY);
        card.addView(photoLabels[index]);

        final int contactIndex = index;

        photoButton.setOnClickListener(v -> {
            selectedPhotoIndex = contactIndex;

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            startActivityForResult(intent, 200);
        });
    }

    private void addDivider(LinearLayout root) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.LTGRAY);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(22);
        params.bottomMargin = dp(18);

        root.addView(divider, params);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == RESULT_OK
                && data != null && data.getData() != null
                && selectedPhotoIndex >= 0) {
            Uri uri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}

            prefs.edit()
                    .putString("photo_" + selectedPhotoIndex, uri.toString())
                    .apply();

            photoLabels[selectedPhotoIndex].setText("عکس انتخاب شد");
            Toast.makeText(this, "عکس انتخاب شد", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAll() {
        SharedPreferences.Editor editor = prefs.edit();

        for (int i = 0; i < 12; i++) {
            editor.putString("name_" + i,
                    names[i].getText().toString().trim());
            editor.putString("phone_" + i,
                    phones[i].getText().toString().trim());
        }

        editor.putInt("hold_ms", holdDuration);
        editor.putInt("battery_threshold", batteryThreshold);
        editor.apply();

        Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_LONG).show();
        finish();
    }
}