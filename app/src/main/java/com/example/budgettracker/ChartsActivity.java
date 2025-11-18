package com.example.budgettracker;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.budgettracker.models.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChartsActivity extends AppCompatActivity {

    private PieChart expensePieChart, incomePieChart;
    private LineChart monthlyTrendChart;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        expensePieChart = findViewById(R.id.expensePieChart);
        incomePieChart = findViewById(R.id.incomePieChart);
        monthlyTrendChart = findViewById(R.id.monthlyTrendChart);

        loadExpenseData();
        loadIncomeData();
        loadMonthlyTrendData();
    }

    // ------------------- SAFE CHART DATA LOADING -------------------

    private void loadExpenseData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long monthStart = calendar.getTimeInMillis();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, Float> totals = new HashMap<>();
                    for (QueryDocumentSnapshot d : query) {
                        Transaction t = d.toObject(Transaction.class);
                        if (!"expense".equals(t.getType())) continue;
                        if (t.getTimestamp() < monthStart) continue;

                        totals.put(t.getCategory(),
                                totals.getOrDefault(t.getCategory(), 0f) + (float) t.getAmount());
                    }
                    displayPieChart(expensePieChart, totals, "No expense data");
                });
    }

    private void loadIncomeData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long monthStart = calendar.getTimeInMillis();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, Float> totals = new HashMap<>();
                    for (QueryDocumentSnapshot d : query) {
                        Transaction t = d.toObject(Transaction.class);
                        if (!"income".equals(t.getType())) continue;
                        if (t.getTimestamp() < monthStart) continue;

                        totals.put(t.getCategory(),
                                totals.getOrDefault(t.getCategory(), 0f) + (float) t.getAmount());
                    }
                    displayPieChart(incomePieChart, totals, "No income data");
                });
    }

    private void loadMonthlyTrendData() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -5);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long sixMonthsAgo = calendar.getTimeInMillis();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, Float> income = new HashMap<>();
                    Map<String, Float> expenses = new HashMap<>();
                    SimpleDateFormat fmt = new SimpleDateFormat("MMM", Locale.getDefault());

                    for (QueryDocumentSnapshot d : query) {
                        Transaction t = d.toObject(Transaction.class);
                        if (t.getTimestamp() < sixMonthsAgo) continue;

                        String month = fmt.format(t.getDate());
                        float amount = (float) t.getAmount();

                        if ("income".equals(t.getType())) {
                            income.put(month, income.getOrDefault(month, 0f) + amount);
                        } else {
                            expenses.put(month, expenses.getOrDefault(month, 0f) + amount);
                        }
                    }

                    displayLineChart(income, expenses);
                });
    }

    // ------------------- PIE CHART DISPLAY -------------------

    private void displayPieChart(PieChart chart, Map<String, Float> data, String emptyText) {
        if (data.isEmpty()) {
            chart.setNoDataText(emptyText);
            chart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getChartColors());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return NumberFormat.getCurrencyInstance(Locale.US).format(value);
            }
        });

        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.getDescription().setEnabled(false);
        chart.setDrawEntryLabels(true);
        chart.setEntryLabelTextSize(11f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setCenterText("Total");
        chart.setCenterTextSize(14f);
        chart.setHoleRadius(40f);
        chart.setTransparentCircleRadius(45f);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);

        chart.animateY(1000);
        chart.invalidate();
    }

    // ------------------- LINE CHART DISPLAY -------------------

    private void displayLineChart(Map<String, Float> incomeData, Map<String, Float> expenseData) {
        List<String> months = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        for (int i = 5; i >= 0; i--) {
            calendar.add(Calendar.MONTH, -i);
            months.add(monthFormat.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, i);
        }

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < months.size(); i++) {
            String month = months.get(i);
            incomeEntries.add(new Entry(i, incomeData.getOrDefault(month, 0f)));
            expenseEntries.add(new Entry(i, expenseData.getOrDefault(month, 0f)));
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(Color.parseColor("#2E7D32"));
        incomeSet.setCircleColor(Color.parseColor("#2E7D32"));
        incomeSet.setLineWidth(2f);
        incomeSet.setCircleRadius(4f);
        incomeSet.setDrawValues(false);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Expenses");
        expenseSet.setColor(Color.parseColor("#C62828"));
        expenseSet.setCircleColor(Color.parseColor("#C62828"));
        expenseSet.setLineWidth(2f);
        expenseSet.setCircleRadius(4f);
        expenseSet.setDrawValues(false);

        LineData lineData = new LineData(incomeSet, expenseSet);
        monthlyTrendChart.setData(lineData);
        monthlyTrendChart.getDescription().setEnabled(false);
        monthlyTrendChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < months.size()) return months.get(index);
                return "";
            }
        });
        monthlyTrendChart.getXAxis().setGranularity(1f);
        monthlyTrendChart.getXAxis().setLabelCount(months.size());
        monthlyTrendChart.animateX(1000);
        monthlyTrendChart.invalidate();
    }

    // ------------------- HELPER: COLORS -------------------

    private int[] getChartColors() {
        return new int[]{
                Color.parseColor("#FF6384"),
                Color.parseColor("#36A2EB"),
                Color.parseColor("#FFCE56"),
                Color.parseColor("#4BC0C0"),
                Color.parseColor("#9966FF"),
                Color.parseColor("#FF9F40"),
                Color.parseColor("#FF6384"),
                Color.parseColor("#C9CBCF")
        };
    }
}
