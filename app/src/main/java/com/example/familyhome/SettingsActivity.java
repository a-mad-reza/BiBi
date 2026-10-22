package com.example.familyhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;

public class SettingsActivity extends Activity {

private android.content.SharedPreferences prefs;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    prefs = getSharedPreferences("family_home", MODE_PRIVATE);

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(16, 16, 16, 16);
    content.setBackgroundColor(Color.rgb(245, 245, 235));

    TextView title = new TextView(this);
    title.setText("تنظیمات خانواده");
    title.setTextSize(26);
    title.setTextColor(Color.BLACK);
    title.setGravity(Gravity.CENTER);
    content.addView(title);

    ScrollView scroll = new ScrollView(this);
    LinearLayout list = new LinearLayout(this);
    list.setOrientation(LinearLayout.VERTICAL);

    String[] defaults = {
        "مامان", "بابا", "فرزند ۱", "فرزند ۲",
        "نوه ۱", "نوه ۲", "نوه ۳", "نوه ۴",
        "خانواده ۹", "خانواده ۱۰", "خانواده ۱۱", "خانواده ۱۲"
    };

    for (int i = 0; i < 12; i++) {
        final int index = i;

        TextView label = new TextView(this);
        label.setText("فرد " + (i + 1));
        label.setTextSize(18);
        label.setTextColor(Color.BLACK);
        list.addView(label);

        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("نام (مثلاً مامان)");
        name.setText(prefs.getString("name" + i, defaults[i]));
        list.addView(name);

        EditText phone = new EditText(this);
        phone.setSingleLine(true);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        phone.setHint("شماره تلفن");
        phone.setText(prefs.getString("phone" + i, ""));
        list.addView(phone);

        Button save = new Button(this);
        save.setText("ذخیره فرد " + (i + 1));
        save.setOnClickListener(v -> {
            prefs.edit()
                .putString("name" + index, name.getText().toString().trim())
                .putString("phone" + index, phone.getText().toString().trim())
                .apply();

            new AlertDialog.Builder(this)
                .setMessage("اطلاعات ذخیره شد.")
                .setPositiveButton("باشه", null)
                .show();
        });
        list.addView(save);
    }

    scroll.addView(list);
    content.addView(scroll, new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

    Button back = new Button(this);
    back.setText("بازگشت");
    back.setOnClickListener(v -> finish());
    content.addView(back);

    setContentView(content);
}

}