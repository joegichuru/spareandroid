package com.joseph.spare;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.joseph.spare.adapaters.ItemsAdapter;
import com.joseph.spare.callbacks.ItemCallBack;
import com.joseph.spare.domain.Item;
import com.joseph.spare.domain.User;
import com.joseph.spare.utils.AppDatabase;
import com.joseph.spare.utils.Constants;
import com.joseph.spare.utils.NetworkUtils;
import com.joseph.spare.utils.ServiceUtils;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;
import de.hdodenhof.circleimageview.CircleImageView;
import io.fabric.sdk.android.Fabric;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class Account extends AppCompatActivity implements ItemCallBack {

    @BindView(R.id.edit_btn)
    View editBtn;
    @BindView(R.id.logout)
    TextView logout;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.avatar)
    CircleImageView avatar;
    @BindView(R.id.back_btn)
    ImageButton backBtn;
    @BindView(R.id.email)
    TextView email;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    String appUserId;
    Subscription subscription;


    User user;
    User appUser=null;
    String userId;
    private ACProgressFlower dialog;
    List<Item> items=new ArrayList<>();
    ItemsAdapter itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);
        userId = ServiceUtils.getLoggedUserId(this);
        if (userId == null) {
            invalidState();
        }
        findUser();
        appUserId=getIntent().getExtras().getString("userId");
        if(getIntent().getExtras().containsKey("user")){
            appUser= (User) getIntent().getExtras().get("user");
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        findUser();
    }

    private void findUser() {
        String userId = ServiceUtils.getLoggedUserId(this);

        if (userId ==null) {
            invalidState();
        } else {
            AppDatabase.getInstance(this).userDao().find(userId)
                    .subscribeOn(Schedulers.io())
                    .doOnSuccess(user1 -> {
                        if (user1 != null) {
                            user = user1;
                            runOnUiThread(this::updateUI);
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

    private void updateUI() {
        runOnUiThread(() -> {
            if(appUser!=null&&!appUserId.equalsIgnoreCase(user.getId())){
                //hide logout and edit btns
                if(logout.getVisibility()!=View.GONE){
                    logout.setVisibility(View.GONE);
                }
                if(editBtn.getVisibility()!=View.GONE){
                    editBtn.setVisibility(View.GONE);
                }
                name.setText(appUser.getName());
                email.setText(appUser.getEmail());
                if(appUser.getImageUrl()!=null){
                    Picasso.with(this).load(Constants.RESOURCE_URL+appUser.getImageUrl()).error(R.drawable.user)
                            .placeholder(R.drawable.user).into(avatar);
                }else {
                    Picasso.with(this).load(R.drawable.user).into(avatar);
                }
            }else {
                name.setText(user.getName());
                email.setText(user.getEmail());
                if(user.getImageUrl()!=null){
                    Picasso.with(this).load(Constants.RESOURCE_URL+user.getImageUrl()).error(R.drawable.user)
                            .placeholder(R.drawable.user).into(avatar);
                }else {
                    Picasso.with(this).load(R.drawable.user).into(avatar);
                }
            }


        });

        //find items belonging to the user whose id was passed to this activity
        findItemsByUser();
    }

    private void invalidState() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Session timed out.Login again", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @OnClick(R.id.edit_btn)
    public void editAccount() {
        startActivity(new Intent(this, EditAccount.class));
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

    @OnClick(R.id.logout)
    public void logout() {
        clearDB();

    }

    public void clearDB() {
        Completable.fromAction(() -> {
            AppDatabase.getInstance(this).userDao().delete(user);
            AppDatabase.getInstance(this).clearAllTables();
        })
                .doOnComplete(() -> {
                    //clear login take user to start
                    //call finish
                    hideDialog();
                    ServiceUtils.clearLogin(this);
                    Intent intent = new Intent(this, SplashScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                })
                .doOnSubscribe(disposable -> {
                    showToast("You will be logged out.All the local data will be lost.");
                })
                .doOnError(e -> {
                    showToast("Could not logout.");
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void findItemsByUser(){
        if(NetworkUtils.isNetworkAvailable(this)){
            subscription=NetworkUtils.getInstance().findByUser(appUserId,user.getAccessToken())
                    .subscribeOn(rx.schedulers.Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(this::startRefresh)
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            stopRefresh();

                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                stopRefresh();
                                String rsp=responseBody.string();
                                JSONArray data=new JSONArray(rsp);
                                items=ServiceUtils.parseItems(data);
                                populateViews();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });

        }
    }

    private void populateViews() {
        runOnUiThread(()->{
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            itemsAdapter=new ItemsAdapter(this,items,this);
            recyclerView.setAdapter(itemsAdapter);
        });
    }

    @OnClick(R.id.back_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void startRefresh(){
        runOnUiThread(()->{
            if(!swipeRefreshLayout.isRefreshing()){
                swipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    private void stopRefresh(){
        runOnUiThread(()->{
            if(swipeRefreshLayout.isRefreshing()){
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onItemClicked(int position) {

    }

    @Override
    public void onItemBookmarked(int position) {

    }

    @Override
    public void onItemUnbookmarked(int position) {

    }

    @Override
    public void onItemLiked(int position) {

    }

    @Override
    public void onComment(int position) {

    }

    @Override
    public void onAvatarClicked(int position) {

    }
}
