package com.example.familyhome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.os.Build;
import android.media.AudioManager;

public class FamilyInCallService extends InCallService {

    private static FamilyInCallService instance;
    private static Call currentCall;

    private final Call.Callback callCallback = new Call.Callback() {

        @Override
        public void onStateChanged(Call call, int state) {
            CallActivity.refreshFromService();

            if (state == Call.STATE_DIALING
                    || state == Call.STATE_CONNECTING
                    || state == Call.STATE_RINGING) {
                prepareCallAudio();
            } else if (state == Call.STATE_ACTIVE) {
                // اگر تنظیم بلندگو روشن است، مسیر صدا را حفظ کن.
                applyAutoSpeakerIfEnabled();
            }
        }

        @Override
        public void onDetailsChanged(
                Call call, Call.Details details) {
            CallActivity.refreshFromService();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        currentCall = call;
        call.registerCallback(callCallback);

        showCallScreen();
    }

    @Override
    public void onCallRemoved(Call call) {
        call.unregisterCallback(callCallback);

        if (currentCall == call) {
            currentCall = null;
        }

        super.onCallRemoved(call);

        CallActivity.closeCallScreen();
    }

    private void showCallScreen() {
        Intent intent = new Intent(this, CallActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        if (Build.VERSION.SDK_INT >= 27) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        startActivity(intent);
    }

    private void prepareCallAudio() {
        SharedPreferences prefs = getSharedPreferences("family_home", MODE_PRIVATE);
        boolean autoSpeaker = prefs.getBoolean("auto_speaker", true);

        try {
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0);
            }
            // وقتی تماس در حال زنگ خوردن/بوق خوردن است هم بلندگو را فعال کن.
            if (autoSpeaker && currentCall != null
                    && (currentCall.getState() == Call.STATE_DIALING
                    || currentCall.getState() == Call.STATE_CONNECTING
                    || currentCall.getState() == Call.STATE_RINGING)) {
                setAudioRoute(CallAudioState.ROUTE_SPEAKER);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyAutoSpeakerIfEnabled() {
        SharedPreferences prefs = getSharedPreferences("family_home", MODE_PRIVATE);
        if (!prefs.getBoolean("auto_speaker", true)) return;
        try {
            setAudioRoute(CallAudioState.ROUTE_SPEAKER);
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
            }
            CallActivity.refreshFromService();
        } catch (Exception ignored) {
        }
    }

    public static Call getCurrentCall() {
        return currentCall;
    }

    public static int getCurrentCallState() {
        if (currentCall == null) {
            return Call.STATE_DISCONNECTED;
        }

        return currentCall.getState();
    }

    public static void answerCall() {
        if (currentCall != null
                && currentCall.getState() == Call.STATE_RINGING) {

            currentCall.answer(
                    android.telecom.VideoProfile.STATE_AUDIO_ONLY
            );
        }
    }

    public static void endCall() {
        if (currentCall != null) {
            currentCall.disconnect();
        }
    }

    public static void toggleSpeaker() {
        if (instance == null) {
            return;
        }

        CallAudioState audioState = instance.getCallAudioState();

        if (audioState == null) {
            return;
        }

        int newRoute;

        if (audioState.getRoute() == CallAudioState.ROUTE_SPEAKER) {
            newRoute = CallAudioState.ROUTE_EARPIECE;
        } else {
            newRoute = CallAudioState.ROUTE_SPEAKER;
        }

        instance.setAudioRoute(newRoute);
        CallActivity.refreshFromService();
    }

    public static boolean isSpeakerOn() {
        if (instance == null) {
            return false;
        }

        CallAudioState audioState = instance.getCallAudioState();

        return audioState != null
                && audioState.getRoute() == CallAudioState.ROUTE_SPEAKER;
    }
}
