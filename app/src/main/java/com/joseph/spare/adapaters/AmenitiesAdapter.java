package com.joseph.spare.adapaters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.joseph.spare.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AmenitiesAdapter extends RecyclerView.Adapter<AmenitiesAdapter.AmenityViewHolder> {
    Context mContext;
    String[] amenities;

    public AmenitiesAdapter(Context mContext, String[] amenities) {
        this.mContext = mContext;
        this.amenities = amenities;
    }

    @NonNull
    @Override
    public AmenityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AmenityViewHolder(LayoutInflater.from(mContext).inflate(R.layout.tag_view,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull AmenityViewHolder holder, int position) {
        CardView container=holder.container;
        TextView text=holder.text;
        String textString=amenities[position];
//        Random rand = new Random();
//        int r = rand.nextInt(255);
//        int g = rand.nextInt(255);
//        int b = rand.nextInt(255);
//        int randomColor = Color.rgb(200,200,200);
//        container.setCardBackgroundColor(randomColor);
        text.setText(textString);
    }

    @Override
    public int getItemCount() {
        return amenities.length;
    }

    public class AmenityViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.amenity_container)
        CardView container;
        @BindView(R.id.amenity)
        TextView text;

        public AmenityViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
