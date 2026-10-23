<activity
    android:name=".DialActivity"
    android:exported="true"
    android:screenOrientation="portrait">

    <!-- پشتیبانی از درخواست باز کردن شماره‌گیر -->
    <intent-filter>
        <action android:name="android.intent.action.DIAL" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>

    <!-- پشتیبانی از شماره‌هایی که با tel ارسال می‌شوند -->
    <intent-filter>
        <action android:name="android.intent.action.DIAL" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="tel" />
    </intent-filter>

</activity>