package com.example.budgettracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.budgettracker.adapters.TransactionAdapter;
import com.example.budgettracker.models.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView welcomeTextView, currentMonthTextView;
    private TextView totalIncomeTextView, totalExpenseTextView, balanceTextView;
    private TextView noTransactionsTextView;
    private Button addTransactionButton, viewTransactionsButton, budgetsButton, chartsButton;
    private FloatingActionButton fab;
    private RecyclerView recentTransactionsRecyclerView;
    private TransactionAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageButton refreshRecentButton;

    private String userId;

    private double totalIncome = 0.0;
    private double totalExpense = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        userId = currentUser.getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        refreshRecentButton = findViewById(R.id.refreshRecentButton);

        setSupportActionBar(toolbar);

        initializeViews();

        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            welcomeTextView.setText("Welcome back, " + displayName + "!");
        }

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        currentMonthTextView.setText(monthFormat.format(Calendar.getInstance().getTime()));

        setupRecyclerView();

        setupButtonListeners();

        loadMonthlyData();
        loadRecentTransactions();

        refreshRecentButton.setOnClickListener(v -> {
            loadRecentTransactions();
        });
    }

    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        currentMonthTextView = findViewById(R.id.currentMonthTextView);
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView);
        totalExpenseTextView = findViewById(R.id.totalExpenseTextView);
        balanceTextView = findViewById(R.id.balanceTextView);
        noTransactionsTextView = findViewById(R.id.noTransactionsTextView);
        addTransactionButton = findViewById(R.id.addTransactionButton);
        viewTransactionsButton = findViewById(R.id.viewTransactionsButton);
        budgetsButton = findViewById(R.id.budgetsButton);
        chartsButton = findViewById(R.id.chartsButton);
        fab = findViewById(R.id.fab);
        recentTransactionsRecyclerView = findViewById(R.id.recentTransactionsRecyclerView);
    }

    private void setupRecyclerView() {
        recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(new ArrayList<>(), transaction -> {
            // Open edit transaction activity
            Intent intent = new Intent(MainActivity.this, EditTransactionActivity.class);
            intent.putExtra("transactionId", transaction.getId());
            startActivity(intent);
        });
        recentTransactionsRecyclerView.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        addTransactionButton.setOnClickListener(v -> openAddTransaction());
        fab.setOnClickListener(v -> openAddTransaction());

        viewTransactionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TransactionListActivity.class);
            startActivity(intent);
        });

        budgetsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BudgetSettingsActivity.class);
            startActivity(intent);
        });

        chartsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChartsActivity.class);
            startActivity(intent);
        });
    }

    private void openAddTransaction() {
        Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
        startActivity(intent);
    }

    private void loadMonthlyData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        Log.d(TAG, "Loading monthly data for user: " + userId);
        Log.d(TAG, "Month start timestamp: " + monthStart);

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", monthStart)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading monthly data", error);
                        return;
                    }

                    totalIncome = 0.0;
                    totalExpense = 0.0;

                    if (value != null) {
                        Log.d(TAG, "Found " + value.size() + " transactions");
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            Log.d(TAG, "Transaction: " + transaction.getType() + " - $" + transaction.getAmount());

                            if (transaction.getType().equals("income")) {
                                totalIncome += transaction.getAmount();
                            } else {
                                totalExpense += transaction.getAmount();
                            }
                        }
                    }

                    Log.d(TAG, "Total Income: $" + totalIncome);
                    Log.d(TAG, "Total Expense: $" + totalExpense);
                    updateSummaryViews();
                });
    }

    private void updateSummaryViews() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        totalIncomeTextView.setText(currencyFormat.format(totalIncome));
        totalExpenseTextView.setText(currencyFormat.format(totalExpense));
        balanceTextView.setText(currencyFormat.format(totalIncome - totalExpense));
    }

    private void loadRecentTransactions() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading recent transactions", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        }
                        adapter.updateTransactions(transactions);
                        noTransactionsTextView.setVisibility(View.GONE);
                        recentTransactionsRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        noTransactionsTextView.setVisibility(View.VISIBLE);
                        recentTransactionsRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMonthlyData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            mAuth.signOut();
            navigateToLogin();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}