package com.example.budgettracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.budgettracker.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private RadioGroup typeRadioGroup;
    private RadioButton incomeRadioButton, expenseRadioButton;
    private TextInputEditText amountEditText, descriptionEditText;
    private Spinner categorySpinner;
    private Button selectDateButton, saveButton;
    private TextView selectedDateTextView;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    private Date selectedDate;
    private String[] incomeCategories = {"Salary", "Business", "Investments", "Gifts", "Other Income"};
    private String[] expenseCategories = {"Food", "Transportation", "Shopping", "Entertainment",
            "Bills", "Healthcare", "Education", "Travel", "Other Expense"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

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

        selectedDate = new Date();
        updateDateDisplay();

        setupCategorySpinner(expenseCategories);

        setupListeners();
    }

    private void initializeViews() {
        typeRadioGroup = findViewById(R.id.typeRadioGroup);
        incomeRadioButton = findViewById(R.id.incomeRadioButton);
        expenseRadioButton = findViewById(R.id.expenseRadioButton);
        amountEditText = findViewById(R.id.amountEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        selectDateButton = findViewById(R.id.selectDateButton);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.incomeRadioButton) {
                setupCategorySpinner(incomeCategories);
            } else {
                setupCategorySpinner(expenseCategories);
            }
        });

        selectDateButton.setOnClickListener(v -> showDatePicker());

        saveButton.setOnClickListener(v -> saveTransaction());
    }

    private void setupCategorySpinner(String[] categories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, month, dayOfMonth);
                    selectedDate = newDate.getTime();
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
        selectedDateTextView.setText(dateFormat.format(selectedDate));
    }

    private void saveTransaction() {
        String amountStr = amountEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String type = incomeRadioButton.isChecked() ? "income" : "expense";

        if (TextUtils.isEmpty(amountStr)) {
            amountEditText.setError("Amount is required");
            amountEditText.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountEditText.setError("Amount must be greater than 0");
                amountEditText.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            amountEditText.setError("Invalid amount");
            amountEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            description = category; // Use category as default description
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        Transaction transaction = new Transaction(userId, type, amount, category, description, selectedDate);

        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(AddTransactionActivity.this,
                            "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(AddTransactionActivity.this,
                            "Error saving transaction: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}