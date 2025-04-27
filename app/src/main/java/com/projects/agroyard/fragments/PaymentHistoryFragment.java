package com.projects.agroyard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.projects.agroyard.R;

import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryFragment extends Fragment {

    private LinearLayout paymentHistoryContainer;

    public PaymentHistoryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_payment_history, container, false);
        
        paymentHistoryContainer = view.findViewById(R.id.payment_history_container);
        
        // Load sample payment history data
        loadPaymentHistory();
        
        return view;
    }
    
    private void loadPaymentHistory() {
        // Sample data - in a real app, this would come from a database or API
        List<PaymentRecord> paymentRecords = getSamplePaymentRecords();
        
        // Display each payment record
        for (PaymentRecord record : paymentRecords) {
            addPaymentRecordView(record);
        }
    }
    
    private void addPaymentRecordView(PaymentRecord record) {
        // Inflate the payment record item layout
        View recordView = getLayoutInflater().inflate(R.layout.item_payment_history, paymentHistoryContainer, false);
        
        // Set payment details
        TextView dateView = recordView.findViewById(R.id.payment_date);
        TextView amountView = recordView.findViewById(R.id.payment_amount);
        TextView fromView = recordView.findViewById(R.id.payment_from);
        TextView statusView = recordView.findViewById(R.id.payment_status);
        TextView purposeView = recordView.findViewById(R.id.payment_purpose);
        
        dateView.setText(record.date);
        amountView.setText("₹" + record.amount);
        fromView.setText("From: " + record.from);
        statusView.setText(record.status);
        purposeView.setText("Purpose: " + record.purpose);
        
        // Set status color
        if ("Completed".equals(record.status)) {
            statusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if ("Pending".equals(record.status)) {
            statusView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
        
        // Add the view to the container
        paymentHistoryContainer.addView(recordView);
    }
    
    private List<PaymentRecord> getSamplePaymentRecords() {
        List<PaymentRecord> records = new ArrayList<>();
        
        // Add sample records
        records.add(new PaymentRecord("15 Apr 2023", "12,500", "Market Member", "Completed", "Wheat Delivery"));
        records.add(new PaymentRecord("02 Apr 2023", "8,750", "Sarah Member", "Completed", "Rice Purchase"));
        records.add(new PaymentRecord("28 Mar 2023", "22,000", "Agro Supplies Ltd", "Completed", "Tomato Bulk Order"));
        records.add(new PaymentRecord("15 Mar 2023", "5,300", "Local Vendor", "Completed", "Vegetable Supply"));
        records.add(new PaymentRecord("01 Mar 2023", "15,250", "Market Member", "Pending", "Seasonal Produce"));
        
        return records;
    }
    
    // Simple data class for payment records
    private static class PaymentRecord {
        String date;
        String amount;
        String from;
        String status;
        String purpose;
        
        PaymentRecord(String date, String amount, String from, String status, String purpose) {
            this.date = date;
            this.amount = amount;
            this.from = from;
            this.status = status;
            this.purpose = purpose;
        }
    }
} 