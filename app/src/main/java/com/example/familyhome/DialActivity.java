package com.example.familyhome;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DialActivity extends Activity {

private EditText phoneNumber;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    createInterface();
    handleIncomingIntent();
}

private void createInterface() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER);
    root.setPadding(24, 24, 24, 24);

    TextView title = new TextView(this);
    title.setText("تماس با خانواده");
    title.setTextSize(28);
    title.setGravity(Gravity.CENTER);
    root.addView(title, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

    phoneNumber = new EditText(this);
    phoneNumber.setTextSize(24);
    phoneNumber.setSingleLine(true);
    phoneNumber.setHint("شماره تلفن");
    phoneNumber.setInputType(3);
    phoneNumber.setGravity(Gravity.CENTER);
    root.addView(phoneNumber, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

    Button callButton = new Button(this);
    callButton.setText("تماس");
    callButton.setTextSize(22);
    root.addView(callButton, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

    callButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            makeCall();
        }
    });

    setContentView(root);
}

private void handleIncomingIntent() {
    Intent intent = getIntent();

    if (intent == null) {
        return;
    }

    Uri data = intent.getData();

    if (data != null && data.getScheme() != null
            && data.getScheme().equals("tel")) {
        String number = data.getSchemeSpecificPart();

        if (number != null) {
            phoneNumber.setText(number);
            phoneNumber.setSelection(phoneNumber.length());
        }
    }
}

private void makeCall() {
    String number = phoneNumber.getText().toString().trim();

    if (number.isEmpty()) {
        Toast.makeText(this, "لطفاً شماره تلفن را وارد کنید",
                Toast.LENGTH_SHORT).show();
        return;
    }

    Intent callIntent = new Intent(
            Intent.ACTION_CALL,
            Uri.fromParts("tel", number, null));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {

        requestPermissions(
                new String[]{Manifest.permission.CALL_PHONE},
                100);

        return;
    }

    try {
        startActivity(callIntent);
    } catch (SecurityException e) {
        Toast.makeText(this, "اجازه تماس صادر نشده است",
                Toast.LENGTH_LONG).show();
    } catch (Exception e) {
        Toast.makeText(this, "برقراری تماس امکان‌پذیر نیست",
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

    if (requestCode == 100
            && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        makeCall();
    } else if (requestCode == 100) {
        Toast.makeText(this, "برای تماس باید اجازه تماس را بدهید",
                Toast.LENGTH_LONG).show();
    }
}

}