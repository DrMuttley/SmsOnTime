package com.example.lukasznowak.smsontime;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.security.Provider;
import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        WakeLocker.acquire(context);

        sendSMS(intent.getStringExtra("phone"), intent.getStringExtra("message"));

        WakeLocker.release();
    }

    protected void sendSMS(String phoneNumber, String message){

        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(String.valueOf(message));
        sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        Log.d("SEND_SMS", "Message was send successfully!");
    }
}
