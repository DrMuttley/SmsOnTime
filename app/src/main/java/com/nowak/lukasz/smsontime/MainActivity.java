package com.nowak.lukasz.smsontime;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnCheckedChangeListener{

    private SharedPreferences sharedPreferences;

    private final String MESSAGE_PREFERENCE_KEY = "message_key";
    private final String DATE_PREFERENCE_KEY = "date_key";
    private final String CALENDAR_PREFERENCE_KEY = "calendar_key";
    private final String CONTACT_PREFERENCE_KEY = "contact_key";
    private final String PHONE_NUMBER_PREFERENCE_KEY = "number_key";
    private final String SEND_MESSAGE_CHECKBOX_KEY = "checkBox_key";

    private final String colorBlack = "#000000";

    private Calendar calendar;

    private CheckBox sendMessageCheckBox;

    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minutes = -1;

    private String message = "";
    private String date = "";
    private String contact ="";
    private String phone = "";

    private final static int PERMISSIONS_SEND_SMS = 101;
    private final static int PERMISSIONS_READ_CONTACT = 102;
    private final static int PERMISSIONS_READ_SMS = 103;

    private final int PHONE_REQUEST_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.clear();

        findViewById(R.id.messageButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.dateButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.contactButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.clearButton).setOnClickListener(MainActivity.this);
        findViewById(R.id.exitButton).setOnClickListener(MainActivity.this);

        sendMessageCheckBox = findViewById(R.id.sendMessageCheckBox);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadPreferenceValue();

        showPreferenceValueInTextViews();

        sendMessageCheckBox.setOnCheckedChangeListener(MainActivity.this);

        if(readSmsPermissionGranted() && smsWasSent()){

            Toast.makeText(getApplicationContext(), "Message was sent on " +
                    date, Toast.LENGTH_SHORT).show();

            resetAllData();
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

                if(readContactPermissionGranted()){
                    takeNumber(PHONE_REQUEST_CODE);
                }else{
                    showPermissionsReason();
                }
                break;
            }

            case R.id.clearButton:{
                resetAllData();
                break;
            }

            case R.id.exitButton:{
                System.exit(0);
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

                addYearMonthDayToDate();

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

                addHourMinutesToDate();

                calendar.set(year, month, day, hour,minutes);
                savePreference(CALENDAR_PREFERENCE_KEY, calendar);

                if(System.currentTimeMillis() < calendar.getTimeInMillis()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            savePreference(DATE_PREFERENCE_KEY, date);

                            TextView dateTextView = findViewById(R.id.dateTextView);

                            setTextViewValue(dateTextView, date, colorBlack);
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

    private void showPermissionsReason(){

        AlertDialog.Builder alertDialog  = new AlertDialog.Builder(this);
        alertDialog.setTitle(Html.fromHtml("<b>Reason for permits needed:</b>"));
        alertDialog.setMessage(Html.fromHtml(
                "<font color='#357EC7'><b>" +
                            "Read contacts" +
                        "</b><br></font>" +
                            "To select the recipient of the message<br><br>" +
                        "<font color='#357EC7'><b>" +
                            "Send SMS" +
                        "</b><br></font>" +
                            "To send SMS message to the recipient<br><br>" +
                        "<font color='#357EC7'><b>" +
                            "Read SMS" +
                        "</b><br></font>" +
                            "To check if the message has been sent correctly"));
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                requestAllPermissions();
            }
        });
        alertDialog.create();
        alertDialog.show();
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

                        savePreference(MESSAGE_PREFERENCE_KEY, message);

                        TextView messageTextView = findViewById(R.id.messageTextView);

                        setTextViewValue(messageTextView, message, colorBlack);
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

    private boolean allPermissionsGranted(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                        != PackageManager.PERMISSION_GRANTED){

            return false;
        }
        return true;
    }

    private void requestAllPermissions(){

        ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_SMS},
                    PERMISSIONS_SEND_SMS);
    }

    private boolean readSmsPermissionGranted(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED){

            return false;
        }
        return true;
    }

    private boolean readContactPermissionGranted(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED){

            return false;
        }
        return true;
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

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void stopAlarmManager(){

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 555,
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

                        number = number.replaceAll("\\s+", "");

                        contact = name + " ( " + number + " ) ";
                        phone = number;

                        savePreference(CONTACT_PREFERENCE_KEY, contact);
                        savePreference(PHONE_NUMBER_PREFERENCE_KEY, phone);

                        TextView contactTextView = findViewById(R.id.contactTextView);

                        setTextViewValue(contactTextView, contact, colorBlack);
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
            case PERMISSIONS_READ_SMS: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d("READ_SMS", "Permission READ_SMS granted.");
                }else{
                    Log.d("READ_SMS", "Permission READ_SMS denied.");
                }
                break;
            }
//            case PERMISSIONS_WAKE_LOCK: {
//                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                    Log.d("WAKE_LOCK", "Permission WAKE_LOCK granted.");
//                }else{
//                    Log.d("WAKE_LOCK", "Permission WAKE_LOCK denied.");
//                }
//                break;
//            }
        }
    }

    private void resetAllData(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                message = "";
                savePreference(MESSAGE_PREFERENCE_KEY, message);

                resetDate();
                savePreference(DATE_PREFERENCE_KEY, date);

                contact = "";
                phone = "";
                savePreference(CONTACT_PREFERENCE_KEY, contact);
                savePreference(PHONE_NUMBER_PREFERENCE_KEY, phone);

                showPreferenceValueInTextViews();

                sendMessageCheckBox.setChecked(false);
                savePreference(SEND_MESSAGE_CHECKBOX_KEY, false);
            }
        });
    }

    private void addYearMonthDayToDate(){

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(day);
        stringBuilder.append("/");
        stringBuilder.append(month + 1);
        stringBuilder.append("/");
        stringBuilder.append(year);
        stringBuilder.append(" ");

        date = stringBuilder.toString();
    }

    private void addHourMinutesToDate(){

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(hour);
        stringBuilder.append(":");

        if(minutes < 10){
            stringBuilder.append(0);
        }
        stringBuilder.append(minutes);

        date += stringBuilder.toString();
    }

    public void savePreference(String key, String value){

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void savePreference(String key, boolean value){

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void savePreference(String key, Calendar calendar){

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, calendar.getTimeInMillis());
        editor.apply();
    }

    private void loadPreferenceValue(){

        message = sharedPreferences.getString(MESSAGE_PREFERENCE_KEY, "");
        date = sharedPreferences.getString(DATE_PREFERENCE_KEY, "");
        calendar.setTimeInMillis(sharedPreferences.getLong(CALENDAR_PREFERENCE_KEY, 0));
        contact = sharedPreferences.getString(CONTACT_PREFERENCE_KEY, "");
        phone = sharedPreferences.getString(PHONE_NUMBER_PREFERENCE_KEY, "");

        sendMessageCheckBox.setChecked(sharedPreferences.getBoolean(SEND_MESSAGE_CHECKBOX_KEY, false));
    }

    private void setTextViewValue(TextView textView, String value, String color){

        textView.setTextColor(Color.parseColor(color));
        textView.setText(value);
    }

    private void showPreferenceValueInTextViews(){

        TextView messageTextView = findViewById(R.id.messageTextView);
        TextView dateTextView = findViewById(R.id.dateTextView);
        TextView contactTextView = findViewById(R.id.contactTextView);

        setTextViewValue(messageTextView, message, colorBlack);
        setTextViewValue(dateTextView, date, colorBlack);
        setTextViewValue(contactTextView, contact, colorBlack);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if(isChecked) {

            if (message.equals("") || date.equals("") || phone.equals("")) {

                Toast.makeText(getApplicationContext(), "You are not fill all needed fields.",
                        Toast.LENGTH_LONG).show();

                sendMessageCheckBox.setChecked(false);
                savePreference(SEND_MESSAGE_CHECKBOX_KEY, false);

            } else if (!allPermissionsGranted()) {

                showPermissionsReason();

                sendMessageCheckBox.setChecked(false);
                savePreference(SEND_MESSAGE_CHECKBOX_KEY, false);

            } else if (System.currentTimeMillis() >= calendar.getTimeInMillis() /* && !smsWasSent()*/) {

                Toast.makeText(getApplicationContext(), "The set date has already passed. Before " +
                        "continue please set date again.", Toast.LENGTH_LONG).show();

                sendMessageCheckBox.setChecked(false);
                savePreference(SEND_MESSAGE_CHECKBOX_KEY, false);

            } else {

                Toast.makeText(getApplicationContext(), "Message will be send on " +
                        date, Toast.LENGTH_SHORT).show();

                startAlarmManager();
                savePreference(SEND_MESSAGE_CHECKBOX_KEY, true);
            }

        } else {

            if (PendingIntent.getBroadcast(this, 555,
                    new Intent(this, AlarmReceiver.class),
                    PendingIntent.FLAG_NO_CREATE) != null) {

                stopAlarmManager();
                savePreference(SEND_MESSAGE_CHECKBOX_KEY, false);
            }
        }
    }

    private boolean smsWasSent() {

        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);
                                                                    //inbox
        if (cursor.moveToFirst()) {

            do {

                for (int i = 0; i < cursor.getColumnCount(); i++) {

                    if(cursor.getString(cursor.getColumnIndexOrThrow("address")).equals(phone) &&
                            cursor.getString(cursor.getColumnIndexOrThrow("body")).equals(message)){
                        return true;
                    }
                }

            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.privacy_policy: {

                String url = "http://www.exclusionzone.eu/send-sms-on-time-privacy-policy";

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);

                return true;
            }
            default:{
                return super.onOptionsItemSelected(item);
            }
        }
    }
}
