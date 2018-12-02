package com.joseph.spare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
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
import de.hdodenhof.circleimageview.CircleImageView;
import io.fabric.sdk.android.Fabric;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import rx.Completable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Dashboard extends AppCompatActivity implements BottomNavigationBar.OnTabSelectedListener, ItemCallBack {
    @BindView(R.id.bottom_navigation)
    BottomNavigationBar bottomNavigationBar;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.avatar)
    CircleImageView avatar;
    @BindView(R.id.notifications)
    ImageButton notifications;
    @BindView(R.id.btn_bookmark)
    ImageButton bookmarks;
    Subscription subscription;
    Subscription likeSubscription;
    User user;

    List<Item> items = new ArrayList<>();
    ItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        setUpBottomNav();
        bottomNavigationBar.setTabSelectedListener(this);
        findUser();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (user != null) {
                findItemsFromNetwork();
            }
        });

    }

    private void setUpBottomNav() {
        bottomNavigationBar.addItem(new BottomNavigationItem(R.drawable.ic_home, "Home"));
        bottomNavigationBar.addItem(new BottomNavigationItem(R.drawable.ic_search_black, "Search"));
        bottomNavigationBar.addItem(new BottomNavigationItem(R.drawable.ic_add, "Add"));
        bottomNavigationBar.addItem(new BottomNavigationItem(R.drawable.ic_user, "Account"));
        bottomNavigationBar.addItem(new BottomNavigationItem(R.drawable.ic_navigation, "Nearby"));
        bottomNavigationBar.initialise();

    }

    @Override
    public void onTabSelected(int position) {
        switch (position) {
            case 0:
                //do nothing
            case 1:
                //go to search
               // Toast.makeText(this, "Search is disabled.", Toast.LENGTH_SHORT).show();
                Intent intent2 = new Intent(this, SearchActivity.class);
                intent2.putExtra("user",user);
                startActivity(intent2);
                break;
            case 2:
                //go to add
                Intent intent = new Intent(this, AddItem.class);
                intent.putExtra("user", user);
                startActivity(intent);
                break;
            case 3:
                //go to account
                Intent intent3 = new Intent(this, Account.class);
                intent3.putExtra("userId",user.getId());
                startActivity(intent3);
                break;
            case 4:
                //go to nearby
                Intent intent1 = new Intent(this, NearbyActivity.class);
                intent1.putExtra("user", user);
                startActivity(intent1);
                break;
        }
        bottomNavigationBar.selectTab(0, false);
        //startActivity(new Intent(this,PlaceDetails.class));
    }


    @Override
    public void onTabUnselected(int position) {

    }

    @Override
    public void onTabReselected(int position) {
        if (position == 0) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    public void findUser() {
        String loggedUser = ServiceUtils.getLoggedUserId(this);
        if (loggedUser != null) {
            AppDatabase.getInstance(this).userDao().find(loggedUser)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<User>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(User u) {
                            user = u;
                            findItemsFromNetwork();
                            sendFcmToken();

                            runOnUiThread(()->{
                                if(user.getImageUrl()!=null){
                                    Picasso.with(getBaseContext()).load(Constants.RESOURCE_URL+user.getImageUrl())
                                            .error(R.drawable.user).placeholder(R.drawable.user).into(avatar);
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void findItemsFromNetwork() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            subscription = NetworkUtils.getInstance().findItems(user.getAccessToken())
                    .subscribeOn(Schedulers.io())
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
                            Toast.makeText(Dashboard.this, Constants.CONNECTION_ERROR, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            stopRefresh();
                            try {
                                String rsp = responseBody.string();
                                Log.i("ITEMS", rsp);
                               // JSONObject jsonObject = new JSONObject(rsp);
                              //  JSONArray content = jsonObject.getJSONArray("content");
                                JSONArray jsonArray=new JSONArray(rsp);
                                items = ServiceUtils.parseItems(jsonArray);
                                updateUI();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                //account not active log out
                                Toast.makeText(Dashboard.this, "Account not active.", Toast.LENGTH_SHORT).show();
                                logout();
                            }
                        }
                    });
        } else {
            showToast(Constants.INTERNET_ERROR_MSG);
        }
    }

    private void logout() {
        Completable.fromAction(()->{
            AppDatabase.getInstance(this).clearAllTables();
            ServiceUtils.clearLogin(this);

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(()->{
                    Intent intent = new Intent(this, SplashScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                   finish();
                })
                .subscribe();
    }

    private void startRefresh() {
        runOnUiThread(() -> {
            if (!swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    private void stopRefresh() {
        runOnUiThread(() -> {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        runOnUiThread(() -> {
            adapter = new ItemsAdapter(this, items, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        });

    }

    @OnClick(R.id.avatar)
    public void onAvatarClicked() {

        Intent intent = new Intent(this, Account.class);
        intent.putExtra("userId",user.getId());
        startActivity(intent);
    }

    @OnClick(R.id.btn_bookmark)
    public void onBookmarkClicked() {
        startActivity(new Intent(this, BookmarksActivity.class));
    }

    @OnClick(R.id.notifications)
    public void onNotificationClicked() {
        startActivity(new Intent(this, NotificationsActivity.class));
    }

    @Override
    public void onItemClicked(int position) {
        Item item = items.get(position);
        //add item to intent
        Intent intent = new Intent(this, PlaceDetails.class);
        intent.putExtra("item", item);
        intent.putExtra("user",user);
        startActivity(intent);
    }

    @Override
    public void onItemBookmarked(int position) {
        Item item = items.get(position);
        bookMarkItem(item);
    }

    @Override
    public void onItemUnbookmarked(int position) {
        Item item = items.get(position);
        unBookmarkItem(item);
    }

    @Override
    public void onItemLiked(int position) {
        //start like service
        //update ui if successfull
        Item item=items.get(position);
        like(item,position);
    }

    private void like(Item item,int position) {
        if(NetworkUtils.isNetworkAvailable(this)){
            likeSubscription=NetworkUtils.getInstance().like(item.getId(),user.getAccessToken())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            if(item.isLiked()){
                                //unlike
                                item.setLiked(false);
                                items.get(position).setLiked(false);
                                items.get(position).setLikes(item.getLikes()-1);
                                adapter.notifyItemChanged(position);
                            }else {
                                //like
                                item.setLiked(true);
                                items.get(position).setLiked(true);
                                items.get(position).setLikes(item.getLikes()+1);
                                adapter.notifyItemChanged(position);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(Dashboard.this, Constants.CONNECTION_ERROR, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String rsp=responseBody.string();
                                Log.i("LIKE",rsp);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    @Override
    public void onComment(int position) {

    }

    @Override
    public void onAvatarClicked(int position) {
        Item item = items.get(position);
        User appUser = item.getUser();
        Intent intent = new Intent(this, Account.class);
        intent.putExtra("userId", appUser.getId());
        intent.putExtra("user", appUser);
        startActivity(intent);
    }

    private void bookMarkItem(Item item) {
        //add item to database
        //update button
        Completable.fromAction(() -> AppDatabase.getInstance(this).itemsDao().insert(item)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> runOnUiThread(() -> Toast.makeText(this, "Bookmarked.", Toast.LENGTH_SHORT).show()))
                .subscribe();

    }

    public void unBookmarkItem(Item item) {
        //remove item from database
        //update button
        Completable.fromAction(() -> AppDatabase.getInstance(this).itemsDao().delete(item)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> runOnUiThread(() -> Toast.makeText(this, "Un-bookmarked.", Toast.LENGTH_SHORT).show()))
                .subscribe();
    }
    //what if we have two users with same token. whom will receive notification?
    private void sendFcmToken(){
        String token=ServiceUtils.getFcmToken(this);
        if(token==null)return;
        Log.i("FCM",token);
        if(NetworkUtils.isNetworkAvailable(this)){
            NetworkUtils.getInstance().sendFcmToken(token,user.getAccessToken())
                    .subscribeOn(Schedulers.io()).subscribe(new Observer<ResponseBody>() {
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
                        String rsp=responseBody.string();
                        Log.i("TOKEN",rsp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        findUserAgain();

    }
    private void findUserAgain() {
        String userId = ServiceUtils.getLoggedUserId(this);

        if (userId==null) {
            Toast.makeText(this, "Session timed out", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            AppDatabase.getInstance(this).userDao().find(userId)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .doOnSuccess(user1 -> {
                        if (user1 != null) {
                            user = user1;
                            runOnUiThread(() -> {
                                // setUpViewPager();
                                //update side drawer
                                if(user.getImageUrl()!=null){
                                    Picasso.with(this).load(Constants.RESOURCE_URL+user.getImageUrl())
                                            .error(R.drawable.user).placeholder(R.drawable.user).into(avatar);
                                }

                            });
                        } else {
                            //call finish
                            //invalid state
                          //  invalidState();
                        }
                    })
                    .doOnError(Throwable::printStackTrace)//invalidState())
                    .subscribe();

        }
    }
}
