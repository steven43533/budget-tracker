package com.example.budgettracker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.budgettracker.adapters.TransactionAdapter;
import com.example.budgettracker.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionListActivity extends AppCompatActivity {

    private RecyclerView transactionsRecyclerView;
    private TextView emptyTextView;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipIncome, chipExpense;
    private TransactionAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private List<Transaction> allTransactions;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipIncome = findViewById(R.id.chipIncome);
        chipExpense = findViewById(R.id.chipExpense);

        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(allTransactions, transaction -> {
        });
        transactionsRecyclerView.setAdapter(adapter);

        setupFilterListeners();

        loadTransactions();
    }

    private void setupFilterListeners() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) {
                currentFilter = "all";
            } else if (checkedId == R.id.chipIncome) {
                currentFilter = "income";
            } else if (checkedId == R.id.chipExpense) {
                currentFilter = "expense";
            }
            filterTransactions();
        });
    }

    private void loadTransactions() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (value != null) {
                        allTransactions.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            allTransactions.add(transaction);
                        }
                        filterTransactions();
                    }
                });
    }

    private void filterTransactions() {
        List<Transaction> filteredList = new ArrayList<>();

        for (Transaction transaction : allTransactions) {
            if (currentFilter.equals("all")) {
                filteredList.add(transaction);
            } else if (transaction.getType().equals(currentFilter)) {
                filteredList.add(transaction);
            }
        }

        if (filteredList.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            transactionsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            transactionsRecyclerView.setVisibility(View.VISIBLE);
            adapter.updateTransactions(filteredList);
        }
    }
}