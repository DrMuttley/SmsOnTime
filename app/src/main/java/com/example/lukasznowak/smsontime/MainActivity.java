package com.example.lukasznowak.smsontime;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Calendar calendar;

    private CheckBox sendMessageCheckBox;

    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minutes = -1;

    private String message = "";
    private String date = "";
    private String phone = "";

    private final static int PERMISSIONS_SEND_SMS = 101;
    private final static int PERMISSIONS_READ_CONTACT = 102;

    private final int PHONE_REQUEST_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendMessageCheckBox = findViewById(R.id.sendMessageCheckBox);
        sendMessageCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked){

                    if(message == "" || date == "" || phone == ""){

                        Toast.makeText(getApplicationContext(), "You aren't fill all needed fields.",
                                Toast.LENGTH_SHORT).show();

                        sendMessageCheckBox.setChecked(false);

                    }else{
                        startAlarmManager();

                        Toast.makeText(getApplicationContext(), "Message will be send on " +
                                date, Toast.LENGTH_SHORT).show();
                    }
                }else{
                    stopAlarmManager();

                    Toast.makeText(getApplicationContext(), "Message won't be send", Toast.LENGTH_SHORT).show();
                }
            }
        });

        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.clear();

        findViewById(R.id.messageButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.dateButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.contactButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.clearButton).setOnClickListener(MainActivity.this);

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(System.currentTimeMillis() < calendar.getTimeInMillis() &&
                sendMessageCheckBox.isChecked()){

            sendMessageCheckBox.setChecked(false);

            Toast.makeText(getApplicationContext(), "Message was successfully send on " +
                    date, Toast.LENGTH_SHORT).show();

            resetDate();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.messageButton:{
                runMessageDialog();
                break;
            }

            case R.id.dateButton:{
                runDatePicker();
                break;
            }

            case R.id.contactButton:{
                takeNumber(PHONE_REQUEST_CODE);
                break;
            }

            case R.id.clearButton:{
                resetAllData();
                break;
            }
        }
    }

    private void runDatePicker() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.data_picker, null, false);

        final DatePicker datePicker = view.findViewById(R.id.date_picker);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                year = datePicker.getYear();
                month = datePicker.getMonth();
                day = datePicker.getDayOfMonth();

                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append(day);
                stringBuilder.append("/");
                stringBuilder.append(month + 1);
                stringBuilder.append("/");
                stringBuilder.append(year);
                stringBuilder.append(" ");

                date = stringBuilder.toString();

                runTimePicker();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                resetDate();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                resetDate();
            }
        });
        builder.create();
        builder.show();
    }

    private void runTimePicker() {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.time_picker, null, false);

        final TimePicker timePicker = view.findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                hour = timePicker.getCurrentHour();
                minutes = timePicker.getCurrentMinute();

                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append(hour);
                stringBuilder.append(":");
                stringBuilder.append(minutes);

                date += stringBuilder.toString();

                calendar.set(year,month, day, hour,minutes);


                if(System.currentTimeMillis() < calendar.getTimeInMillis()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TextView dateTextView = findViewById(R.id.dateTextView);
                            dateTextView.setTextColor(Color.parseColor("#000000"));
                            dateTextView.setText(date);
                        }
                    });

                }else{
                    resetDate();

                    Toast.makeText(getApplicationContext(), "This date is gone, try again!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                resetDate();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                resetDate();
            }
        });
        builder.create();
        builder.show();
    }

    private void runMessageDialog(){

        final EditText edittext = new EditText(MainActivity.this);
        edittext.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edittext.setHint("text message");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(edittext);
        builder.setTitle("Provide SMS message to send:");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                message = edittext.getText().toString();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        TextView messageTextView = findViewById(R.id.messageTextView);
                        messageTextView.setTextColor(Color.parseColor("#000000"));
                        messageTextView.setText(message);
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        //builder.create();

        AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void checkPermissions(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_CONTACTS,},
                    PERMISSIONS_SEND_SMS);
        }
    }

    private void resetDate(){

        calendar.clear();

        year = -1;
        month = -1;
        day = -1;
        hour = -1;
        minutes = -1;

        date = "";
    }

    private void startAlarmManager() {

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("message", message);
        intent.putExtra("phone", phone);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 555,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void stopAlarmManager(){

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 444,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
    }

    private void takeNumber(final Integer REQUEST_CODE){

        Intent calContctPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        calContctPickerIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(calContctPickerIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {

            if (requestCode == PHONE_REQUEST_CODE) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Uri contactUri = data.getData();
                        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
                        cursor.moveToFirst();

                        int columnName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int columnNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                        String name = cursor.getString(columnName);
                        String number = cursor.getString(columnNumber);

                        TextView textView = findViewById(R.id.contactTextView);
                        textView.setTextColor(Color.parseColor("#000000"));
                        textView.setText(name + " ( " + number + " ) ");

                        phone = number;
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_SEND_SMS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("SEND_SMS", "Permission SEND_SMS granted.");
                } else {
                    Log.d("SEND_SMS", "Permission SEND_SMS denied.");
                }
                break;
            }
            case PERMISSIONS_READ_CONTACT: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("READ_CONTACTS", "Permission READ_CONTACTS granted.");
                }else{
                    Log.d("READ_CONTACTS", "Permission READ_CONTACTS denied.");
                }
                break;
            }
        }
    }

    private void resetAllData(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                message = "";

                TextView messageTextView = findViewById(R.id.messageTextView);
                messageTextView.setTextColor(Color.parseColor("#FF0000"));
                messageTextView.setText("There is no message to send");

                resetDate();

                TextView dateTextView = findViewById(R.id.dateTextView);
                dateTextView.setTextColor(Color.parseColor("#FF0000"));
                dateTextView.setText("The date isn't set");

                phone = "";

                TextView contactTextView = findViewById(R.id.contactTextView);
                contactTextView.setTextColor(Color.parseColor("#FF0000"));
                contactTextView.setText("Contacts weren't selected");

                sendMessageCheckBox.setChecked(false);
            }
        });

        Toast.makeText(getApplicationContext(), "All data was removed.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.info:
                showIconsSource();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showIconsSource(){
        Toast.makeText(getApplicationContext(), "All icons come from the website " +
                "https://icons8.com/icons", Toast.LENGTH_SHORT).show();
    }

}
