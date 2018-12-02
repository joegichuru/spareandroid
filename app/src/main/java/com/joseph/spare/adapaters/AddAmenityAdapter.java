package com.joseph.spare.adapaters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.joseph.spare.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddAmenityAdapter extends RecyclerView.Adapter<AddAmenityAdapter.AddAmenityViewHolder> {
    private Context context;
    private String[] amenities;
    private AmenityCheckListener listener;

    public AddAmenityAdapter(Context context, String[] amenities, AmenityCheckListener listener) {
        this.context = context;
        this.amenities = amenities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddAmenityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AddAmenityViewHolder(LayoutInflater.from(context).inflate(R.layout.amenity_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AddAmenityViewHolder holder, int position) {
        String amenity = amenities[position];
        holder.name.setText(amenity);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                listener.onAmenitySelected(holder.getAdapterPosition());
            } else {
                listener.onAmenityUnselected(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return amenities.length;
    }

    class AddAmenityViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.checkbox)
        CheckBox checkBox;
        @BindView(R.id.name)
        TextView name;

        AddAmenityViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface AmenityCheckListener {
        void onAmenitySelected(int index);

        void onAmenityUnselected(int index);
    }
}
