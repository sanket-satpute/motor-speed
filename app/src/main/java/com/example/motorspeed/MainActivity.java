package com.example.motorspeed;

import static com.example.motorspeed.BackgroundIntentService.SERVICE_IS_ON;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseTest";
    private boolean fMinChecked = false;
    private boolean fMaxChecked = false;
    private boolean fValChecked = false;
    private int fMin = 0;
    private int fMax = 0;
    private int fVal = 0;
    private int rMin = 0;
    private int rMax = 100;
    private int xValue = 0;
    private int airThreshold = 0;

    private DatabaseReference dbRef;
    private boolean fdateChecked = false;
    private boolean fmonthChecked = false;
    private boolean fyearChecked = false;
    private boolean isFirst = true;
    private int fdate = 0;
    private int fmonth = 0;
    private int fyear = 0;
    private int fhour = 0;
    int temperature = -1, temperature_limit = -1;
    private DatabaseReference databaseReference;

    ValueEventListener minListener, maxListener, valListener, temperatureListener, temperatureLimitListener, thresholdListener;

    private TextView scoreTxt, temp_txt, temp_celsius_txt, air_quality_msg;
    private Button showHistoryButton;

    CircularProgressIndicator temperature_progress;
    SpeedometerView speedometerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent inte = new Intent(this, BackgroundIntentService.class);

        if (SERVICE_IS_ON)
            stopService(inte);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(inte);
        } else {
            startService(inte);
        }

        initViews();
        initDatabase();
//        removeValues();
        onClick();
    }

    private void removeValues() {
        DatabaseReference red = databaseReference.child("/db/dates");
        for (int i = 11; i < 200; i++) {
            red.child("/" + i).removeValue();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startFirebase();
        checkDateIfNotThenAdd();
    }

    @Override
    protected void onPause() {
        super.onPause();
        databaseReference.removeEventListener(maxListener);
        databaseReference.removeEventListener(minListener);
        databaseReference.removeEventListener(valListener);
        databaseReference.removeEventListener(temperatureListener);
        databaseReference.removeEventListener(temperatureLimitListener);
        databaseReference.removeEventListener(thresholdListener);
    }

    private void initDatabase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void initViews() {
        showHistoryButton = findViewById(R.id.showHistoryButton);
        speedometerView = findViewById(R.id.speedometerView);
        temperature_progress = findViewById(R.id.temperature_progress_bar);
        temp_txt = findViewById(R.id.temperature_txt);
        temp_celsius_txt = findViewById(R.id.temperature_not_txt);
        scoreTxt = findViewById(R.id.scoreText);
        air_quality_msg = findViewById(R.id.airQualityMsg);

        temperature_progress.setMax(60);
    }

    private void onClick() {
        showHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SecondActivity.class);
            startActivity(intent);
        });
    }

    private void startFirebase() {

        temperatureListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                temperature = dataSnapshot.getValue(Integer.class);
                temperatureAnimate(temperature, temperature_limit);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("temp").addValueEventListener(temperatureListener);

        temperatureLimitListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                temperature_limit = dataSnapshot.getValue(Integer.class);
                temperatureAnimate(temperature, temperature_limit);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("temp_limit").addValueEventListener(temperatureLimitListener);

        minListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fMinChecked = true;
                fMin = dataSnapshot.getValue(Integer.class);
                checkAllOk();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen_min").addValueEventListener(minListener);

        thresholdListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                airThreshold = dataSnapshot.getValue(Integer.class);
                airQualityMsgAnimate(fVal, fMax, airThreshold);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen_threshold").addValueEventListener(thresholdListener);

        maxListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fMaxChecked = true;
                fMax = dataSnapshot.getValue(Integer.class);
                checkAllOk();
                airQualityMsgAnimate(fVal, fMax, airThreshold);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen_limit").addValueEventListener(maxListener);

        valListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fValChecked = true;
                fVal = dataSnapshot.getValue(Integer.class);
                checkAllOk();
                airQualityMsgAnimate(fVal, fMax, airThreshold);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen").addValueEventListener(valListener);
    }

    private void airQualityMsgAnimate(int quality, int qualityLimit, int airThreshold) {
        int color;
        String msg;
//        int mediumQuality = qualityLimit / 2;
//        int thirdHalfQuality = (int) ((int) qualityLimit / 1.5);
//        if (quality >= mediumQuality && quality <= thirdHalfQuality) {
////                warn
//            color = Color.parseColor("#eed202");
//            msg = "air quality is bad.";
//        } else
            if (quality < airThreshold) {
//                all ok
            color = Color.parseColor("#00ff00");
            msg = "air quality is good.";
        } else {
//                danger
            color = Color.parseColor("#ff0000");
            msg = "air quality is bad.";
        }
        air_quality_msg.setText(msg);
        air_quality_msg.setTextColor(color);
    }

    private void temperatureAnimate(int cross_limit, int maintain_limit) {
        temperature_progress.setProgress(cross_limit);
        int color;
        if (cross_limit >= (maintain_limit - 2) && cross_limit <= maintain_limit) {
//                warn
            color = Color.parseColor("#eed202");
            temperature_progress.setIndicatorColor(color); // #ffff00
            temp_txt.setTextColor(color);
            temp_celsius_txt.setTextColor(color);
        } else if (cross_limit < maintain_limit) {
//                all ok
            color = Color.parseColor("#00ff00");
            temperature_progress.setIndicatorColor(color); // #ffff00
            temp_txt.setTextColor(color);
            temp_celsius_txt.setTextColor(color);
        } else {
//                danger
            color = Color.parseColor("#ff0000");
            temperature_progress.setIndicatorColor(color); // #ffff00
            temp_txt.setTextColor(color);
            temp_celsius_txt.setTextColor(color);
        }

        ValueAnimator temp_anim = ValueAnimator.ofInt(Integer.parseInt(temp_txt.getText().toString()), cross_limit);
        temp_anim.setDuration(400);
        temp_anim.setInterpolator(new AccelerateDecelerateInterpolator());
        temp_anim.addUpdateListener(animation -> {
            temp_txt.setText(animation.getAnimatedValue().toString());
            temperature_progress.setProgress(Integer.parseInt(animation.getAnimatedValue().toString()));
        });
        temp_anim.start();
    }

    private void checkAllOk() {
        if (fMinChecked && fMaxChecked && fValChecked) {
            xValue = convertValueToRange(fVal, fMin, fMax, rMin, rMax);
            updateScore(xValue, fVal);
        }
    }

    private int convertValueToRange(int value, int minInput, int maxInput, int minOutput, int maxOutput) {
        return (value - minInput) * (maxOutput - minOutput) / (maxInput - minInput) + minOutput;
    }

    public void checkDateIfNotThenAdd() {
        Date currentDate = Calendar.getInstance().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH returns 0-based index
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        dbRef = databaseReference.child("db/dates");

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int dataLength = (int) dataSnapshot.getChildrenCount() - 1;

                DatabaseReference fDtRef = dbRef.child(String.valueOf(dataLength)).child("date");
                fDtRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        fdate = snapshot.getValue(Integer.class);
                        fdateChecked = true;
//                        checkDates(day, month, year, hour, minute, dataLength);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to read date value.", error.toException());
                    }
                });

                DatabaseReference fMtRef = dbRef.child(String.valueOf(dataLength)).child("month");
                fMtRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        fmonth = snapshot.getValue(Integer.class);
                        fmonthChecked = true;
//                        checkDates(day, month, year, hour, minute, dataLength);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to read month value.", error.toException());
                    }
                });

                DatabaseReference fYtRef = dbRef.child(String.valueOf(dataLength)).child("year");
                fYtRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        fyear = snapshot.getValue(Integer.class);
                        fyearChecked = true;
//                        checkDates(day, month, year, hour, minute, dataLength);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to read year value.", error.toException());
                    }
                });

                DatabaseReference fHourRef = dbRef.child(String.valueOf(dataLength)).child("hours");
                fHourRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Object data = snapshot.getValue();
                        if (data instanceof HashMap) {
                            HashMap<String, HashMap<String, Object>> hoursList = (HashMap<String, HashMap<String,Object>>) data;
                            Set<String> keys = hoursList.keySet();
                            int tempHour = hour;
                            if (!keys.contains(String.valueOf(hour))) {
                                tempHour = hour * -1;
                            }
                            checkDates(day, month, year, tempHour, minute, dataLength);
                        } else if (data instanceof ArrayList) {
                            ArrayList<HashMap<String, Object>> dataList = (ArrayList<HashMap<String, Object>>) data;
                            int tempHour = hour;
                            if (dataList.get(dataList.size() - 1) != null) {
                                tempHour = hour * -1;
                            }
                            checkDates(day, month, year, tempHour, minute, dataLength);
                        } else {
                            Log.e(TAG, "Unexpected data type: " + data.getClass().getSimpleName());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to read year value.", error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to read database value.", error.toException());
            }
        });
    }

    private void checkDates(int cdate, int cmonth, int cyear, int chour, int minute, int dataLength) {
        if (fdateChecked && fmonthChecked && fyearChecked && isFirst) {
            isFirst = false;
            if ((cdate == fdate) && (cmonth == fmonth) && (cyear == fyear)) {
                // Same date, check hour
                if (chour < 0) {
                    chour = chour * -1;
                    // Not same hour, create new entry
                    String time = ((chour > 12) ? chour / 2 : chour) + ":" + minute + " " + ((chour > 12) ? "PM" : "AM");
                    dbRef.child(String.valueOf(dataLength)).child("hours").child(String.valueOf(chour)).setValue(new HourReading(fVal, time, temperature));

                    DatabaseReference fReadingCheckRef = dbRef.child(String.valueOf(dataLength)).child("date_reading");
                    fReadingCheckRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            int data = snapshot.getValue(Integer.class);
                            if (data < fVal) {
                                // Not same date, create new entry
                                DatabaseReference dbRefNewDate = dbRef.child(String.valueOf(dataLength));
                                dbRefNewDate.child("date_reading").setValue(fVal);
                                Log.d(TAG, "Data updated");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Failed to read date_reading value.", error.toException());
                        }
                    });
                }
            } else {
                // Not same date, create new entry
                DatabaseReference dbRefNewEntry = dbRef.child(String.valueOf(dataLength + 1));
                dbRefNewEntry.child("date").setValue(cdate);
                dbRefNewEntry.child("month").setValue(cmonth);
                dbRefNewEntry.child("year").setValue(cyear);
                dbRefNewEntry.child("date_reading").setValue(fVal);
                dbRefNewEntry.child("temp").setValue(temperature);

                // Set first hour for the new date

                String time = ((chour > 12) ? chour / 2: chour) + ":" + minute + " " + ((chour > 12) ? "PM" : "AM");
                dbRefNewEntry.child("hours").child(String.valueOf(chour)).setValue(new HourReading(fVal, time, temperature));
                Log.d(TAG, "Updated ...");
            }
        }
    }

    // Model class for hour reading
    private static class HourReading {
        public int reading;
        public String time;
        public int temp;

        public HourReading() {
            // Default constructor required for Firebase
        }

        public HourReading(int reading, String time, int temp) {
            this.reading = reading;
            this.time = time;
            this.temp = temp;

        }
    }

    private void updateScore(int progress, int score) {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            ValueAnimator pr_animator = ValueAnimator.ofInt((int) speedometerView.getCurrentValue(), progress);
            pr_animator.setDuration(500);
            pr_animator.start();
            pr_animator.setInterpolator(new AccelerateDecelerateInterpolator());
            pr_animator.addUpdateListener(animation -> speedometerView.setCurrentValue(Integer.parseInt(animation.getAnimatedValue().toString())));

            ValueAnimator sc_animator = ValueAnimator.ofInt(Integer.parseInt(scoreTxt.getText().toString()), score);
            sc_animator.setDuration(500);
            sc_animator.start();
            sc_animator.setInterpolator(new AccelerateDecelerateInterpolator());
            sc_animator.addUpdateListener(animation -> scoreTxt.setText(animation.getAnimatedValue().toString()));
        }, 0);
    }


}




// Write a message to the database
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference("message");
//
//        myRef.setValue("Hello, World!");
