package com.example.familyhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.ContentUris;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

    private CheckBox autoSpeakerCheckBox;

    private static final int PHOTO_REQUEST = 200;
    private static final int DEFAULT_DIALER_REQUEST = 300;
    private static final int CONTACT_PICK_REQUEST = 400;
    private static final int CONTACT_PERMISSION_REQUEST = 401;

    private int pendingContactIndex = -1;

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "family_home",
                MODE_PRIVATE
        );

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
        root.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(24)
        );

        root.setBackgroundColor(Color.rgb(240, 245, 248));

        scroll.addView(root);

        TextView title = makeText(
                "تنظیمات خانواده",
                24,
                Color.rgb(30, 65, 90)
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                )
        );

        TextView instruction = makeText(
                "برای هر دکمه مخاطب را از دفترچه تلفن انتخاب کنید؛ " +
                        "امکان ویرایش دستی هم وجود دارد.",
                15,
                Color.DKGRAY
        );

        instruction.setTypeface(Typeface.DEFAULT);
        root.addView(instruction);

        Button dialerButton = new Button(this);

        dialerButton.setText(
                "انتخاب برنامه به‌عنوان تلفن پیش‌فرض"
        );

        dialerButton.setTextSize(17);
        dialerButton.setTextColor(Color.WHITE);

        dialerButton.setBackground(
                rounded(Color.rgb(35, 90, 145), 14)
        );

        LinearLayout.LayoutParams dialerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(60)
                );

        dialerParams.topMargin = dp(16);

        root.addView(dialerButton, dialerParams);

        dialerButton.setOnClickListener(
                v -> requestDefaultDialer()
        );

        TextView dialerInfo = makeText(
                "برای نمایش صفحه تماس اختصاصی، ممکن است لازم باشد " +
                        "این برنامه را به‌عنوان تلفن پیش‌فرض انتخاب کنید.",
                14,
                Color.DKGRAY
        );

        root.addView(dialerInfo);

        addDivider(root);

        for (int i = 0; i < 12; i++) {
            addContactForm(root, i);
        }

        addDivider(root);

        TextView holdTitle = makeText(
                "مدت نگه‌داشتن عکس برای تماس",
                19,
                Color.rgb(30, 65, 90)
        );

        root.addView(holdTitle);

        TextView holdValue = makeText(
                "",
                17,
                Color.DKGRAY
        );

        root.addView(holdValue);

        SeekBar holdSeek = new SeekBar(this);

        holdSeek.setMax(2300);

        holdSeek.setProgress(
                Math.max(
                        0,
                        Math.min(2300, holdDuration - 700)
                )
        );

        root.addView(holdSeek);

        updateHoldValue(holdValue);

        holdSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        holdDuration = progress + 700;
                        updateHoldValue(holdValue);
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );

        addDivider(root);

        TextView speakerTitle = makeText(
                "تنظیمات صدای تماس",
                19,
                Color.rgb(30, 65, 90)
        );

        root.addView(speakerTitle);

        autoSpeakerCheckBox = new CheckBox(this);

        autoSpeakerCheckBox.setText(
                "فعال‌سازی خودکار بلندگو با صدای زیاد هنگام پاسخ"
        );

        autoSpeakerCheckBox.setTextSize(17);
        autoSpeakerCheckBox.setTextColor(Color.DKGRAY);

        autoSpeakerCheckBox.setChecked(
                prefs.getBoolean("auto_speaker", true)
        );

        root.addView(autoSpeakerCheckBox);

        addDivider(root);

        TextView batteryTitle = makeText(
                "هشدار کم بودن باتری",
                19,
                Color.rgb(30, 65, 90)
        );

        root.addView(batteryTitle);

        TextView batteryValue = makeText(
                "",
                17,
                Color.DKGRAY
        );

        root.addView(batteryValue);

        SeekBar batterySeek = new SeekBar(this);

        batterySeek.setMax(50);

        batterySeek.setProgress(
                Math.max(
                        0,
                        Math.min(50, batteryThreshold)
                )
        );

        root.addView(batterySeek);

        updateBatteryValue(batteryValue);

        batterySeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        batteryThreshold = progress;
                        updateBatteryValue(batteryValue);
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );

        Button saveButton = new Button(this);

        saveButton.setText("ذخیره همه تنظیمات");
        saveButton.setTextSize(20);
        saveButton.setTextColor(Color.WHITE);

        saveButton.setBackground(
                rounded(Color.rgb(35, 110, 75), 14)
        );

        LinearLayout.LayoutParams saveParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(60)
                );

        saveParams.topMargin = dp(24);

        root.addView(saveButton, saveParams);

        saveButton.setOnClickListener(
                v -> saveAll()
        );

        Button backButton = new Button(this);

        backButton.setText("بازگشت");

        root.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        setContentView(scroll);
    }

    private void updateHoldValue(TextView view) {
        view.setText(
                String.format(
                        java.util.Locale.getDefault(),
                        "%.1f ثانیه",
                        holdDuration / 1000.0
                )
        );
    }

    private void updateBatteryValue(TextView view) {
        view.setText(
                "هشدار در " + batteryThreshold + " درصد"
        );
    }

    private void requestDefaultDialer() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            Toast.makeText(
                    this,
                    "این قابلیت به نسخه اندروید بالاتری نیاز دارد.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {
            TelecomManager telecomManager =
                    (TelecomManager) getSystemService(
                            TELECOM_SERVICE
                    );

            if (telecomManager == null) {
                Toast.makeText(
                        this,
                        "مدیریت تماس در این گوشی در دسترس نیست.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String currentPackage =
                    telecomManager.getDefaultDialerPackage();

            if (getPackageName().equals(currentPackage)) {
                Toast.makeText(
                        this,
                        "این برنامه هم‌اکنون تلفن پیش‌فرض است.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            Intent intent = new Intent(
                    TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
            );

            intent.putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    getPackageName()
            );

            startActivityForResult(
                    intent,
                    DEFAULT_DIALER_REQUEST
            );

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "امکان باز کردن تنظیم تلفن پیش‌فرض وجود ندارد.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void addContactForm(
            LinearLayout root,
            int index
    ) {
        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(12)
        );

        card.setBackground(
                rounded(Color.WHITE, 14)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.topMargin = dp(10);

        root.addView(card, cardParams);

        TextView title = makeText(
                "مخاطب " + (index + 1),
                19,
                Color.rgb(35, 75, 105)
        );

        card.addView(title);

        Button chooseContactButton = new Button(this);

        chooseContactButton.setText(
                "انتخاب از دفترچه تلفن"
        );

        card.addView(
                chooseContactButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        chooseContactButton.setOnClickListener(
                v -> chooseContact(index)
        );

        names[index] = new EditText(this);

        names[index].setSingleLine(true);
        names[index].setTextSize(18);
        names[index].setHint("نام مخاطب");

        names[index].setText(
                prefs.getString("name_" + index, "")
        );

        card.addView(
                names[index],
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                )
        );

        phones[index] = new EditText(this);

        phones[index].setSingleLine(true);
        phones[index].setTextSize(18);
        phones[index].setHint("شماره تلفن");
        phones[index].setInputType(3);

        phones[index].setText(
                prefs.getString("phone_" + index, "")
        );

        card.addView(
                phones[index],
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                )
        );

        Button photoButton = new Button(this);

        photoButton.setText(
                "انتخاب عکس از گالری"
        );

        card.addView(
                photoButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        String savedPhoto = prefs.getString(
                "photo_" + index,
                ""
        );

        photoLabels[index] = makeText(
                savedPhoto.isEmpty()
                        ? "عکسی انتخاب نشده"
                        : "برای تغییر عکس، دکمه بالا را بزنید",
                13,
                Color.DKGRAY
        );

        card.addView(photoLabels[index]);

        final int contactIndex = index;

        photoButton.setOnClickListener(v -> {
            selectedPhotoIndex = contactIndex;

            Intent intent = new Intent(
                    Intent.ACTION_OPEN_DOCUMENT
            );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            intent.setType("image/*");

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            );

            startActivityForResult(
                    intent,
                    PHOTO_REQUEST
            );
        });
    }

    private void chooseContact(int index) {
        pendingContactIndex = index;

        if (android.os.Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                        android.Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            android.Manifest.permission.READ_CONTACTS
                    },
                    CONTACT_PERMISSION_REQUEST
            );

            return;
        }

        launchContactPicker();
    }

    private void launchContactPicker() {
        try {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            );

            startActivityForResult(
                    intent,
                    CONTACT_PICK_REQUEST
            );

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "دفترچه تلفن در دسترس نیست.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == CONTACT_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                launchContactPicker();

            } else {
                Toast.makeText(
                        this,
                        "برای انتخاب مخاطب، اجازه دسترسی به مخاطبین لازم است.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void readPickedContact(Uri uri) {
        if (uri == null || pendingContactIndex < 0) {
            return;
        }

        Cursor cursor = null;

        try {
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            };

            cursor = getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String number = cursor.getString(1);
                long contactId = cursor.getLong(2);

                names[pendingContactIndex].setText(
                        name == null ? "" : name
                );

                phones[pendingContactIndex].setText(
                        number == null ? "" : number
                );

                Uri photoUri = Uri.withAppendedPath(
                        ContentUris.withAppendedId(
                                ContactsContract.Contacts.CONTENT_URI,
                                contactId
                        ),
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                );

                prefs.edit()
                        .putLong(
                                "contact_id_" + pendingContactIndex,
                                contactId
                        )
                        .putString(
                                "photo_" + pendingContactIndex,
                                photoUri.toString()
                        )
                        .apply();

                photoLabels[pendingContactIndex].setText(
                        "مخاطب و عکس از دفترچه تلفن انتخاب شد"
                );

                Toast.makeText(
                        this,
                        "مخاطب انتخاب شد",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "خواندن اطلاعات مخاطب ناموفق بود.",
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void addDivider(LinearLayout root) {
        View divider = new View(this);

        divider.setBackgroundColor(
                Color.LTGRAY
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        params.topMargin = dp(22);
        params.bottomMargin = dp(18);

        root.addView(divider, params);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == CONTACT_PICK_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            readPickedContact(data.getData());

        } else if (requestCode == PHOTO_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null
                && selectedPhotoIndex >= 0) {

            Uri uri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            prefs.edit()
                    .putString(
                            "photo_" + selectedPhotoIndex,
                            uri.toString()
                    )
                    .apply();

            photoLabels[selectedPhotoIndex].setText(
                    "عکس انتخاب شد"
            );

            Toast.makeText(
                    this,
                    "عکس انتخاب شد",
                    Toast.LENGTH_SHORT
            ).show();

        } else if (requestCode == DEFAULT_DIALER_REQUEST) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                TelecomManager telecomManager =
                        (TelecomManager) getSystemService(
                                TELECOM_SERVICE
                        );

                if (telecomManager != null
                        && getPackageName().equals(
                                telecomManager.getDefaultDialerPackage()
                        )) {

                    Toast.makeText(
                            this,
                            "برنامه به‌عنوان تلفن پیش‌فرض انتخاب شد.",
                            Toast.LENGTH_LONG
                    ).show();

                } else {
                    Toast.makeText(
                            this,
                            "برنامه به‌عنوان تلفن پیش‌فرض انتخاب نشد.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

    private void saveAll() {
        SharedPreferences.Editor editor = prefs.edit();

        for (int i = 0; i < 12; i++) {
            editor.putString(
                    "name_" + i,
                    names[i].getText().toString().trim()
            );

            editor.putString(
                    "phone_" + i,
                    phones[i].getText().toString().trim()
            );
        }

        editor.putInt(
                "hold_ms",
                holdDuration
        );

        editor.putInt(
                "battery_threshold",
                batteryThreshold
        );

        if (autoSpeakerCheckBox != null) {
            editor.putBoolean(
                    "auto_speaker",
                    autoSpeakerCheckBox.isChecked()
            );
        }

        editor.apply();

        Toast.makeText(
                this,
                "تنظیمات ذخیره شد",
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
}