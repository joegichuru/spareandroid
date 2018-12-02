package com.joseph.spare;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.joseph.spare.adapaters.AddAmenityAdapter;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.Configuration;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.yayandroid.locationmanager.LocationManager;
import com.yayandroid.locationmanager.listener.LocationListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import io.fabric.sdk.android.Fabric;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AddItem extends AppCompatActivity implements AddAmenityAdapter.AmenityCheckListener, OnMapReadyCallback, GoogleMap.OnCameraChangeListener, LocationListener {

    @BindView(R.id.image)
    ImageView image;
    @BindView(R.id.add_photo)
    ImageButton addPhoto;
    @BindView(R.id.back_btn)
    ImageButton backBtn;
    @BindView(R.id.name)
    EditText name;
    @BindView(R.id.type)
    EditText type;
    @BindView(R.id.category)
    EditText category;
    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.phone)
    EditText phone;
    @BindView(R.id.website)
    EditText website;
    @BindView(R.id.price)
    EditText price;
    @BindView(R.id.duration)
    EditText duration;
    @BindView(R.id.bathrooms)
    EditText bathrooms;
    @BindView(R.id.bedrooms)
    EditText bedrooms;
    @BindView(R.id.city)
    EditText city;
    @BindView(R.id.area)
    EditText area;
    @BindView(R.id.description)
    EditText description;
    @BindView(R.id.transparent_image)
    ImageView transparent_image;
    @BindView(R.id.publish_btn)
    TextView publishBtn;
    @BindView(R.id.amenities_view)
    RecyclerView amenitiesView;

    Uri imageUri;
    private ACProgressFlower dialog;
    private Context mContext;
    LatLng latLng;
    int categoryId;
    Set<String> selectedAmenities = new HashSet<>();
    String[] amenities;
    Subscription subscription;

    User user;
    GoogleMap mGoogleMap;
    LocationManager locationManager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        user= (User) getIntent().getExtras().get("user");
        mContext = this;
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        this.locationManager = new LocationManager.Builder(getApplicationContext()).activity(this).configuration(Configuration.defaultConfiguration("Spare requires to use your location to customize experience.", "Allow Spare to turn on GPS?")).notify(this).build();
        this.locationManager.get();
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
        List<String>am = new ArrayList<>();
        am.add("Garage");
        am.add("Outdoor Pool");
        am.add("Garden");
        am.add("Security System");
        am.add("Internet");
        am.add("Telephone");
        am.add("Air Conditioning");
        am.add("Balcony");
        am.add("Fire Place");
        am.add("Parking Space");
        am.add("Cable TV");
        am.add("Water Heating");
        amenities=  am.toArray(new String[am.size()]);
        AddAmenityAdapter addAmenityAdapter=new AddAmenityAdapter(this,amenities,this);
        amenitiesView.setAdapter(addAmenityAdapter);
    }

    @OnClick(R.id.publish_btn)
    public void publishItem() {
        if (imageUri == null) {
            Toast.makeText(this, "Add at least one image.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name.getText().toString()) ||
                TextUtils.isEmpty(type.getText().toString()) ||
                TextUtils.isEmpty(category.getText().toString()) ||
                TextUtils.isEmpty(email.getText().toString()) ||
                TextUtils.isEmpty(price.getText().toString()) ||
                TextUtils.isEmpty(city.getText().toString()) ||
                TextUtils.isEmpty(description.getText().toString())
                ) {
            Toast.makeText(this, "All fields marked with * are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
            Toast.makeText(this, "Email address not valid.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(phone.getText().toString())) {
            if (!Patterns.PHONE.matcher(phone.getText().toString()).matches()) {
                Toast.makeText(this, "Phone number not valid.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        RequestBody namePart = RequestBody.create(MultipartBody.FORM, name.getText().toString());
        RequestBody typePart = RequestBody.create(MultipartBody.FORM, type.getText().toString().toLowerCase().trim());
        RequestBody categoryPart = RequestBody.create(MultipartBody.FORM, String.valueOf(categoryId));
        RequestBody emailPart = RequestBody.create(MultipartBody.FORM, email.getText().toString());
        RequestBody phonePart = RequestBody.create(MultipartBody.FORM, phone.getText().toString());
        RequestBody websitePart = RequestBody.create(MultipartBody.FORM, website.getText().toString());
        RequestBody pricePart = RequestBody.create(MultipartBody.FORM, price.getText().toString());
        RequestBody durationPart = RequestBody.create(MultipartBody.FORM, duration.getText().toString());
        String bath;
        if (TextUtils.isEmpty(bathrooms.getText().toString())) {
            bath = "0";
        } else {
            bath = bathrooms.getText().toString();
        }
        RequestBody bathroomsPart = RequestBody.create(MultipartBody.FORM, bath);
        String bed;
        if (TextUtils.isEmpty(bedrooms.getText().toString())) {
            bed = "0";
        } else {
            bed = bedrooms.getText().toString();
        }
        RequestBody bedroomsPart = RequestBody.create(MultipartBody.FORM, bed);

        RequestBody cityPart = RequestBody.create(MultipartBody.FORM, city.getText().toString());
        String are;
        if (TextUtils.isEmpty(area.getText().toString())) {
            are = "0";
        } else {
            are = area.getText().toString();
        }
        RequestBody areaPart = RequestBody.create(MultipartBody.FORM, are);
        RequestBody descriptionPart = RequestBody.create(MultipartBody.FORM, description.getText().toString());
        RequestBody latPart;
        RequestBody lngPart;
        //that means user dragged map
        if (latLng != null) {
            latPart = RequestBody.create(MultipartBody.FORM, String.valueOf(latLng.latitude));
            lngPart = RequestBody.create(MultipartBody.FORM, String.valueOf(latLng.longitude));
        } else {
            //use location history
            Map<String, Float> location = ServiceUtils.findLocation(this);
            latPart = RequestBody.create(MultipartBody.FORM, String.valueOf(location.get("lat")));
            lngPart = RequestBody.create(MultipartBody.FORM, String.valueOf(location.get("lng")));
        }
        StringBuilder am = new StringBuilder();
        for (String s : selectedAmenities) {
            am.append(s).append(",");
        }
        am.deleteCharAt(am.lastIndexOf(","));
        Log.i("Builder", am.toString());
        RequestBody amenitiesPart = RequestBody.create(MultipartBody.FORM, am.toString());

        //create image part
        MultipartBody.Part imagePart = buildFilePartBody(imageUri);

        //send data to server

        postNew(namePart, pricePart, durationPart, typePart, categoryPart, descriptionPart, cityPart, latPart
                , lngPart, amenitiesPart, emailPart, phonePart, websitePart, bathroomsPart, bedroomsPart, areaPart, imagePart);


    }

    public void postNew(RequestBody name, RequestBody price, RequestBody duration,
                        RequestBody type, RequestBody category, RequestBody description,
                        RequestBody city, RequestBody lat, RequestBody lng, RequestBody amenities,
                        RequestBody email, RequestBody phone, RequestBody website, RequestBody bathrooms,
                        RequestBody bedrooms, RequestBody area, MultipartBody.Part image
    ) {

        if(NetworkUtils.isNetworkAvailable(this)){
            if(subscription!=null&&!subscription.isUnsubscribed()){
                subscription.unsubscribe();
            }
            subscription=NetworkUtils.getInstance().postNew(name,price,duration,type,category,
                    description,city,lat,lng,amenities,email,phone,website,bathrooms,bedrooms,area,
                    image,user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(this::showDialog)
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            hideDialog();
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            hideDialog();
                            Toast.makeText(AddItem.this,"Could not post item", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                hideDialog();
                                String rsp=responseBody.string();
                                Log.i("Item",rsp);
                                if(rsp.contains("id")){
                                    Toast.makeText(AddItem.this, "Item posted", Toast.LENGTH_SHORT).show();
                                    onBackPressed();
                                }else {
                                    Toast.makeText(AddItem.this,"Could not post item", Toast.LENGTH_LONG).show();
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }else {
            Toast.makeText(this, Constants.INTERNET_ERROR_MSG, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.add_photo)
    public void addPhoto() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .setMinCropResultSize(250, 250)
                .start(this);
    }

    @OnClick(R.id.type)
    public void onTypeClicked() {
        //show context menu with rent and sell
        String[] types = {"Rent", "Sell"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select type");
        builder.setItems(types, (dialog, which) -> {
            type.setText(types[which]);
        });
        builder.show();
    }

    @OnClick(R.id.category)
    public void onCategoryClicked() {
        //show context menu with categories
        String[] categories = {"Office", "Apartment", "Self Contained", "Town House", "Studio Apartment", "Bungalow", "Beach House"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Category");
        builder.setItems(categories, (dialog, which) -> {
            category.setText(categories[which]);
            categoryId = which + 1;
        });
        builder.show();

    }

    @OnClick(R.id.duration)
    public void onDurationClicked() {
        //show context menu with month and year
        String[] durations = {"Month", "Year"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Rent Frequency");
        builder.setItems(durations, (dialog, which) -> {
            duration.setText(durations[which]);

        });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Log.i("IMAGE", resultUri.toString());
                //  Toast.makeText(this, resultUri.toString(), Toast.LENGTH_SHORT).show();
                imageUri = resultUri;
                Picasso.with(this).load(resultUri).into(image);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                error.printStackTrace();
                imageUri = null;
                Toast.makeText(this, "Could not select photo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public MultipartBody.Part buildFilePartBody(Uri uri) {
        String filePath = getRealPathFromUri(uri);
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            if (file.exists()) {
                // creates RequestBody instance from file
                RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
                // MultipartBody.Part is used to send also the actual filename
                return MultipartBody.Part.createFormData("images", file.getName(), requestFile);
            }
        }
        return null;
    }

    /***file utils
     *
     */

    public String getRealPathFromUri(final Uri uri) {
        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(mContext, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(mContext, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(mContext, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(mContext, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection,
                                 String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    private void showDialog() {
        runOnUiThread(() -> {
            if (dialog == null) {
                dialog = new ACProgressFlower.Builder(this)
                        .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                        .themeColor(Color.WHITE)
                        .textColor(Color.WHITE)
                        .text("Posting...")
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

    @Override
    public void onAmenitySelected(int index) {
        selectedAmenities.add(amenities[index]);
       // Toast.makeText(mContext, selectedAmenities.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAmenityUnselected(int index) {
        selectedAmenities.remove(amenities[index]);
       // Toast.makeText(mContext, selectedAmenities.toString(), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap=googleMap;
        mGoogleMap.setMapType(1);
        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") == 0 || ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") == 0) {
            this.mGoogleMap.setMyLocationEnabled(true);
        }
        Map<String,Float> lastLocation=ServiceUtils.findLocation(this);
        float lat=lastLocation.get("lat");
        float lng=lastLocation.get("lng");
        LatLng position=new LatLng(lat,lng);
        this.mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(position).zoom(10.0f).tilt(70.0f).build()));
        this.mGoogleMap.setOnCameraChangeListener(this);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        latLng=cameraPosition.target;
    }

    @Override
    public void onProcessTypeChanged(int processType) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //update last location
        ServiceUtils.updateLocation((float) location.getLatitude(),(float) location.getLongitude(),this);
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
}
