package com.joseph.spare.adapaters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.joseph.spare.R;
import com.joseph.spare.callbacks.ItemCallBack;
import com.joseph.spare.domain.Item;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.ServiceUtils;
import com.squareup.picasso.Picasso;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemsViewHolder> {
    private Context context;
    private List<Item> items;
    private ItemCallBack itemCallBack;

    public ItemsAdapter(Context context, List<Item> items,ItemCallBack itemCallBack) {
        this.context = context;
        this.items = items;
        this.itemCallBack=itemCallBack;
    }

    @NonNull
    @Override
    public ItemsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemsViewHolder(LayoutInflater.from(context).inflate(R.layout.place_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemsViewHolder holder, int position) {
        Item item = items.get(position);
        holder.userName.setText(item.getUser().getName());
        String t = "for " + item.getItemType();
        holder.itemType.setText(t);
        holder.name.setText(item.getName());
        String price = ServiceUtils.formatAmount(item.getPrice());
        holder.price.setText(price);
        String qt = "p.m";
        switch (item.getDuration()) {
            case "month":
                qt = "p.m";
                break;
            case "year":
                qt = "p.a";
                break;
            default:
                qt = "";
        }
        holder.quotation.setText(qt);
//
//        item.setCity("Nairobi");
        holder.city.setText(item.getCity());
        holder.bathrooms.setText(""+item.getBathrooms());
        holder.bedrooms.setText(""+item.getBedrooms());
        String imageId = item.getImageUrls()[0];
        String url = Constants.RESOURCE_URL + imageId;
        Picasso.with(context).load(url).placeholder(R.drawable.grey_placeholder).into(holder.image);
        String userImage = item.getUser().getImageUrl();
        String userImageUrl=Constants.RESOURCE_URL+userImage;
        if (userImage != null) {

            Picasso.with(context).load(userImageUrl).placeholder(R.drawable.user).into(holder.avatar);
        } else {
            Picasso.with(context).load(R.drawable.user).into(holder.avatar);
        }
        //format date to be mm date at time;
        //or x hrs/minutes ago if t<12 hrs
        long hrs = 43200000;
        Date now = new Date();
        long tnow = now.getTime();
        long diff = tnow - item.getPostedOn();
        //parse as real date
        Date date = new Date(item.getPostedOn());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
        String[] shortMonths = dateFormatSymbols.getShortMonths();
        String month = shortMonths[calendar.get(Calendar.MONTH)];
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int min=calendar.get(Calendar.MINUTE);
        String m="";
        if(min<10){
            m="0"+min;
        }else {
            m=""+min;
        }
        int hr=calendar.get(Calendar.HOUR_OF_DAY);
        if(hr>12){
            hr=hr-12;
        }
        int amPm=calendar.get(Calendar.AM_PM);
        String s=amPm==0?" A.M":"P.M";
        Log.i("DATE",date.toString());
        String dt=month+" "+day+" at "+hr+":"+m+" "+s;
        holder.posted.setText(dt);
        holder.likeTxt.setText(ServiceUtils.tranform(item.getLikes()));
        if(item.isLiked()){
            holder.likesIconSwicher.showNext();
        }else {
            holder.likesIconSwicher.showPrevious();
        }
        Log.i("Is liked",""+item.isLiked());
        //todo parse time as instance
        //todo parse time as date if longer than a year
        //todo parse time as refference if less than a year

//        if (diff > hrs) {
//            //parse as real date
//            Date date = new Date(item.getPostedOn());
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(date);
//            DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
//            String[] shortMonths = dateFormatSymbols.getShortMonths();
//            String month = shortMonths[calendar.get(Calendar.MONTH)];
//            int day = calendar.get(Calendar.DAY_OF_MONTH);
//            int hr=calendar.get(Calendar.HOUR_OF_DAY);
//            int amPm=calendar.get(Calendar.AM_PM);
//            String s=amPm==0?" a.m":"p.m";
//            String dt=month+" "+day+" at "+hr+" "+s;
//            holder.posted.setText(dt);
//
//        } else {
//            //todo
//            //pass as instance
//        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ItemsViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar)
        CircleImageView avatar;
        @BindView(R.id.user_name)
        TextView userName;
        @BindView(R.id.type)
        TextView itemType;
        @BindView(R.id.unbookmarked)
        ImageView bookMark;
        @BindView(R.id.bookmarked)
        ImageView bookmarked;
        @BindView(R.id.bookmark_switcher)
        ViewSwitcher bookmarkSwitcher;
        @BindView(R.id.image)
        ImageView image;
        @BindView(R.id.more_btn)
        ImageButton moreBtn;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.price)
        TextView price;
        @BindView(R.id.quotation)
        TextView quotation;
        @BindView(R.id.likes_icon_switcher)
        ViewSwitcher likesIconSwicher;
        @BindView(R.id.like_btn)
        ImageButton likeBtn;
        @BindView(R.id.unlike_btn)
        ImageButton unlikeBtn;
        @BindView(R.id.like_txt)
        TextView likeTxt;
        @BindView(R.id.posted)
        TextView posted;
        @BindView(R.id.city)
        TextView city;
        @BindView(R.id.bedrooms)
        TextView bedrooms;
        @BindView(R.id.bathrooms)
        TextView bathrooms;
        @BindView(R.id.area)
        TextView area;

        @BindView(R.id.root)
        View root;


        public ItemsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            avatar.setOnClickListener(v->itemCallBack.onAvatarClicked(getAdapterPosition()));
            bookMark.setOnClickListener(v->{
                itemCallBack.onItemBookmarked(getAdapterPosition());
                bookmarkSwitcher.showNext();
            });
            bookmarked.setOnClickListener(v->{
                itemCallBack.onItemUnbookmarked(getAdapterPosition());
                bookmarkSwitcher.showPrevious();
            });
            likeBtn.setOnClickListener(v->{
                itemCallBack.onItemLiked(getAdapterPosition());
                //likesIconSwicher.showPrevious();
            });
            unlikeBtn.setOnClickListener(v->{
                itemCallBack.onItemLiked(getAdapterPosition());
               // likesIconSwicher.showNext();
            });
            root.setOnClickListener(v->itemCallBack.onItemClicked(getAdapterPosition()));

        }
    }
}
