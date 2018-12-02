package com.joseph.spare;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.AppDatabase;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import io.fabric.sdk.android.Fabric;
import okhttp3.ResponseBody;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ChangePassword extends AppCompatActivity {


    @BindView(R.id.back_btn)
    ImageButton backBtn;
    @BindView(R.id.done)
    ImageButton doneBtn;
    @BindView(R.id.current_password)
    EditText currentPassword;
    @BindView(R.id.new_password)
    EditText newPassword;
    @BindView(R.id.confirm_password)
    EditText confirmPassword;
    User user;
    String userId;
    Subscription subscription;
    private ACProgressFlower dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        //crash reporting
       // Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        userId= ServiceUtils.getLoggedUserId(this);
        if(userId==null){
            invalidState();
        }
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
    private void invalidState() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Session timed out.Login again", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    private void showSnackBar(String message) {
        Snackbar.make(confirmPassword, message, Snackbar.LENGTH_LONG).show();
    }

    @OnClick(R.id.back_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @OnClick(R.id.done)
    public void changePassword(){
        if(TextUtils.isEmpty(currentPassword.getText().toString())
                ||TextUtils.isEmpty(newPassword.getText().toString())
                ||TextUtils.isEmpty(confirmPassword.getText().toString())){
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!confirmPassword.getText().toString().equals(newPassword.getText().toString())){
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        if(user!=null){
            doChange(currentPassword.getText().toString(),newPassword.getText().toString());
        }
    }

    private void doChange(String currentPassword, String newPassword) {
        if(NetworkUtils.isNetworkAvailable(this)){
            subscription=NetworkUtils.getInstance().changePassword(currentPassword,newPassword,user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(r->hideDialog())
                    .doOnSubscribe(this::showDialog)
                    .subscribe(new SingleSubscriber<ResponseBody>() {
                        @Override
                        public void onSuccess(ResponseBody value) {
                            try {
                                hideDialog();
                                String rsp=value.string();
                                JSONObject jsonObject=new JSONObject(rsp);
                                if(jsonObject.getBoolean("status")){
                                    Toast.makeText(ChangePassword.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                                    onBackPressed();
                                    finish();
                                }else {
                                    Toast.makeText(ChangePassword.this, jsonObject.optString("message","Could no change password"), Toast.LENGTH_SHORT).show();
                                }


                                Log.i("Password",rsp);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                            hideDialog();
                        }
                    });
        }else {
            showSnackBar(Constants.INTERNET_ERROR_MSG);
        }
    }


    private void showDialog() {
        runOnUiThread(()->{
            if(dialog==null){
                dialog = new ACProgressFlower.Builder(this)
                        .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                        .themeColor(Color.WHITE)
                        .bgAlpha(0)
                        .fadeColor(Color.DKGRAY).build();
                dialog.setCancelable(false);
                dialog.show();
            }else {
                if(!dialog.isShowing()){
                    dialog.show();
                }
            }

        });

    }

    private void hideDialog() {
        runOnUiThread(()->{
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });

    }
}
