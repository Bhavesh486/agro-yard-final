package com.projects.agroyard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.projects.agroyard.R;
import com.projects.agroyard.model.MarketInsight;
import java.util.List;

public class MarketInsightAdapter extends RecyclerView.Adapter<MarketInsightAdapter.ViewHolder> {
    private List<MarketInsight> insightsList;
    private Context context;

    public MarketInsightAdapter(List<MarketInsight> insightsList, Context context) {
        this.insightsList = insightsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_market_insight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MarketInsight insight = insightsList.get(position);
        holder.cropName.setText(insight.getCropName());
        holder.price.setText(insight.getPrice());
        holder.changePercentage.setText(insight.getChangePercentage());
        holder.description.setText(insight.getDescription());

        Glide.with(context).load(insight.getImageUrl()).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return insightsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cropName, price, changePercentage, description;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cropName = itemView.findViewById(R.id.crop_name);
            price = itemView.findViewById(R.id.price);
            changePercentage = itemView.findViewById(R.id.change_percentage);
            description = itemView.findViewById(R.id.description);
            image = itemView.findViewById(R.id.crop_image);
        }
    }
}
