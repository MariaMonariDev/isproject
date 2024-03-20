package com.example.isproject;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText latitude, longitude, housing_median_age, total_rooms, total_bedrooms, population, median_income, households ;
    private Button predictButton;
    private TextView Result,outputTextView;
    String url = "https://property-price-prediction-d81fee4c4d27.herokuapp.com/predict";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = findViewById(R.id.latEditText);
        longitude = findViewById(R.id.lonEditText);
        housing_median_age = findViewById(R.id.housingAgeEditText);
        total_rooms = findViewById(R.id.totalRoomsEditText);
        total_bedrooms = findViewById(R.id.totalBedroomsEditText);
        population= findViewById(R.id.populationEditText);
        median_income= findViewById(R.id.medianIncomeEditText);
        households = findViewById(R.id.householdsEditText);
        Result = findViewById(R.id.textView);
        predictButton = findViewById(R.id.predict_button);
        outputTextView = findViewById(R.id.output_textview);


        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateInput()) {
                    sendPredictionRequest();
                }
            }
        });
    }
    private boolean validateInput() {
        // Implement your input validation logic here
        // For simplicity, assuming all fields are required
        if (latitude.getText().toString().isEmpty() || longitude.getText().toString().isEmpty() ||
                housing_median_age.getText().toString().isEmpty() || total_rooms.getText().toString().isEmpty() ||
                total_bedrooms.getText().toString().isEmpty() || population.getText().toString().isEmpty() ||
                median_income.getText().toString().isEmpty() || households.getText().toString().isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private void sendPredictionRequest() {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("latitude", Double.parseDouble(latitude.getText().toString()));
            jsonBody.put("longitude", Double.parseDouble(longitude.getText().toString()));
            jsonBody.put("housing_median_age", Integer.parseInt(housing_median_age.getText().toString()));
            jsonBody.put("total_rooms", Integer.parseInt(total_rooms.getText().toString()));
            jsonBody.put("total_bedrooms", Integer.parseInt(total_bedrooms.getText().toString()));
            jsonBody.put("population", Integer.parseInt(population.getText().toString()));
            jsonBody.put("median_income", Double.parseDouble(median_income.getText().toString()));
            jsonBody.put("households", Integer.parseInt(households.getText().toString()));

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                // Check if the 'prediction' key is present
                                if (response.has("prediction")) {
                                    // Get the prediction array
                                    JSONArray predictionArray = response.getJSONArray("prediction");

                                    // Assuming there is only one value in the array, retrieve it
                                    double prediction = predictionArray.getDouble(0);

                                    Log.d("Predict Result:", "onResponse: " + prediction);
                                    Result.setTextColor(Color.parseColor("#5bdeac"));
                                    Result.setText("Predicted Value: $" + prediction);
                                } else {
                                    Log.e("Predict Result:", "No 'prediction' key found in the response");
                                    // Handle the case where 'prediction' key is not present in the response
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String err = (error.getMessage() == null) ? "Failed! Please Try Again" : error.getMessage();
                            Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                            Log.d("API ERROR: ", err);
                        }
                    });

            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
            queue.add(jsonObjectRequest);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}