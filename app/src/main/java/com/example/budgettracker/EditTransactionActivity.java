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

import androidx.appcompat.app.AlertDialog;
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

public class EditTransactionActivity extends AppCompatActivity {

    private RadioGroup typeRadioGroup;
    private RadioButton incomeRadioButton, expenseRadioButton;
    private TextInputEditText amountEditText, descriptionEditText;
    private Spinner categorySpinner;
    private Button selectDateButton, updateButton, deleteButton;
    private TextView selectedDateTextView;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private String transactionId;
    private Transaction currentTransaction;

    private Date selectedDate;
    private String[] incomeCategories = {"Salary", "Business", "Investments", "Gifts", "Other Income"};
    private String[] expenseCategories = {"Food", "Transportation", "Shopping", "Entertainment",
            "Bills", "Healthcare", "Education", "Travel", "Other Expense"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        transactionId = getIntent().getStringExtra("transactionId");
        if (transactionId == null) {
            Toast.makeText(this, "Error: Transaction not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initializeViews();

        loadTransactionData();

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
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadTransactionData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("transactions").document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        currentTransaction = documentSnapshot.toObject(Transaction.class);
                        if (currentTransaction != null) {
                            currentTransaction.setId(documentSnapshot.getId());
                            populateFields();
                        }
                    } else {
                        Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading transaction", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateFields() {
        if (currentTransaction.getType().equals("income")) {
            incomeRadioButton.setChecked(true);
            setupCategorySpinner(incomeCategories);
        } else {
            expenseRadioButton.setChecked(true);
            setupCategorySpinner(expenseCategories);
        }

        amountEditText.setText(String.valueOf(currentTransaction.getAmount()));

        descriptionEditText.setText(currentTransaction.getDescription());

        String[] categories = currentTransaction.getType().equals("income") ? incomeCategories : expenseCategories;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(currentTransaction.getCategory())) {
                categorySpinner.setSelection(i);
                break;
            }
        }

        selectedDate = currentTransaction.getDate();
        updateDateDisplay();
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

        updateButton.setOnClickListener(v -> updateTransaction());

        deleteButton.setOnClickListener(v -> confirmDelete());
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

    private void updateTransaction() {
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
            description = category;
        }

        progressBar.setVisibility(View.VISIBLE);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);

        Transaction updatedTransaction = new Transaction(userId, type, amount, category, description, selectedDate);

        db.collection("transactions").document(transactionId)
                .set(updatedTransaction)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    updateButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(EditTransactionActivity.this,
                            "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    updateButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(EditTransactionActivity.this,
                            "Error updating transaction: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransaction())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTransaction() {
        progressBar.setVisibility(View.VISIBLE);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);

        db.collection("transactions").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditTransactionActivity.this,
                            "Transaction deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    updateButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    Toast.makeText(EditTransactionActivity.this,
                            "Error deleting transaction: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}