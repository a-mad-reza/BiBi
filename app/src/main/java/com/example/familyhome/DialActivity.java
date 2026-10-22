package com.example.familyhome;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DialActivity extends Activity {

    private EditText numberInput;
    private static final int CALL_PERMISSION_REQUEST = 501;

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.rgb(240, 245, 248));

        numberInput = new EditText(this);
        numberInput.setSingleLine(true);
        numberInput.setTextSize(25);
        numberInput.setGravity(Gravity.CENTER);
        numberInput.setHint("شماره تلفن");
        numberInput.setInputType(3);

        root.addView(numberInput, new LinearLayout.LayoutParams(
                -1, dp(65)));

        // اگر صفحه با شماره‌ای باز شده باشد، آن را نمایش بده.
        Intent incoming = getIntent();
        Uri data = incoming.getData();

        if (data != null && data.getScheme() != null
                && "tel".equals(data.getScheme())) {
            numberInput.setText(data.getSchemeSpecificPart());
            numberInput.setSelection(numberInput.length());
        }

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"*", "0", "#"}
        };

        for (String[] row : keys) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER);

            for (String key : row) {
                Button button = new Button(this);
                button.setText(key);
                button.setTextSize(23);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(0, dp(58), 1);

                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(button, params);

                button.setOnClickListener(v ->
                        numberInput.append(key));
            }

            root.addView(line);
        }

        Button deleteButton = new Button(this);
        deleteButton.setText("پاک کردن");

        deleteButton.setOnClickListener(v -> {
            int length = numberInput.length();

            if (length > 0) {
                numberInput.getText().delete(length - 1, length);
            }
        });

        root.addView(deleteButton, new LinearLayout.LayoutParams(
                -1, dp(48)));

        Button callButton = new Button(this);
        callButton.setText("تماس");
        callButton.setTextSize(20);
        callButton.setTextColor(Color.WHITE);
        callButton.setBackgroundColor(Color.rgb(35, 130, 75));

        root.addView(callButton, new LinearLayout.LayoutParams(
                -1, dp(58)));

        callButton.setOnClickListener(v -> makeCall());

        Button backButton = new Button(this);
        backButton.setText("بازگشت");

        root.addView(backButton, new LinearLayout.LayoutParams(
                -1, dp(48)));

        backButton.setOnClickListener(v -> finish());

        setContentView(root);
    }

    private void makeCall() {
        String number = numberInput.getText().toString().trim();

        if (number.isEmpty()) {
            numberInput.setError("شماره را وارد کنید");
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PERMISSION_REQUEST);
            return;
        }

        placeCall(number);
    }

    private void placeCall(String number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.fromParts("tel", number, null));
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "شروع تماس ممکن نشد.",
                    Toast.LENGTH_LONG).show();
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
                makeCall();
            } else {
                Toast.makeText(this,
                        "اجازه تماس داده نشد.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}