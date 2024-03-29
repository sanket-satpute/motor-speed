package com.example.motorspeed;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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


    private DatabaseReference dbRef;
    private boolean fdateChecked = false;
    private boolean fmonthChecked = false;
    private boolean fyearChecked = false;
    private boolean isFirst = true;
    private int fdate = 0;
    private int fmonth = 0;
    private int fyear = 0;
    private int fhour = 0;
    private int limit = 0;
    private int xlimit = 0;
    int temperature = -1, temperature_limit = -1;
    private DatabaseReference databaseReference;
    ValueEventListener minListener, maxListener, valListener, limitListener, xLimitListener, temperatureListener, temperatureLimitListener;
    private ConstraintLayout dialogLayout;
    private LineChart lineChart;
    private TextView noDataMessage, scoreTxt, temp_txt, temp_celsius_txt, air_quality_msg;
    private Button showHistoryButton;

    CircularProgressIndicator temperature_progress;
    SpeedometerView speedometerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initDatabase();
        onClick();
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
        databaseReference.removeEventListener(limitListener);
        databaseReference.removeEventListener(xLimitListener);
        databaseReference.removeEventListener(temperatureListener);
        databaseReference.removeEventListener(temperatureLimitListener);
    }

    private void initDatabase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void initViews() {
        dialogLayout = findViewById(R.id.dialogLayout);
//        lineChart = findViewById(R.id.chart);
//        noDataMessage = findViewById(R.id.noDataMessage);
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
            if (dialogLayout.getVisibility() == View.VISIBLE) {
                hideDialog();
                showHistoryButton.setText("Show History");
            } else {
                showDialog();
                showHistoryButton.setText("Hide History");
            }
        });
    }

    private void showDialog() {
        Button closeBtn;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.graph_dialog_layout, null);
        dialog.setView(view);

        lineChart = view.findViewById(R.id.chart);
        noDataMessage = view.findViewById(R.id.noDataMessage);
        closeBtn = view.findViewById(R.id.closeDialogButton);

        AlertDialog alert = dialog.create();
        alert.show();

        closeBtn.setOnClickListener(v -> alert.dismiss());

//        dialogLayout.setVisibility(View.VISIBLE);
//        dialogLayout.bringToFront();
//        dialogLayout.requestFocus();
//
        databaseReference.child("db/dates/").get().addOnSuccessListener(dataSnapshot -> {
            List<HashMap<String, Long>> pojo = (List<HashMap<String, Long>>) dataSnapshot.getValue();
            if (pojo != null) {
                if (pojo.size() > 0) {
                    if (pojo.size() > 3) {
                        noDataMessage.setVisibility(View.GONE);
                        lineChart.setVisibility(View.VISIBLE);
                        collectData(pojo);
                    } else {
                        noDataFoundTxt(-1);
                    }
                } else {
                    noDataFoundTxt(0);
                }
            } else {
                noDataFoundTxt(1);
            }
        });
    }

    private void collectData(List<HashMap<String, Long>> pojos) {
        List<String> dates = new ArrayList<>();
        List<Long> labels = new ArrayList<>();
        for (int i = pojos.size() - 1; i > (pojos.size() - Math.min(pojos.size(), 7)); i--) {
            dates.add(pojos.get(i).get("date") + "/" + pojos.get(i).get("month"));
            labels.add(pojos.get(i).get("date_reading"));
        }
        Collections.reverse(labels);
        Collections.reverse(dates);
        setupLineChart();
        addRandomDataToChart(dates, labels);
    }

    private void startFirebase() {

        temperatureListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                temperature = dataSnapshot.getValue(Integer.class);
                if (temperature >= temperature_limit) {
                    warnByNotify(temperature, temperature_limit, 1);
                }
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
                if (temperature >= temperature_limit) {
                    warnByNotify(temperature, temperature_limit, 1);
                }
                temperatureAnimate(temperature, temperature_limit);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("temp_limit").addValueEventListener(temperatureLimitListener);

        limitListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                limit = dataSnapshot.getValue(Integer.class);
                if (limit <= xlimit) {
                    warnByNotify(xlimit, limit, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("limit").addValueEventListener(limitListener);

        xLimitListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                xlimit = dataSnapshot.getValue(Integer.class);
                if (limit <= xlimit) {
                    warnByNotify(xlimit, limit, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("x_limit").addValueEventListener(xLimitListener);

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

        maxListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                fMaxChecked = true;
                fMax = dataSnapshot.getValue(Integer.class);
                checkAllOk();
                airQualityMsgAnimate(fVal, fMax);
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
                airQualityMsgAnimate(fVal, fMax);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
        databaseReference.child("db").child("sen").addValueEventListener(valListener);
    }

    private void airQualityMsgAnimate(int quality, int qualityLimit) {
        int color;
        String msg;
        int mediumQuality = qualityLimit / 2;
        int thirdHalfQuality = (int) ((int) qualityLimit / 1.5);
        if (quality >= mediumQuality && quality <= thirdHalfQuality) {
//                warn
            color = Color.parseColor("#eed202");
            msg = "air quality is bad.";
        } else if (quality < mediumQuality) {
//                all ok
            color = Color.parseColor("#00ff00");
            msg = "air quality is good.";
        } else {
//                danger
            color = Color.parseColor("#ff0000");
            msg = "air quality is worst.";
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

    private static final String CHANNEL_ID = "your_channel_id";
    private static final CharSequence CHANNEL_NAME = "Your Channel Name";
    private static final String CHANNEL_DESCRIPTION = "Your Channel Description";
    private void warnByNotify(int cross_limit, int maintain_limit, int warn_for_what) {
//        0 = limit   and     1 = temperature
        String message;
        String title;
        int drawable;
        if (warn_for_what == 0) {
            message = "Limit is crossed " + cross_limit + " maintain limit under " + maintain_limit;
            drawable = R.mipmap.notification_icon;
            title = "Limit Crossed";
        } else {
            message = "High Temperature Detected " + cross_limit + "°c";
            drawable = R.mipmap.tempreture_icon;
            title = "High Temperature";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(drawable)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        // Show the notification
        notificationManager.notify(/* notificationId */ 1, builder.build());
    }

    private void checkAllOk() {
        if (fMinChecked && fMaxChecked && fValChecked) {
            xValue = convertValueToRange(fVal, fMin, fMax, rMin, rMax);
            Log.d(TAG, "X : " + xValue);
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

        dbRef = databaseReference.child("db/dates");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int dataLength = (int) dataSnapshot.getChildrenCount() - 1;

                DatabaseReference fDtRef = dbRef.child(String.valueOf(dataLength)).child("date");
                fDtRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        fdate = snapshot.getValue(Integer.class);
                        fdateChecked = true;
                        checkDates(day, month, year, hour, dataLength);
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
                        checkDates(day, month, year, hour, dataLength);
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
                        checkDates(day, month, year, hour, dataLength);
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

    private void checkDates(int cdate, int cmonth, int cyear, int chour, int dataLength) {
        if (fdateChecked && fmonthChecked && fyearChecked && isFirst) {
            isFirst = false;
            if ((cdate == fdate) && (cmonth == fmonth) && (cyear == fyear)) {
                // Same date, check hour
                if (chour != fhour) {
                    // Not same hour, create new entry
                    dbRef.child(String.valueOf(dataLength)).child("hours").child(String.valueOf(chour)).setValue(new HourReading(fVal));

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

                // Set first hour for the new date
                dbRefNewEntry.child("hours").child(String.valueOf(chour)).setValue(new HourReading(fVal));
                Log.d(TAG, "Updated ...");
            }
        }
    }

    // Model class for hour reading
    private static class HourReading {
        public int reading;

        public HourReading() {
            // Default constructor required for Firebase
        }

        public HourReading(int reading) {
            this.reading = reading;
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

    private void noDataFoundTxt(int stateValue) {
        noDataMessage.setVisibility(View.VISIBLE);
        lineChart.setVisibility(View.GONE);
        if (stateValue == 0) {
            noDataMessage.setText("No Data Found");
        } else if (stateValue == -1) {
            noDataMessage.setText("Data is less than 3 days we can't show!");
        } else if (stateValue == 1) {
            noDataMessage.setText("Some Problem Occured");
        }
    }

    private void hideDialog() {
        dialogLayout.setVisibility(View.GONE);
    }

    private void setupLineChart() {
        // Customize line chart appearance
        lineChart.setDrawGridBackground(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addRandomDataToChart(List<String> dates, List<Long> labels) {
        List<Entry> entries = new ArrayList<>();

        // Generate random data points
        for (int i = 0; i < dates.size(); i++) {
            entries.add(new Entry(i, labels.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Mater Data");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(128);
        dataSet.setFillColor(Color.BLUE);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates)); // Set fixed labels for the X-axis

        lineChart.setData(lineData);
        lineChart.invalidate(); // Refresh chart
    }
}




// Write a message to the database
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference myRef = database.getReference("message");
//
//        myRef.setValue("Hello, World!");
