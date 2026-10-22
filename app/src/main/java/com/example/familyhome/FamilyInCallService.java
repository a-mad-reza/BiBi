package com.example.familyhome;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.CallAudioState;
import android.os.Build;

public class FamilyInCallService extends InCallService {

    private static FamilyInCallService instance;
    private static Call currentCall;

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            CallActivity.refreshFromService();
        }

        @Override
        public void onDetailsChanged(Call call, Call.Details details) {
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
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );
        }

        startActivity(intent);
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