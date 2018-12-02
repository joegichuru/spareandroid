package com.joseph.spare;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.joseph.spare.domain.Item;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.Configuration;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;
import com.squareup.picasso.Picasso;
import com.yayandroid.locationmanager.LocationManager;
import com.yayandroid.locationmanager.listener.LocationListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import io.fabric.sdk.android.Fabric;
import okhttp3.ResponseBody;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NearbyActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, LocationListener {

    private GoogleMap mMap;
    @BindView(R.id.back_btn)
    ImageButton backBtn;
    @BindView(R.id.details_c)
    View detailsC;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.period)
    TextView duration;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.view_btn)
    Button viewButton;
    @BindView(R.id.image)
    ImageView image;


    List<Item> items = new ArrayList<>();
    User user;
    private ACProgressFlower dialog;

    Subscription subscription;
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        user= (User) getIntent().getExtras().get("user");
        this.locationManager = new LocationManager.Builder(getApplicationContext())
                .activity(this).configuration(Configuration.defaultConfiguration("Spare requires to use your location to customize experience.", "Allow Spare to turn on GPS?")).notify(this).build();
        this.locationManager.get();
        findItems();
    }

    private void findItems() {
        if(NetworkUtils.isNetworkAvailable(this)){
            if(subscription!=null&&!subscription.isUnsubscribed()){
                subscription.unsubscribe();
            }
            Map<String,Float> lastLocation=ServiceUtils.findLocation(this);
            subscription=NetworkUtils.getInstance().findNearby(lastLocation.get("lat"),lastLocation.get("lng"),user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(this::showDialog)
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            hideDialog();
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            hideDialog();
                            try {
                                String rsp=responseBody.string();
                                Log.i("ITEMS",rsp);
                                JSONArray jsonArray=new JSONArray(rsp);
                                items = ServiceUtils.parseItems(jsonArray);
                                updateMarkers();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }else {
            Toast.makeText(this, Constants.INTERNET_ERROR_MSG, Toast.LENGTH_SHORT).show();
        }
    }


    private void showDialog() {
        runOnUiThread(() -> {
            if (dialog == null) {
                dialog = new ACProgressFlower.Builder(this)
                        .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                        .themeColor(Color.WHITE)
                        .textColor(Color.WHITE)
                        .bgAlpha(0)
                        .fadeColor(Color.DKGRAY).build();
                dialog.setCancelable(false);
                dialog.show();
            } else {
                if (!dialog.isShowing()) {
                    dialog.show();
                }
            }

        });

    }

    private void hideDialog() {
        runOnUiThread(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });

    }

    @OnClick(R.id.back_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    //move camera to users last known location
    //show dialog
    //get items and populate pins
    //hide dialog

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(1);

        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") == 0 || ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") == 0) {
            this.mMap.setMyLocationEnabled(true);
        }
        Map<String, Float> lastLocation = ServiceUtils.findLocation(this);
        float lat = lastLocation.get("lat");
        float lng = lastLocation.get("lng");
        LatLng position = new LatLng(lat, lng);
      //move camera to whichever place is tops
        //  this.mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(position).zoom(17.0f).tilt(70.0f).build()));

    }

    private void updateMarkers() {
        Log.i("SIZE",""+items.size());
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            LatLng position = new LatLng(item.getLat(), item.getLon());
            MarkerOptions markerOptions = new MarkerOptions().draggable(false).position(position)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.location));
            Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(i);
            Log.i("LATLNG",position.toString());
            mMap.setOnMarkerClickListener(this);
        }

        if(!items.isEmpty()){
            Item tops=items.get(0);
            LatLng position=new LatLng(tops.getLat(),tops.getLon());
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(position).zoom(13.0f).tilt(70.0f).build()));
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //show place bottom bar
        if (marker.getTag() != null) {
            showPlace((Integer) marker.getTag());
        }
        return false;
    }

    Item v=null;
    private void showPlace(int position) {
        Item item = items.get(position);
        v=item;
        name.setText(item.getName());
        switch (item.getDuration()) {
            case "month":
                duration.setText("p.m");
                break;
            case "year":
                duration.setText("p.a");
                break;
        }

        String[] images = item.getImageUrls();
        Picasso.with(this).load(Constants.RESOURCE_URL + images[0])
                .placeholder(R.drawable.grey_placeholder).error(R.drawable.grey_placeholder).into(image);
        if (detailsC.getVisibility() != View.VISIBLE) {
            detailsC.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onProcessTypeChanged(int processType) {

    }

    @Override
    public void onLocationChanged(Location location) {
        ServiceUtils.updateLocation((float) location.getLatitude(),(float) location.getLongitude(),this);
        Log.i("LOC","location changed");
    }

    @Override
    public void onLocationFailed(int type) {

    }

    @Override
    public void onPermissionGranted(boolean alreadyHadPermission) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    @OnClick(R.id.view_btn)
    public void showItem(){
        if(v!=null){
            //add item to intent
            Intent intent = new Intent(this, PlaceDetails.class);
            intent.putExtra("item", v);
            intent.putExtra("user",user);
            startActivity(intent);
        }
    }
}
