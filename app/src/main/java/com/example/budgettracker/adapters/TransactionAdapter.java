package com.example.budgettracker.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.budgettracker.R;
import com.example.budgettracker.models.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView, descriptionTextView, dateTextView, amountTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransactionClick(transactions.get(position));
                }
            });
        }

        public void bind(Transaction transaction) {
            categoryTextView.setText(transaction.getCategory());
            descriptionTextView.setText(transaction.getDescription());

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateTextView.setText(dateFormat.format(transaction.getDate()));

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            String formattedAmount = currencyFormat.format(transaction.getAmount());

            if (transaction.getType().equals("income")) {
                amountTextView.setText("+" + formattedAmount);
                amountTextView.setTextColor(Color.parseColor("#2E7D32")); // Green
            } else {
                amountTextView.setText("-" + formattedAmount);
                amountTextView.setTextColor(Color.parseColor("#C62828")); // Red
            }
        }
    }
}