package com.joseph.spare;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.crashlytics.android.Crashlytics;
import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.joseph.spare.adapaters.AmenitiesAdapter;
import com.joseph.spare.domain.Item;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.AppDatabase;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;
import okhttp3.ResponseBody;
import rx.Completable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlaceDetails extends AppCompatActivity implements OnMapReadyCallback {

    @BindView(R.id.slider_layout)
    SliderLayout sliderLayout;
    @BindView(R.id.indicator)
    PagerIndicator pagerIndicator;
    AmenitiesAdapter amenitiesAdapter;
    @BindView(R.id.amenities_view)
    RecyclerView amenitiesView;
    @BindView(R.id.back_btn)
    ImageButton backBtn;

    @BindView(R.id.image)
    ImageView image;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.quotation)
    TextView quotation;
    @BindView(R.id.posted)
    TextView posted;
    @BindView(R.id.likes_icon_switcher)
    ViewSwitcher likesIconSwicher;
    //    @BindView(R.id.posted)
//    TextView posted;
    @BindView(R.id.bedrooms)
    TextView bedrooms;
    @BindView(R.id.bathrooms)
    TextView bathrooms;
    @BindView(R.id.area)
    TextView area;
    @BindView(R.id.description)
    TextView description;
    @BindView(R.id.transparent_image)
    ImageView transparent_image;
    @BindView(R.id.amenities_c)
    View amenities_c;

    @BindView(R.id.call_btn)
    ImageButton callBtn;
    @BindView(R.id.email_btn)
    ImageButton emailBtn;
    @BindView(R.id.website_btn)
    ImageButton websiteBtn;
    @BindView(R.id.share_btn)
    ImageButton shareBtn;
    @BindView(R.id.bookmarked)
    ImageButton unbookmarked;
    @BindView(R.id.like_txt)
    TextView likeTxt;


    Item item;
    private GoogleMap mMap;
    private Subscription likeSubscription;
    User user;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);
        ButterKnife.bind(this);

        Fabric.with(this, new Crashlytics());
        item = (Item) getIntent().getExtras().get("item");
        user = (User) getIntent().getExtras().get("user");
        setUpContent();

        final NestedScrollView nestedScrollView = findViewById(R.id.scrollView);
        transparent_image.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case 0:
                    nestedScrollView.requestDisallowInterceptTouchEvent(true);
                    return false;
                case 1:
                    nestedScrollView.requestDisallowInterceptTouchEvent(false);
                    return true;
                case 2:
                    nestedScrollView.requestDisallowInterceptTouchEvent(true);
                    return false;
                default:
                    return true;
            }
        });

        registerView();
    }

    private void registerView() {
        if(NetworkUtils.isNetworkAvailable(this)){
            NetworkUtils.getInstance().viewItem(item.getId(),user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String str=responseBody.string();
                                Log.i("VIEW",str);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    private void setUpContent() {
        name.setText(item.getName());
        String price = ServiceUtils.formatAmount(item.getPrice());
        this.price.setText(price);
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

        Log.i("LatLng",""+item.getLat()+"#"+item.getLon());

        quotation.setText(qt);
        setUpAmenities();
        //todo remove this
        bathrooms.setText("" + item.getBathrooms());
        bedrooms.setText("" + item.getBedrooms());
        String imageId = item.getImageUrls()[0];
        String url = Constants.RESOURCE_URL + imageId;
        Picasso.with(this).load(url).placeholder(R.drawable.grey_placeholder).into(image);
        String userImage = item.getUser().getImageUrl();
        likeTxt.setText(ServiceUtils.tranform(item.getLikes()));
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
        int min = calendar.get(Calendar.MINUTE);
        String m = "";
        if (min < 10) {
            m = "0" + min;
        } else {
            m = "" + min;
        }
        int hr = calendar.get(Calendar.HOUR_OF_DAY);
        if (hr > 12) {
            hr = hr - 12;
        }
        int amPm = calendar.get(Calendar.AM_PM);
        String s = amPm == 0 ? " A.M" : "P.M";
        Log.i("DATE", date.toString());
        String dt = month + " " + day + " at " + hr + ":" + m + " " + s;
        posted.setText(dt);
        if(item.isLiked()){
            likesIconSwicher.showNext();
        }else {
            likesIconSwicher.showPrevious();
        }

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
        description.setText(item.getDescription());
        String[] images = item.getImageUrls();
        if (images.length > 1) {
            for (int i = 0; i < images.length; i++) {
                String imageUrl = Constants.RESOURCE_URL + images[i];
                sliderLayout.addSlider(new DefaultSliderView(this).image(imageUrl));
            }
            sliderLayout.setCustomIndicator(pagerIndicator);
            if (image.getVisibility() != View.GONE) {
                image.setVisibility(View.GONE);
            }
        } else {
            String imageUrl = Constants.RESOURCE_URL + images[0];
            Picasso.with(this).load(imageUrl).placeholder(R.drawable.grey_placeholder).into(image);
            if (image.getVisibility() != View.VISIBLE) {
                image.setVisibility(View.VISIBLE);
            }
            if (sliderLayout.getVisibility() != View.GONE) {
                sliderLayout.setVisibility(View.GONE);
            }
            if (pagerIndicator.getVisibility() != View.GONE) {
                pagerIndicator.setVisibility(View.GONE);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setUpAmenities() {
        if (item.getAmenities() != null) {
            amenitiesAdapter = new AmenitiesAdapter(this, item.getAmenities());
            amenitiesView.setAdapter(amenitiesAdapter);
        } else {
            amenities_c.setVisibility(View.GONE);
        }


    }

    @OnClick(R.id.bookmarked)
    public void unBookMark() {
        unBookmarkItem(item);
    }
    public void unBookmarkItem(Item item) {
        //remove item from database
        //update button
        Completable.fromAction(() -> AppDatabase.getInstance(this).itemsDao().delete(item)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> runOnUiThread(() ->{
                    Toast.makeText(this, "Un-bookmarked.", Toast.LENGTH_SHORT).show();
                    unbookmarked.setVisibility(View.GONE);
                } ))
                .subscribe();
    }

    @OnClick(R.id.back_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng latLng = new LatLng(item.getLat(), item.getLon());
        mMap.addMarker(new MarkerOptions().position(latLng).title(item.getName()));
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(10.0f).tilt(70.0f).build()));

    }

    @OnClick(R.id.share_btn)
    public void onShare() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String text = item.getName() + " \n" + Constants.RESOURCE_URL + item.getImageUrls()[0];
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share item using"));
    }

    @OnClick(R.id.like_btn)
    public void onLike() {
        like();
    }

    @OnClick(R.id.unlike_btn)
    public void onUnlike() {
        like();
    }

    @OnClick(R.id.call_btn)
    public void onCall() {
        if (item.getPhone() != null && !item.getPhone().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + item.getPhone()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone not provided.", Toast.LENGTH_SHORT).show();
        }

    }

    @OnClick(R.id.email_btn)
    public void onEmail() {
        if (item.getEmail() != null && !item.getEmail().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, item.getEmail());
            intent.putExtra(Intent.EXTRA_SUBJECT, item.getName());

            startActivity(Intent.createChooser(intent, "Send Email via.."));
        } else {
            Toast.makeText(this, "Email not provided.", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.website_btn)
    public void onWebsite() {
        if (item.getWebsite() != null && item.getWebsite().isEmpty()) {
            if (Patterns.WEB_URL.matcher(item.getWebsite()).matches()) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(item.getWebsite()));
                startActivity(i);
            } else {
                Toast.makeText(this, "Provided website link not valid.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "Website not provided.", Toast.LENGTH_SHORT).show();

        }
    }

    private void like() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            likeSubscription = NetworkUtils.getInstance().like(item.getId(), user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            if (item.isLiked()) {
                                //unlike
                                item.setLiked(false);
                                likesIconSwicher.showPrevious();
                                item.setLikes(item.getLikes() - 1);
                                likeTxt.setText(ServiceUtils.tranform(item.getLikes()));
//                                items.get(position).setLiked(false);
//                                items.get(position).setLikes(item.getLikes()-1);
//                                adapter.notifyItemChanged(position);
                            } else {
                                //like
                                item.setLiked(true);
                                likesIconSwicher.showNext();
                                item.setLikes(item.getLikes() + 1);
                                likeTxt.setText(ServiceUtils.tranform(item.getLikes()));
//                                items.get(position).setLiked(true);
//                                items.get(position).setLikes(item.getLikes()+1);
//                                adapter.notifyItemChanged(position);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                         //   Toast.makeText(Dashboard.this, Constants.CONNECTION_ERROR, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String rsp = responseBody.string();
                                Log.i("LIKE", rsp);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }
}
