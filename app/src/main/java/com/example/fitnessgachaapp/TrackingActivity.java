package com.example.fitnessgachaapp;

import android.hardware.SensorEventListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

    }
}
