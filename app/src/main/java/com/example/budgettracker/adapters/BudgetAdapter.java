package com.example.budgettracker.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgettracker.R;
import com.example.budgettracker.models.Budget;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgets;
    private OnBudgetActionListener listener;

    public interface OnBudgetActionListener {
        void onDeleteBudget(Budget budget);
    }

    public BudgetAdapter(List<Budget> budgets, OnBudgetActionListener listener) {
        this.budgets = budgets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgets.get(position);
        holder.bind(budget);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    public void updateBudgets(List<Budget> newBudgets) {
        this.budgets = newBudgets;
        notifyDataSetChanged();
    }

    class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView, budgetInfoTextView, percentageTextView;
        ProgressBar budgetProgressBar;
        Button deleteButton;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            budgetInfoTextView = itemView.findViewById(R.id.budgetInfoTextView);
            percentageTextView = itemView.findViewById(R.id.percentageTextView);
            budgetProgressBar = itemView.findViewById(R.id.budgetProgressBar);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Budget budget) {
            categoryTextView.setText(budget.getCategory());

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            String info = "Spent " + currencyFormat.format(budget.getSpent()) +
                    " of " + currencyFormat.format(budget.getLimit());
            budgetInfoTextView.setText(info);

            int percentage = budget.getPercentageUsed();
            budgetProgressBar.setProgress(percentage);
            percentageTextView.setText(percentage + "% used");

            if (percentage >= 100) {
                percentageTextView.setTextColor(Color.parseColor("#C62828")); // Red
            } else if (percentage >= 80) {
                percentageTextView.setTextColor(Color.parseColor("#F57C00")); // Orange
            } else {
                percentageTextView.setTextColor(Color.parseColor("#2E7D32")); // Green
            }

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteBudget(budget);
                }
            });
        }
    }
}