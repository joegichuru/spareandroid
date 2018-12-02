package com.joseph.spare;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.AppDatabase;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import de.hdodenhof.circleimageview.CircleImageView;
import io.fabric.sdk.android.Fabric;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rx.Completable;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EditAccount extends AppCompatActivity {

    @BindView(R.id.back_btn)
    ImageButton backBtn;
    @BindView(R.id.done)
    ImageButton doneBtn;
    @BindView(R.id.avatar)
    CircleImageView avatar;
    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.change_password)
    View changePassword;
    @BindView(R.id.avatar_c)
    View avatarContainer;
    boolean avatarChanged = false;

    Uri imageUri = null;
    User user;
    private Context mContext;

    Subscription subscription;
    private ACProgressFlower dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);
        //crash reporting
       // Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        mContext = this;
        findUser();

    }

    private void findUser() {
        String userId = ServiceUtils.getLoggedUserId(this);

        if (userId == null) {
            invalidState();
        } else {
            AppDatabase.getInstance(this).userDao().find(userId)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .doOnSuccess(user1 -> {
                        if (user1 != null) {
                            user = user1;
                            //populate views
                            populateViews();
                        } else {
                            //call finish
                            //invalid state
                            invalidState();
                        }
                    })
                    .doOnError(e -> invalidState())
                    .subscribe();

        }
    }

    private void populateViews() {
        runOnUiThread(() -> {
            if (!(user.getImageUrl()==null)) {
                Picasso.with(this).load(Constants.RESOURCE_URL+user.getImageUrl()).placeholder(R.drawable.user).error(R.drawable.user).into(avatar);
            } else {
                Picasso.with(this).load(R.drawable.user).into(avatar);
            }

            email.setText(user.getEmail());
            username.setText(user.getName());
        });
    }

    private void invalidState() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Session timed out.Login again", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @OnClick(R.id.done)
    public void editAccount() {
        if (TextUtils.isEmpty(username.getText().toString().trim())) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email.getText().toString().trim()) || !Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
            Toast.makeText(this, "Email address not valid.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody namePart = RequestBody.create(MultipartBody.FORM, username.getText().toString());
        RequestBody emailPart = RequestBody.create(MultipartBody.FORM, email.getText().toString());

        if (avatarChanged) {
            //call endpoint with image
            MultipartBody.Part part = buildFilePartBody(imageUri);
            if (part == null) {
                //call enpoint without image
                Toast.makeText(mContext, "The selected image could not be uploaded.", Toast.LENGTH_SHORT).show();
                //call end point without image
                updateAll(namePart, emailPart);
            } else {
                //call endpoint with image
                updateAll(namePart, emailPart, part);
            }
        } else {
            //call endpoint without image
            updateAll(namePart, emailPart);
        }
    }

    public void updateAll(RequestBody name, RequestBody email, MultipartBody.Part image) {
        if (NetworkUtils.isNetworkAvailable(this)) {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            subscription = NetworkUtils.getInstance().updateAccount(name, email, image, user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(this::showDialog)
                    .subscribe(new SingleSubscriber<ResponseBody>() {
                        @Override
                        public void onSuccess(ResponseBody value) {
                            try {
                                String rsp = value.string();
                                Log.i("RSP", rsp);
                                JSONObject jsonObject = new JSONObject(rsp);
                                if (jsonObject.has("id")) {
                                    user = ServiceUtils.parseUser(jsonObject,user);
                                    hideDialog();
                                   // showToast(jsonObject.getString("message"));
                                    saveUser();
                                } else {
                                    hideDialog();
                                    showSnackBar("Could not update account");
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                                hideDialog();
                                showSnackBar("Could not update account");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                hideDialog();
                                showSnackBar("Could not update account");
                            }

                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                            Toast.makeText(EditAccount.this, "Could not update account.", Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }
                    });
        } else {
            showSnackBar(Constants.INTERNET_ERROR_MSG);
        }
    }

    public void updateAll(RequestBody name, RequestBody email) {
        if (NetworkUtils.isNetworkAvailable(this)) {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            subscription = NetworkUtils.getInstance().updateAccount(name, email, user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(this::showDialog)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleSubscriber<ResponseBody>() {
                        @Override
                        public void onSuccess(ResponseBody value) {
                            try {
                                String rsp = value.string();
                                Log.i("RSP", rsp);
                                JSONObject jsonObject = new JSONObject(rsp);
                                if (jsonObject.has("id")) {
                                    user = ServiceUtils.parseUser(jsonObject,user);
                                    hideDialog();
                                    //showToast(jsonObject.getString("message"));
                                    saveUser();
                                } else {
                                    hideDialog();
                                    showSnackBar("Could not update account");
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                                hideDialog();
                                showSnackBar("Could not update account");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                hideDialog();
                                showSnackBar("Could not update account");
                            }

                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                            Toast.makeText(EditAccount.this, "Could not update account.", Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }
                    });
        } else {
            showSnackBar(Constants.INTERNET_ERROR_MSG);
        }
    }

    private void saveUser() {
        Completable.fromAction(() -> AppDatabase.getInstance(this).userDao().update(user)).doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> runOnUiThread(() -> {
                    onBackPressed();
                    finish();
                }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }



    public MultipartBody.Part buildFilePartBody(Uri uri) {
        String filePath = getRealPathFromUri(uri);
        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            if (file.exists()) {
                // creates RequestBody instance from file
                RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
                // MultipartBody.Part is used to send also the actual filename
                return MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);
            }
        }
        return null;
    }

    @OnClick(R.id.change_password)
    public void changePassword() {
        startActivity(new Intent(this, ChangePassword.class));
    }

    /**
     * launch image-crop picker to change avatar
     */
    @OnClick(R.id.avatar_c)
    public void changeAvatar() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .setMinCropResultSize(250, 250)
                .start(this);
    }

    private void showSnackBar(String message) {
        runOnUiThread(() -> {
            Snackbar.make(backBtn, message, Snackbar.LENGTH_LONG).show();
        });
    }

    private void showToast(String message){
        runOnUiThread(()-> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Log.i("IMAGE", resultUri.toString());
                // Toast.makeText(this, resultUri.toString(), Toast.LENGTH_SHORT).show();
                imageUri = resultUri;
                Picasso.with(this).load(resultUri).into(avatar);
                avatarChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                error.printStackTrace();
                Toast.makeText(this, "Could not select photo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDialog() {
        runOnUiThread(() -> {
            if (dialog == null) {
                dialog = new ACProgressFlower.Builder(this)
                        .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                        .themeColor(Color.WHITE)
                        .textColor(Color.WHITE)
                        .text("Updating...")
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
}
