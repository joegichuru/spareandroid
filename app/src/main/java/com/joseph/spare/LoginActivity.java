package com.joseph.spare;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.AppDatabase;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;

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
import io.fabric.sdk.android.Fabric;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.HttpException;
import rx.Completable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity  {
    @BindView(R.id.register)
    View register;
    @BindView(R.id.login_btn)
    View loginBtn;
    @BindView(R.id.email)
    EditText email;
    @BindView(R.id.password)
    EditText password;

    @BindView(R.id.forgot_password)
    TextView forgotPassword;




    ACProgressFlower dialog;
    String userEmail;
    String userPassword;
    Subscription loginSubscription;
    Subscription saveSubsription;

    User user;

    private static final String TAG = "LoginActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //crash reporting
        //Fabric.with(this, new Crashlytics());
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);

    }

    @OnClick(R.id.register)
    public void registerUser() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    @OnClick(R.id.login_btn)
    public void startLogin() {

        if (TextUtils.isEmpty(email.getText().toString().trim())) {
            showSnackBar("Email required.");
            return;
        }
        if (TextUtils.isEmpty(password.getText().toString().trim())) {
            showSnackBar("Password required.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()) {
            showSnackBar("Email not valid.");
            return;
        }
        emailLogin();
    }

    private void emailLogin() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            userEmail = email.getText().toString().toLowerCase().trim();
            userPassword = password.getText().toString().trim();
            if (loginSubscription != null && !loginSubscription.isUnsubscribed()) {
                loginSubscription.unsubscribe();
            }
            showDialog();
            loginSubscription = NetworkUtils.getInstance().emailLogin(userEmail, userPassword)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(this::showDialog)
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            hideDialog();
                            if (user!=null) {

                                //saveUser();
                                //update preferences and take user home
                                // ServiceUtils.saveLoggedUser(user.getWebId(),"google",LoginActivity.this);
                               // ServiceUtils.saveToken(token,deviceToken, method, LoginActivity.this);
                                ServiceUtils.saveLoggedUser(user.getId(),LoginActivity.this);
                                saveUser();
                               // saveUser();
                               // startMainActivity();
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
                                                errorBuilder.append("Could not login.Try again later");
                                            }
                                            break;
                                        case 500:
                                            rsp = exception.response().errorBody().string();
                                            if (rsp != null) {
                                                JSONObject rs = new JSONObject(rsp);
                                                //  errorBuilder.append(rs.getString("message"));
                                                //iterate through errors to get messages
                                                errorBuilder.append(rs.getString("message"));
                                            } else {
                                                errorBuilder.append("Could not login.Try again later");
                                            }
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
                                                errorBuilder.append("Could not login.Try again later");
                                            }
                                            break;
                                        default:errorBuilder.append("Could not login.Try again later");

                                    }

                                } catch (IOException e1) {
                                    errorBuilder.append("Could not login.Try again later");
                                    e1.printStackTrace();
                                } catch (JSONException e1) {
                                    errorBuilder.append("Could not login.Try again later");
                                    e1.printStackTrace();
                                }
                                showSnackBar(errorBuilder.toString());
                            } else if (e instanceof SocketTimeoutException) {
                                showSnackBar(Constants.CONNECTION_ERROR);
                            } else {
                                showSnackBar("Could not login.Try again later");
                            }
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {

                            try {
                                String rsp = responseBody.string();
                                Log.i(TAG, rsp);
                                JSONObject jsonObject = new JSONObject(rsp);

                                if (jsonObject.has("id")) {
//                                    JSONObject tokenObject=jsonObject.getJSONObject("tokens");
//                                    token = tokenObject.getString("access_token");
//                                    method ="email";
                                    //parse user
                                    if(!jsonObject.getBoolean("active")){
                                        showSnackBar("Your account has been suspended.Contact site administrator.");
                                    }else {
                                        user=ServiceUtils.parseUser(jsonObject,null);
                                    }


                                }else {
                                    if(jsonObject.getString("status").equalsIgnoreCase("error")){
                                        showSnackBar(jsonObject.getString("message"));
                                    }
                                    if(jsonObject.getString("status").equalsIgnoreCase("failed")){
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

        } else {
            showSnackBar(Constants.INTERNET_ERROR_MSG);
        }
    }


    private void startMainActivity() {
        startActivity(new Intent(this, Dashboard.class));
        finish();
    }

    private void saveUser() {
        Completable.fromAction(()-> {
            ServiceUtils.saveLoggedUser(user.getId(),this);
            AppDatabase
                    .getInstance(getApplicationContext()).userDao().insert(user);
        }).subscribeOn(Schedulers.io())
                .doOnCompleted(this::startMainActivity)
                .doOnError(Throwable::printStackTrace)
                .subscribe();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


    }





    @OnClick(R.id.close_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showDialog() {
        runOnUiThread(()->{
            if(dialog==null){
                dialog = new ACProgressFlower.Builder(this)
                        .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                        .themeColor(Color.WHITE)
                        .textColor(Color.WHITE)
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

    private void showSnackBar(String message) {
        Snackbar.make(loginBtn, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideDialog();
        if (loginSubscription != null && !loginSubscription.isUnsubscribed()) {
            loginSubscription.unsubscribe();
        }
    }

    @OnClick(R.id.forgot_password)
    public void onForgotPassword(){
        startActivity(new Intent(this,PasswordReset.class));
    }


}
