package com.example.fitnessgachaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {


    private static final int REQUEST_LOCATION_PERMISSION = 1; // Location permission request code

    private DatabaseHelper databaseHelper;

    // UI
    private GoogleMap googleMap;
    private Marker currentUserLocationMarker;
    private float speedKilometersPerHour = 0;

    // UI Elements
    private TextView sessionSpeedView, sessionDistanceView, sessionCalorieView;
    private Chronometer chronometer;
    private long pauseOffset;
    Button startButton;
    Button stopButton;

    //Tracking record
    float caloriesBurned = 0;
    private long sessionDuration;
    private float totalDistance;

    private Intent serviceIntent;

    // Activity lifecycle methods ------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(this, TrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        databaseHelper = new DatabaseHelper(this); // Initialize DatabaseHelper
        setContentView(R.layout.tracking_page);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!hasLocationPermission()) {
            requestLocationPermission();
        }

        // Initialize UI elements
        sessionSpeedView = findViewById(R.id.sessionSpeed);
        sessionDistanceView = findViewById(R.id.sessionDistance);
        chronometer = findViewById(R.id.chronometer);
        sessionCalorieView = findViewById(R.id.sessionCalories);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });
        stopButton.setEnabled(false);

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_tracker);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent = null;
            if (item.getItemId() == R.id.navigation_home) {
                intent = new Intent(TrackingActivity.this, MainActivity.class);
            } else if (item.getItemId() == R.id.navigation_tracker) {
                return false;
            } else if (item.getItemId() == R.id.navigation_profile) {
                intent = new Intent(TrackingActivity.this, ProfileActivity.class);
            } else if (item.getItemId() == R.id.navigation_gacha) {
                intent = new Intent(TrackingActivity.this, GachaActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("TrackingUpdate");
        registerReceiver(trackingUpdateReceiver, filter);
    }
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        boolean isTracking = sharedPreferences.getBoolean("isTracking", false);
        updateUI(isTracking);
    }

    private void updateUI(boolean isTracking) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        if (isTracking) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            // If tracking, set chronometer base to reflect the correct elapsed time
            long elapsedTimeSinceTrackingStarted = SystemClock.elapsedRealtime() - sharedPreferences.getLong("startTimeMillis", 0);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTimeSinceTrackingStarted);
            chronometer.start();
        } else {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            chronometer.stop();

        }
    }
    private void updateChronometer(boolean isTracking) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        if (isTracking) {
            long startTimeMillis = sharedPreferences.getLong("startTimeMillis", 0);
            long elapsedTimeSinceTrackingStarted = SystemClock.elapsedRealtime() - startTimeMillis;

            chronometer.setBase(SystemClock.elapsedRealtime() - elapsedTimeSinceTrackingStarted);
            chronometer.start();
        } else {
            chronometer.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }

    private BroadcastReceiver trackingUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("TrackingUpdate".equals(intent.getAction())) {
                speedKilometersPerHour = intent.getFloatExtra("Speed", 0);
                totalDistance = intent.getFloatExtra("Distance", 0);
                caloriesBurned = intent.getFloatExtra("CaloriesBurned", 0);
                // Extract the position if present
                if (intent.hasExtra("Position")) {
                    LatLng position = intent.getParcelableExtra("Position"); // If LatLng was Parcelable and sent directly
                    updateMapMarker(position);
                }

                // Update the UI elements
                sessionSpeedView.setText(String.format(Locale.US, "%.2f km/h", speedKilometersPerHour));
                sessionDistanceView.setText(String.format(Locale.US, "%.2f m", totalDistance));
                sessionCalorieView.setText(String.format(Locale.US, "%.2f kcal", caloriesBurned));
            }
        }
    };

    private void updateMapMarker(LatLng position) {
        if (googleMap != null) {
            if (currentUserLocationMarker == null) {
                currentUserLocationMarker = googleMap.addMarker(new MarkerOptions().position(position).title("Your Location"));
            } else {
                currentUserLocationMarker.setPosition(position);
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 17));
        }
    }

    // Location request and update methods ---------------------------------------------------------
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MapReady", "Map is ready and adding marker.");
        this.googleMap = googleMap;
        LatLng markerPosition = new LatLng(10,10);
        googleMap.addMarker(new MarkerOptions().position(markerPosition).title("Marker Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPosition,17));
    }

    private void startTracking() {
        if (serviceIntent == null) {
            serviceIntent = new Intent(this, TrackingService.class);
        }
        serviceIntent.setAction(TrackingService.ACTION_START_TRACKING);
        ContextCompat.startForegroundService(this, serviceIntent);

        totalDistance = 0; // Reset total distance
        caloriesBurned = 0;

        // Start the chronometer from 0
        long startTimeMillis = SystemClock.elapsedRealtime();
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("startTimeMillis", startTimeMillis);
        editor.putBoolean("isTracking", true);
        editor.apply();

        updateChronometer(true);

        stopButton.setEnabled(true);
        startButton.setEnabled(false);
        Toast.makeText(this, "Started Tracking", Toast.LENGTH_LONG).show();
    }
    private void stopTracking() {
        updateChronometer(false);
        Intent stopIntent = new Intent(this, TrackingService.class);
        stopIntent.setAction(TrackingService.ACTION_STOP_TRACKING);
        ContextCompat.startForegroundService(this, stopIntent);

        long endTime = SystemClock.elapsedRealtime();
        sessionDuration = endTime - chronometer.getBase();
        Log.d("Tracking", "Session Duration in ms: " + sessionDuration);
        chronometer.stop();
        long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(sessionDuration);
        
        String summaryText = String.format(Locale.US, "Distance: %.2f m, Calories: %.2f kcal, Min: %s",
                totalDistance, caloriesBurned, durationMinutes);
        Toast.makeText(this, summaryText, Toast.LENGTH_LONG).show();

        // Adding to database
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        TrackingRecord record = new TrackingRecord(currentDate, totalDistance, caloriesBurned, durationMinutes); // Create a new TrackingRecord
        databaseHelper.addTrackingRecord(record);

        stopButton.setEnabled(false);
        startButton.setEnabled(true);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isTracking", false);
        editor.apply();
    }

    // Location Permission functions ---------------------------------------------------------------
    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check request code
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
            } else {
                // Permission denied
            }
        }
    }
}