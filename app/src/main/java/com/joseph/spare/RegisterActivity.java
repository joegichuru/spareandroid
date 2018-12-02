package com.joseph.spare;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RegisterActivity extends AppCompatActivity {

    @BindView(R.id.close_btn)
    ImageButton closeBtn;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.register_btn)
    View registerBtn;
    private ACProgressFlower dialog;
    Subscription registerSubscription;
    boolean successful=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //crash reporting
       // Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
    }
    @OnClick(R.id.close_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    @OnClick(R.id.sign_in)
    public void backToLogin(){
        onBackPressed();
        finish();
    }
    @OnClick(R.id.register_btn)
    public void registerUser(){
        if(TextUtils.isEmpty(username.getText().toString().trim())){
            showSnackBar("Username required");
            return;
        }
        if(TextUtils.isEmpty(email.getText().toString().trim())){
            showSnackBar("Email required");
            return;
        }
        if(TextUtils.isEmpty(password.getText().toString().trim())){
            showSnackBar("Password required");
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()){
            showSnackBar("Invalid email address");
            return;
        }
        if(password.getText().toString().length()<6){
            showSnackBar("Password too short. Required 6 or more characters.");
            return;
        }
        register(username.getText().toString().toLowerCase().trim()
                ,email.getText().toString().trim().toLowerCase(),password.getText().toString());

    }
    private void register(String name,String email,String password){
        if(NetworkUtils.isNetworkAvailable(this)){
            if(registerSubscription!=null&&!registerSubscription.isUnsubscribed()){
                registerSubscription.unsubscribe();
            }
            showDialog();
            registerSubscription=NetworkUtils.getInstance().register(name,email,password)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            hideDialog();
                            if(successful){
                                onBackPressed();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            hideDialog();
                            if (e instanceof HttpException) {
                                StringBuilder errorBuilder = new StringBuilder();
                                String rsp = null;
                                try {
                                    HttpException exception = (HttpException) e;
                                    switch (exception.code()){
                                        case 403:
                                            rsp = exception.response().errorBody().string();
                                            if (rsp != null) {
                                                JSONObject rs = new JSONObject(rsp);
                                              //  errorBuilder.append(rs.getString("message"));
                                                //iterate through errors to get messages
                                                Iterator<String> keys = rs.getJSONObject("errors").keys();
                                                while (keys.hasNext()){
                                                    errorBuilder.append(rs.getJSONObject("errors").keys().next()).append("\n");
                                                }
                                            } else {
                                                errorBuilder.append("Could not register.Try again later");
                                            }
                                            break;
                                        case 500:
                                            errorBuilder.append("Could not register due to server error.");
                                            break;
                                        case 422:
                                            rsp = exception.response().errorBody().string();
                                            if (rsp != null) {
                                                JSONObject rs = new JSONObject(rsp);
                                                //  errorBuilder.append(rs.getString("message"));
                                                //iterate through errors to get messages
                                                Iterator<String> keys = rs.getJSONObject("errors").keys();
                                                while (keys.hasNext()){
                                                    String next = keys.next();
                                                    JSONArray errors = rs.getJSONObject("errors").getJSONArray(next);
                                                    if(errors.length()>0){
                                                        errorBuilder.append(errors.getString(0)).append("");
                                                    }
                                                }
                                            } else {
                                                errorBuilder.append("Could not register.Try again later");
                                            }
                                            break;
                                            default:errorBuilder.append("Could not register.Try again later");

                                    }

                                } catch (IOException e1) {
                                    errorBuilder.append("Could not register.Try again later");
                                    e1.printStackTrace();
                                } catch (JSONException e1) {
                                    errorBuilder.append("Could not register.Try again later");
                                    e1.printStackTrace();
                                }
                                showSnackBar(errorBuilder.toString());
                            } else if (e instanceof SocketTimeoutException) {
                                showSnackBar(Constants.CONNECTION_ERROR);
                            } else {
                                showSnackBar("Could not register.Try again later");
                            }
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String rsp=responseBody.string();
                                JSONObject jsonObject=new JSONObject(rsp);
                                if(jsonObject.has("status")&&jsonObject.getString("status").equalsIgnoreCase("success")){
                                    successful=true;
                                    Toast.makeText(RegisterActivity.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                                }else {
                                    if(jsonObject.has("message")){
                                        showSnackBar(jsonObject.getString("message"));
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
        }else {
            showSnackBar(Constants.INTERNET_ERROR_MSG);
        }
    }

    private void showDialog() {
        dialog = new ACProgressFlower.Builder(this)
                .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                .themeColor(Color.WHITE)
                .textColor(Color.WHITE)
                .text("Please Wait...")
                .bgAlpha(0)
                .fadeColor(Color.DKGRAY).build();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void showSnackBar(String message) {
        Snackbar.make(registerBtn, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(registerSubscription!=null&&!registerSubscription.isUnsubscribed()){
            registerSubscription.unsubscribe();
        }
    }
}
