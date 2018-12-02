package com.joseph.spare;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;
import rx.Completable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class BookmarksActivity extends AppCompatActivity implements ItemCallBack {

    @BindView(R.id.empty_view)
    View emptyView;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.back_btn)
    ImageButton backBtn;

    List<Item> items=new ArrayList<>();
    ItemsAdapter itemsAdapter;
    User user;
    private Subscription likeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        findUser();
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
                            findItems();
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void findItems() {
        AppDatabase.getInstance(this).itemsDao().findAll().observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe(new SingleObserver<List<Item>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(List<Item> i) {
                        items=i;
                        //populate recycler view
                        populateRecyclerView();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if(emptyView.getVisibility()!=View.VISIBLE){
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void populateRecyclerView() {
        runOnUiThread(()->{
            if(items.isEmpty()){
                if(emptyView.getVisibility()!=View.VISIBLE){
                    emptyView.setVisibility(View.VISIBLE);
                }
                if(recyclerView.getVisibility()!=View.GONE){
                    recyclerView.setVisibility(View.GONE);
                }
            }else {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                itemsAdapter=new ItemsAdapter(this,items,this);
                recyclerView.setAdapter(itemsAdapter);
                if(recyclerView.getVisibility()!=View.VISIBLE){
                    recyclerView.setVisibility(View.VISIBLE);
                }
                if(emptyView.getVisibility()!=View.GONE){
                    emptyView.setVisibility(View.GONE);
                }
            }

        });
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
        unBookmarkItem(item,position);
    }

    @Override
    public void onItemUnbookmarked(int position) {
        Item item = items.get(position);
        bookMarkItem(item);
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
                    .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            if(item.isLiked()){
                                //unlike
                                item.setLiked(false);
                                items.get(position).setLiked(false);
                                items.get(position).setLikes(item.getLikes()-1);
                                itemsAdapter.notifyItemChanged(position);
                            }else {
                                //like
                                item.setLiked(true);
                                items.get(position).setLiked(true);
                                items.get(position).setLikes(item.getLikes()+1);
                                itemsAdapter.notifyItemChanged(position);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(BookmarksActivity.this, Constants.CONNECTION_ERROR, Toast.LENGTH_SHORT).show();
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
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> runOnUiThread(() -> Toast.makeText(this, "Bookmarked.", Toast.LENGTH_SHORT).show()))
                .subscribe();

    }
    public void unBookmarkItem(Item item,int position) {
        //remove item from database
        //update button
        Completable.fromAction(() -> AppDatabase.getInstance(this).itemsDao().delete(item)).subscribeOn(Schedulers.io())
                .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnCompleted(() -> runOnUiThread(() ->{
                    items.remove(position);
                    itemsAdapter.notifyItemRemoved(position);
                    if(items.isEmpty()){
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(this, "Un-bookmarked.", Toast.LENGTH_SHORT).show();
                }))
                .subscribe();
    }

    @OnClick(R.id.back_btn)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
