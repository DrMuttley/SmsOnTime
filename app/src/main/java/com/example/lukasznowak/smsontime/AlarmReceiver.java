package com.example.lukasznowak.smsontime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver{

    private final String MESSAGE_STATUS_KEY = "message_status_key";

    @Override
    public void onReceive(Context context, Intent intent) {

        sendSMS(intent.getStringExtra("phone"), intent.getStringExtra("message"));

        setMessageStatus(context);
    }

    protected void sendSMS(String phoneNumber, String message){

        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(String.valueOf(message));
        sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        Log.d("SEND_SMS", "Message was send successfully!");
    }

    private void setMessageStatus(Context context){

        SharedPreferences sharedPreferences = context.getSharedPreferences("myPreference", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MESSAGE_STATUS_KEY, "SMS sent!");
        editor.commit();
    }
}
