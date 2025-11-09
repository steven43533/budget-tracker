package com.example.budgettracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.budgettracker.models.Transaction;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class IncomeSetupActivity extends AppCompatActivity {

    private TextInputEditText salaryEditText, freelanceEditText, businessEditText;
    private TextInputEditText investmentEditText, otherIncomeEditText;
    private TextView totalIncomeTextView;
    private Button saveButton, skipButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_setup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initializeViews();

        setupTextWatchers();
        setupButtonListeners();
    }

    private void initializeViews() {
        salaryEditText = findViewById(R.id.salaryEditText);
        freelanceEditText = findViewById(R.id.freelanceEditText);
        businessEditText = findViewById(R.id.businessEditText);
        investmentEditText = findViewById(R.id.investmentEditText);
        otherIncomeEditText = findViewById(R.id.otherIncomeEditText);
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView);
        saveButton = findViewById(R.id.saveButton);
        skipButton = findViewById(R.id.skipButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalIncome();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        salaryEditText.addTextChangedListener(textWatcher);
        freelanceEditText.addTextChangedListener(textWatcher);
        businessEditText.addTextChangedListener(textWatcher);
        investmentEditText.addTextChangedListener(textWatcher);
        otherIncomeEditText.addTextChangedListener(textWatcher);
    }

    private void setupButtonListeners() {
        saveButton.setOnClickListener(v -> saveIncomeData());
        skipButton.setOnClickListener(v -> navigateToMainActivity());
    }

    private void updateTotalIncome() {
        double total = 0.0;

        total += getAmount(salaryEditText);
        total += getAmount(freelanceEditText);
        total += getAmount(businessEditText);
        total += getAmount(investmentEditText);
        total += getAmount(otherIncomeEditText);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        totalIncomeTextView.setText(currencyFormat.format(total));
    }

    private double getAmount(TextInputEditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return 0.0;

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void saveIncomeData() {
        double salary = getAmount(salaryEditText);
        double freelance = getAmount(freelanceEditText);
        double business = getAmount(businessEditText);
        double investment = getAmount(investmentEditText);
        double otherIncome = getAmount(otherIncomeEditText);

        if (salary == 0 && freelance == 0 && business == 0 && investment == 0 && otherIncome == 0) {
            Toast.makeText(this, "Please enter at least one income source", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        skipButton.setEnabled(false);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date = calendar.getTime();

        final int[] completedSaves = {0};
        final int totalSaves = (salary > 0 ? 1 : 0) + (freelance > 0 ? 1 : 0) +
                (business > 0 ? 1 : 0) + (investment > 0 ? 1 : 0) +
                (otherIncome > 0 ? 1 : 0);

        if (salary > 0) {
            saveIncomeTransaction("Salary", salary, date, completedSaves, totalSaves);
        }
        if (freelance > 0) {
            saveIncomeTransaction("Business", freelance, date, completedSaves, totalSaves);
        }
        if (business > 0) {
            saveIncomeTransaction("Business", business, date, completedSaves, totalSaves);
        }
        if (investment > 0) {
            saveIncomeTransaction("Investments", investment, date, completedSaves, totalSaves);
        }
        if (otherIncome > 0) {
            saveIncomeTransaction("Other Income", otherIncome, date, completedSaves, totalSaves);
        }
    }

    private void saveIncomeTransaction(String category, double amount, Date date,
                                       int[] completedSaves, int totalSaves) {
        Transaction transaction = new Transaction(userId, "income", amount, category,
                "Monthly " + category, date);

        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    completedSaves[0]++;
                    if (completedSaves[0] == totalSaves) {
                        // All saves completed
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(IncomeSetupActivity.this,
                                "Income information saved successfully!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    skipButton.setEnabled(true);
                    Toast.makeText(IncomeSetupActivity.this,
                            "Error saving income: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(IncomeSetupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}