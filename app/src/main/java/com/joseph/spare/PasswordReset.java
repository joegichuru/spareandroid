package com.joseph.spare;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import okhttp3.ResponseBody;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PasswordReset extends AppCompatActivity {
    @BindView(R.id.change_password_btn)
    View changePasswordBtn;
    @BindView(R.id.email)
    EditText emailAddress;


    Subscription subscription;

    @BindView(R.id.close_btn)
    ImageButton closeButton;
    private ACProgressFlower dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.close_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @OnClick(R.id.change_password_btn)
    public void doChange() {
        if(!Patterns.EMAIL_ADDRESS.matcher(emailAddress.getText().toString()).matches()){
            Toast.makeText(this, "Email address not valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        netWorkChange();

    }
    private void netWorkChange() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            subscription = NetworkUtils.getInstance().passwordResetRequest(emailAddress.getText().toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(this::showDialog)
                    .doOnSuccess(r -> hideDialog())
                    .subscribe(new SingleSubscriber<ResponseBody>() {
                        @Override
                        public void onSuccess(ResponseBody value) {
                            try {
                                String rsp = value.string();
                                JSONObject jsonObject = new JSONObject(rsp);
                                Log.i("PAS",rsp);
                                if (jsonObject.getBoolean("status")) {
                                    //handle success
                                    hideDialog();
                                    showToast(jsonObject.getString("message"));
                                    onBackPressed();
                                } else {
                                    //handle failure;
                                    hideDialog();
                                    showSnackBar("Action could not be completed.");
                                }
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
                            showSnackBar("Action could not be completed.");
                        }
                    });
        } else {
            Snackbar.make(closeButton, Constants.INTERNET_ERROR_MSG, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showToast(String message) {
        runOnUiThread(()->{
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void showSnackBar(String message) {
        runOnUiThread(() -> Snackbar.make(closeButton, message, Snackbar.LENGTH_LONG).show());
    }

    private void showDialog() {
        runOnUiThread(() -> {
            if (dialog == null) {
                dialog = new ACProgressFlower.Builder(this)
                        .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                        .themeColor(Color.WHITE)
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
}
