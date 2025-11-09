package com.example.budgettracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTextView, emailTextView;
    private TextView totalTransactionsTextView, activeBudgetsTextView, memberSinceTextView;
    private Button manageCategoriesButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        nameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        totalTransactionsTextView = findViewById(R.id.totalTransactionsTextView);
        activeBudgetsTextView = findViewById(R.id.activeBudgetsTextView);
        memberSinceTextView = findViewById(R.id.memberSinceTextView);
        manageCategoriesButton = findViewById(R.id.manageCategoriesButton);
        logoutButton = findViewById(R.id.logoutButton);

        loadUserInfo(currentUser);
        loadStatistics();

        manageCategoriesButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, CategoryManagementActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> logout());
    }

    private void loadUserInfo(FirebaseUser user) {
        nameTextView.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
        emailTextView.setText(user.getEmail());

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long createdAt = documentSnapshot.getLong("createdAt");
                        if (createdAt != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                            memberSinceTextView.setText(dateFormat.format(new Date(createdAt)));
                        }
                    }
                });
    }

    private void loadStatistics() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalTransactionsTextView.setText(String.valueOf(queryDocumentSnapshots.size()));
                });

        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonth = monthFormat.format(Calendar.getInstance().getTime());

        db.collection("budgets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("month", currentMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    activeBudgetsTextView.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}