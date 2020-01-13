package com.example.showlocation;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.pm.PackageManager;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.showlocation.Constants.createDurationCounter;

/*
Main activity for texting application
 */

public class MainActivity extends AppCompatActivity {

    //Sending the message variables
    private Handler handler;
    private Runnable runnable;
    private int delay = 1000 * 10;
    private CountUpTimer timer;
    //Layout of the screen
    private EditText numberText, timeSpan;
    private Button startButton, endButton;
    private TextView textView, listNumbers;
    //Location managing variables
    private FusedLocationProviderClient fusedLocationProviderClient;
    public static String locationStr;
    private static double wayLatitude, wayLongitude;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int SMS_REQUEST_CODE = 1;
    //Geocoder variables to obtain address
    private Geocoder geocoder;
    private List<Address> addresses;
    private String messageDuration, endMessage;
    //Phone numbers
    private List<String> phoneNums;
    private Button addNum;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpButtonsAndVariables();

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION},
                SMS_REQUEST_CODE);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    wayLatitude = location.getLatitude();
                    wayLongitude = location.getLongitude();
                }
            }
        };

        createLocationRequest();
        LocationAddress();
        startOrEndButton();
    }

    private void setUpButtonsAndVariables() {
        numberText = findViewById(R.id.numEditText);
        startButton = findViewById(R.id.startButton);
        addNum = findViewById(R.id.AddNumber);
        endButton = findViewById(R.id.endButton);
        textView = findViewById(R.id.timeView);
        listNumbers = findViewById(R.id.ListNumbers);
        timeSpan = findViewById(R.id.timeSpan);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        phoneNums = new ArrayList<>();
        messageDuration = Constants.messageDurationFormat();
        endMessage = Constants.messageEndFormat();
    }

    public String printAddressStatement(double latitude, double longitude) {
        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        String address="";
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if(addresses != null && addresses.size() > 0) {
                address += addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }

    private int toMinutesConversion(String str) {
        int num = Integer.parseInt(str);
        num *= (1000 * 60);
        return num;
    }

    private void LocationAddress() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    createLocationRequest();
                    wayLatitude = location.getLatitude();
                    wayLongitude = location.getLongitude();
                    locationStr = wayLatitude + " -- " + wayLongitude;
                }
            }
        });
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * 30);
        locationRequest.setFastestInterval(1000 * 30);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void startOrEndButton() {
        timer = new CountUpTimer(Long.MAX_VALUE) {
            @SuppressLint("DefaultLocale")
            public void onTick(int second) {
                textView.setText(String.format("%02d:%02d:%02d", second / 3600,
                        (second % 3600) / 60, (second % 60)));
                count++;
            }
        };

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                String str = timeSpan.getText().toString();
                delay = toMinutesConversion(str);
                handler.postDelayed(runnable, delay);
                String s = printAddressStatement(wayLatitude, wayLongitude);
                messageText(messageDuration + s + ". Duration: " + createDurationCounter(count));
            }
        };

        addNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = numberText.getText().toString();
                checkTextNumberInList(num, phoneNums);
                String message = phoneNums.toString();
                listNumbers.setText(message);
                numberText.setText("");
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runnable.run();
                timer.start();
                startLocationUpdates();
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = printAddressStatement(wayLatitude, wayLongitude);
                timer.cancel();
                handler.removeCallbacks(runnable);
                messageText(endMessage + s + ". Total Duration: " + createDurationCounter(count));
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                clearPage();
            }
        });

    }

    private void checkTextNumberInList(String num, List<String> phoneNums) {
        boolean isTrue = true;
        if(phoneNums.size() == 0) {
            phoneNums.add(num);
            return;
        }

        for(int i = 0; i < phoneNums.size(); i++){
            if(phoneNums.get(i).equals(num)) {
                isTrue = false;
            }
        }

        if(isTrue) {
            phoneNums.add(num);
        } else Toast.makeText(getApplicationContext(), "Number already in List", Toast.LENGTH_SHORT).show();
    }

    private void clearPage() {
        textView.setText(R.string.reset_time);
        phoneNums = new ArrayList<>();
        numberText.setText("");
        timeSpan.setText("");
        listNumbers.setText("");
        count = 0;
    }

    private void messageText(String str) {
        for (int i = 0; i < phoneNums.size(); i++) {
            String phoneNumber = phoneNums.get(i);
            if (!TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(str)) {

                if (checkPermission(Manifest.permission.SEND_SMS)) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNumber, null, str, null, null);
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean checkPermission(String str) {
        int result = ActivityCompat.checkSelfPermission(MainActivity.this, str);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public boolean requestPermissions(String str) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, str)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{str}, SMS_REQUEST_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case SMS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (requestPermissions(Manifest.permission.SEND_SMS) && requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION)) {}
                break;
            }
        }
    }
}


