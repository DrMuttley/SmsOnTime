package com.example.lukasznowak.smsontime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        sendSMS(intent.getStringExtra("phone"), intent.getStringExtra("message"));
    }

    protected void sendSMS(String phoneNumber, String message){

        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(String.valueOf(message));
        sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        Log.d("SEND_SMS", "Message was send successfully!");
    }
}
