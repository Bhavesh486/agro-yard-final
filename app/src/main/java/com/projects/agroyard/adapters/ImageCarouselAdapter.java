package com.projects.agroyard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.projects.agroyard.R;
import com.projects.agroyard.model.Slide;
import java.util.List;

public class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.CarouselViewHolder> {
    private List<Slide> slideList;
    private Context context;

    public ImageCarouselAdapter(List<Slide> slideList, Context context) {
        this.slideList = slideList;
        this.context = context;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.slide_item, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Slide slide = slideList.get(position);
        holder.titleTextView.setText(slide.getTitle());
        holder.descriptionTextView.setText(slide.getDescription());
        // Glide.with(context).load(slide.getImageUrl()).centerCrop().into(holder.slideImageView);
    }

    @Override
    public int getItemCount() {
        return slideList.size();
    }

    public class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView slideImageView;
        TextView titleTextView, descriptionTextView;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            slideImageView = itemView.findViewById(R.id.slide_image);
            titleTextView = itemView.findViewById(R.id.slide_title);
            descriptionTextView = itemView.findViewById(R.id.slide_description);
        }
    }
}