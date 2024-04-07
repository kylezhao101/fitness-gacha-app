package com.example.fitnessgachaapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Random;

public class PullActivity extends AppCompatActivity implements SensorEventListener {

    private Button returnButton;
    private TextView foodText;
    private ImageView randomImageView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pull_page);

        //layout setup
        returnButton = findViewById(R.id.returnButton);
        foodText = findViewById(R.id.foodName);
        randomImageView = findViewById(R.id.FoodImageView);

        try {
            // Load drawable items JSON
            JSONArray itemsArray = new JSONArray(loadJSONFromAssets());
            // Select a random item
            JSONObject selectedItem = itemsArray.getJSONObject(new Random().nextInt(itemsArray.length()));
            // Get the drawable resource identifier
            int drawableId = getResources().getIdentifier(selectedItem.getString("drawableName"), "drawable", getPackageName());
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
            Bitmap scaledBitmap = scalePixelArt(originalBitmap, 5);
            randomImageView.setImageBitmap(scaledBitmap);
            foodText.setText(selectedItem.getString("name"));
        } catch (Exception e) {
            Log.e("PullActivity", "Error loading or parsing drawable items JSON", e);
            Toast.makeText(this, "Failed to load items.", Toast.LENGTH_SHORT).show();
        }

        returnButton.setOnClickListener(view -> {
            Intent intent = new Intent(PullActivity.this, GachaActivity.class);
            startActivity(intent);
        });
    }

    private String loadJSONFromAssets() {
        String json;
        try {
            InputStream is = getAssets().open("drawable_items.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static Bitmap scalePixelArt(Bitmap bitmap, int scale) {
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap.getWidth() * scale, bitmap.getHeight() * scale, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        Paint paint = new Paint();
        paint.setFilterBitmap(false);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return scaledBitmap;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}