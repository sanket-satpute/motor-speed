package com.example.motorspeed;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecondActivity extends AppCompatActivity {

    Spinner dropdown, tempDropdown;
    private LineChart lineChart, tempLineChart;
    TextView noDataMessage, tempNoMsgData;
    ImageView close_or_exit_img;
    DatabaseReference databaseReference;


    String[] items = {"Days", "Hours"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        initViews();
        connectToDatabase();
        addSpinner();
        onClick();

        showDaysChart();
    }

    private void initViews() {
        close_or_exit_img = findViewById(R.id.close_page_btn);
        lineChart = findViewById(R.id.chart);
        noDataMessage = findViewById(R.id.noDataMessage);
        dropdown = findViewById(R.id.mySpinner);
        tempLineChart = findViewById(R.id.chartTemp);
        tempNoMsgData = findViewById(R.id.noDataMessageTemp);
        tempDropdown = findViewById(R.id.mySpinnerTemp);
    }

    private void onClick() {
        close_or_exit_img.setOnClickListener(v -> finish());
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedItem = items[position];
                if (selectedItem.equals("Days")) {
                    showDaysChart();
                } else {
                    showHoursChart();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){}
        });

        tempDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedItem = items[position];
                if (selectedItem.equals("Days")) {
                    showDaysChartTemp();
                } else {
                    showHoursChartTemp();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){}
        });
    }

    private void connectToDatabase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void addSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.setAdapter(adapter);

        ArrayAdapter<String> adapterTemp = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapterTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tempDropdown.setAdapter(adapter);
    }

    int days_size;
    private void showDaysChart() {
        databaseReference.child("db/dates/").get().addOnSuccessListener(dataSnapshot -> {
            List<HashMap<String, Long>> pojo = (List<HashMap<String, Long>>) dataSnapshot.getValue();
            if (pojo != null) {
                days_size = pojo.size();
                if (pojo.size() > 0) {
                    if (pojo.size() > 3) {
                        noDataMessage.setVisibility(View.GONE);
                        lineChart.setVisibility(View.VISIBLE);
                        collectDataOnDays(pojo);
                    } else {
                        noDataFoundTxt(-1);
                    }
                } else {
                    noDataFoundTxt(0);
                }
            }
        });
    }

    private void showHoursChart() {
        databaseReference.child("db/dates/" + (days_size - 1) + "/hours").get().addOnSuccessListener(dataSnapshot -> {
            HashMap<String, HashMap<String, Long>> hoursMap = (HashMap<String, HashMap<String, Long>>) dataSnapshot.getValue();
            if (hoursMap != null) {
                if (!hoursMap.isEmpty()) {
                    noDataMessage.setVisibility(View.GONE);
                    lineChart.setVisibility(View.VISIBLE);
                    collectDataOnHours(hoursMap);
                } else {
                    noDataFoundTxt(0);
                }
            } else {
                noDataFoundTxt(1);
            }
        });
    }

    private void collectDataOnDays(List<HashMap<String, Long>> pojos) {
        List<String> dates = new ArrayList<>();
        List<Long> labels = new ArrayList<>();
        for (int i = pojos.size() - 1; i > (pojos.size() - Math.min(pojos.size(), 7)); i--) {
            dates.add(pojos.get(i).get("date") + "/" + pojos.get(i).get("month"));
            labels.add(pojos.get(i).get("date_reading"));
        }
        Collections.reverse(labels);
        Collections.reverse(dates);
        setupLineChart();
        addRandomDataToChart(dates, labels, "per days");
    }

    private void collectDataOnHours(HashMap<String, HashMap<String, Long>> hoursMap) {
        List<String> time = new ArrayList<>();
        List<Long> labels = new ArrayList<>();

        for (Map.Entry<String, HashMap<String, Long>> entry : hoursMap.entrySet()) {
            HashMap<String, Long> innerMap = entry.getValue();

            if (innerMap != null) {
                Long reading_value = innerMap.get("reading");
                String key = String.valueOf(innerMap.get("time"));

                if (reading_value != null) {
                    labels.add(reading_value);
                    time.add(key);
                }
            }
        }

        setupLineChart();
        addRandomDataToChart(time, labels, "per hours");
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

    private void addRandomDataToChart(List<String> dates, List<Long> labels, String whatItIs) {
        List<Entry> entries = new ArrayList<>();

        // Generate random data points
        for (int i = 0; i < dates.size(); i++) {
            entries.add(new Entry(i, labels.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "air quality " + whatItIs);
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

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Set position as needed
        xAxis.setLabelRotationAngle(45); // Rotate labels if necessary
        xAxis.setGranularity(1f); // Set granularity to 1 for integer indices

        lineChart.setData(lineData);
        lineChart.invalidate(); // Refresh chart
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





//    temperature

    private void showDaysChartTemp() {
        databaseReference.child("db/dates/").get().addOnSuccessListener(dataSnapshot -> {
            List<HashMap<String, Long>> pojo = (List<HashMap<String, Long>>) dataSnapshot.getValue();
            if (pojo != null) {
                days_size = pojo.size();
                if (pojo.size() > 0) {
                    if (pojo.size() > 3) {
                        tempNoMsgData.setVisibility(View.GONE);
                        tempLineChart.setVisibility(View.VISIBLE);
                        collectDataOnDaysTemp(pojo);
                    } else {
                        noDataFoundTxtTemp(-1);
                    }
                } else {
                    noDataFoundTxtTemp(0);
                }
            }
        });
    }

    private void showHoursChartTemp() {
        databaseReference.child("db/dates/" + (days_size - 1) + "/hours").get().addOnSuccessListener(dataSnapshot -> {
            HashMap<String, HashMap<String, Long>> hoursMap = (HashMap<String, HashMap<String, Long>>) dataSnapshot.getValue();
            if (hoursMap != null) {
                if (!hoursMap.isEmpty()) {
                    noDataMessage.setVisibility(View.GONE);
                    lineChart.setVisibility(View.VISIBLE);
                    collectDataOnHoursTemp(hoursMap);
                } else {
                    noDataFoundTxtTemp(0);
                }
            } else {
                noDataFoundTxtTemp(1);
            }
        });
    }

    private void collectDataOnDaysTemp(List<HashMap<String, Long>> pojos) {
        List<String> dates = new ArrayList<>();
        List<Long> labels = new ArrayList<>();
        for (int i = pojos.size() - 1; i > (pojos.size() - Math.min(pojos.size(), 7)); i--) {
            dates.add(pojos.get(i).get("date") + "/" + pojos.get(i).get("month"));
            labels.add(pojos.get(i).get("temp"));
        }
        Collections.reverse(labels);
        Collections.reverse(dates);
        setupLineChartTemp();
        addRandomDataToChartTemp(dates, labels, "per days");
    }

    private void collectDataOnHoursTemp(HashMap<String, HashMap<String, Long>> hoursMap) {
        List<String> time = new ArrayList<>();
        List<Long> labels = new ArrayList<>();

        for (Map.Entry<String, HashMap<String, Long>> entry : hoursMap.entrySet()) {
//            String hourKey = entry.getKey();
            HashMap<String, Long> innerMap = entry.getValue();

            if (innerMap != null) {
                Long reading_value = innerMap.get("temp");
                String key = String.valueOf(innerMap.get("time"));

                if (reading_value != null) {
                    labels.add(reading_value);
                    time.add(key);
                }
            }
        }

        setupLineChartTemp();
        addRandomDataToChartTemp(time, labels, "per hour");
    }

    private void setupLineChartTemp() {
        // Customize line chart appearance
        tempLineChart.setDrawGridBackground(false);
        tempLineChart.getDescription().setEnabled(false);
        tempLineChart.setTouchEnabled(false);
        tempLineChart.setDragEnabled(false);
        tempLineChart.setScaleEnabled(false);
        tempLineChart.setPinchZoom(false);
        tempLineChart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = tempLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = tempLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = tempLineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addRandomDataToChartTemp(List<String> dates, List<Long> labels, String whatItIs) {
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {
            entries.add(new Entry(i, labels.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "High Temperature " + whatItIs);
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
        XAxis xAxis = tempLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Set position as needed
        xAxis.setLabelRotationAngle(45); // Rotate labels if necessary
        xAxis.setGranularity(1f); // Set granularity to 1 for integer indices

        tempLineChart.setData(lineData);
        tempLineChart.invalidate(); // Refresh chart
    }

    private void noDataFoundTxtTemp(int stateValue) {
        tempNoMsgData.setVisibility(View.VISIBLE);
        tempLineChart.setVisibility(View.GONE);
        if (stateValue == 0) {
            tempNoMsgData.setText("No Data Found");
        } else if (stateValue == -1) {
            tempNoMsgData.setText("Data is less than 3 days we can't show!");
        } else if (stateValue == 1) {
            tempNoMsgData.setText("Some Problem Occured");
        }
    }

}