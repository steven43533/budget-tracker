package com.example.budgettracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.budgettracker.adapters.BudgetAdapter;
import com.example.budgettracker.models.Budget;
import com.example.budgettracker.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetSettingsActivity extends AppCompatActivity {

    private static final String TAG = "BudgetSettingsActivity";

    private Spinner categorySpinner;
    private TextInputEditText budgetLimitEditText;
    private Button addBudgetButton;
    private RecyclerView budgetsRecyclerView;
    private TextView noBudgetsTextView;
    private BudgetAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private String currentMonth;

    private String[] expenseCategories = {"Food", "Transportation", "Shopping", "Entertainment",
            "Bills", "Healthcare", "Education", "Travel", "Other Expense"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Get current month
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        currentMonth = monthFormat.format(Calendar.getInstance().getTime());

        Log.d(TAG, "Current month: " + currentMonth);
        Log.d(TAG, "User ID: " + userId);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        categorySpinner = findViewById(R.id.categorySpinner);
        budgetLimitEditText = findViewById(R.id.budgetLimitEditText);
        addBudgetButton = findViewById(R.id.addBudgetButton);
        budgetsRecyclerView = findViewById(R.id.budgetsRecyclerView);
        noBudgetsTextView = findViewById(R.id.noBudgetsTextView);

        // Setup category spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, expenseCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Setup RecyclerView
        budgetsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BudgetAdapter(new ArrayList<>(), this::deleteBudget);
        budgetsRecyclerView.setAdapter(adapter);

        // Setup listeners
        addBudgetButton.setOnClickListener(v -> addBudget());

        // Load budgets
        loadBudgets();
    }

    private void addBudget() {
        String category = categorySpinner.getSelectedItem().toString();
        String limitStr = budgetLimitEditText.getText().toString().trim();

        if (TextUtils.isEmpty(limitStr)) {
            budgetLimitEditText.setError("Limit is required");
            budgetLimitEditText.requestFocus();
            return;
        }

        double limit;
        try {
            limit = Double.parseDouble(limitStr);
            if (limit <= 0) {
                budgetLimitEditText.setError("Limit must be greater than 0");
                budgetLimitEditText.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            budgetLimitEditText.setError("Invalid amount");
            budgetLimitEditText.requestFocus();
            return;
        }

        Log.d(TAG, "Adding budget for category: " + category + ", limit: " + limit);

        // Check if budget already exists for this category
        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category)
                .whereEqualTo("month", currentMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Update existing budget
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Log.d(TAG, "Updating existing budget: " + docId);

                        db.collection("budgets").document(docId)
                                .update("limit", limit)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Budget updated!", Toast.LENGTH_SHORT).show();
                                    budgetLimitEditText.setText("");
                                    loadBudgets();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating budget", e);
                                    Toast.makeText(this, "Error updating budget", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Create new budget
                        Log.d(TAG, "Creating new budget");
                        Budget budget = new Budget(userId, category, limit, currentMonth);

                        db.collection("budgets")
                                .add(budget)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "Budget added successfully: " + documentReference.getId());
                                    Toast.makeText(this, "Budget added!", Toast.LENGTH_SHORT).show();
                                    budgetLimitEditText.setText("");
                                    loadBudgets();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding budget", e);
                                    Toast.makeText(this, "Error adding budget", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing budget", e);
                    Toast.makeText(this, "Error checking budget", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBudgets() {
        Log.d(TAG, "Loading budgets for user: " + userId + ", month: " + currentMonth);

        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("month", currentMonth)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading budgets", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Found " + value.size() + " budgets");
                        List<Budget> budgets = new ArrayList<>();

                        for (QueryDocumentSnapshot document : value) {
                            Budget budget = document.toObject(Budget.class);
                            budget.setId(document.getId());
                            budgets.add(budget);
                            Log.d(TAG, "Budget: " + budget.getCategory() + " - $" + budget.getLimit());
                        }

                        // Calculate spent amounts
                        calculateSpentAmounts(budgets);
                    } else {
                        Log.d(TAG, "No budgets found");
                        noBudgetsTextView.setVisibility(View.VISIBLE);
                        budgetsRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private void calculateSpentAmounts(List<Budget> budgets) {
        // Get current month start timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        Log.d(TAG, "Calculating spent amounts from timestamp: " + monthStart);

        // Use addSnapshotListener for real-time updates
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "expense")
                .whereGreaterThanOrEqualTo("timestamp", monthStart)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error calculating spent amounts", error);
                        // Still show budgets even if spent calculation fails
                        noBudgetsTextView.setVisibility(View.GONE);
                        budgetsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.updateBudgets(budgets);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " expense transactions");

                        // Calculate spent per category
                        for (Budget budget : budgets) {
                            double spent = 0.0;
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Transaction transaction = document.toObject(Transaction.class);
                                if (transaction.getCategory().equals(budget.getCategory())) {
                                    spent += transaction.getAmount();
                                }
                            }
                            budget.setSpent(spent);
                            Log.d(TAG, "Category: " + budget.getCategory() +
                                    ", Limit: $" + budget.getLimit() +
                                    ", Spent: $" + spent);
                        }

                        // Update UI
                        noBudgetsTextView.setVisibility(View.GONE);
                        budgetsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.updateBudgets(budgets);
                    }
                });
    }

    private void deleteBudget(Budget budget) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete this budget?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("budgets").document(budget.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Budget deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting budget", e);
                                Toast.makeText(this, "Error deleting budget", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}