package com.joseph.spare;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
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
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dmax.dialog.SpotsDialog;
import io.fabric.sdk.android.Fabric;
import okhttp3.ResponseBody;
import rx.Completable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, MaterialSearchBar.OnSearchActionListener, SuggestionsAdapter.OnItemViewClickListener, ItemCallBack {

    Subscription searchSubscription;
    User user;
    List<Item> items = new ArrayList<>();
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.searchBar)
    MaterialSearchBar materialSearchBar;
    @BindView(R.id.empty_view)
    View emptyView;
    ItemsAdapter searchItemAdapter;
    Item item;
    Context mContext;
    Subscription followingSubscription;
    AlertDialog sponserDialog;
    SpotsDialog loadingDialog;
    Subscription sponserSubscription;
    String qq = "";
    boolean success = false;
    List lastSearches = new ArrayList<>();
    private Subscription likeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        Fabric.with(this, new Crashlytics());
        user= (User) getIntent().getExtras().get("user");
        mContext = this;
        searchItemAdapter = new ItemsAdapter(this, items,this);
        recyclerView.setAdapter(searchItemAdapter);
        materialSearchBar.setOnSearchActionListener(this);
        materialSearchBar.setSuggstionsClickListener(this);
        lastSearches = materialSearchBar.getLastSuggestions();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        //searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!query.isEmpty()) {
            qq = query;
            startSearch(query);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
//        if(newText.length()>5){
//            startSearch(newText);
//        }
        return false;
    }

    public void startSearch(String query) {
        if (NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(mContext, "Searching " + query + "...", Toast.LENGTH_SHORT).show();
            if (searchSubscription != null && !searchSubscription.isUnsubscribed()) {
                searchSubscription.unsubscribe();
            }
            searchSubscription = NetworkUtils.getInstance().search(query, user != null ? user.getAccessToken() : "")
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onCompleted() {
                            if (items.isEmpty()) {
                                if(emptyView.getVisibility()!=View.VISIBLE){
                                    emptyView.setVisibility(View.VISIBLE);
                                }
                                Toast.makeText(mContext, "\'" + qq + "\'" + " not found.", Toast.LENGTH_SHORT).show();
                            }else {
                                if(emptyView.getVisibility()!=View.GONE){
                                    emptyView.setVisibility(View.GONE);
                                }
                            }
                            searchItemAdapter = new ItemsAdapter(SearchActivity.this, items, SearchActivity.this);
                            recyclerView.setAdapter(searchItemAdapter);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            if(items.isEmpty()){
                                if(emptyView.getVisibility()!=View.VISIBLE){
                                    emptyView.setVisibility(View.VISIBLE);
                                }
                            }else {

                            } if(emptyView.getVisibility()!=View.GONE){
                                emptyView.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String rsp = responseBody.string();
                                Log.i("Search:::", rsp);
                                JSONArray data = new JSONArray(rsp);
                                items = ServiceUtils.parseItems(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, Constants.INTERNET_ERROR_MSG, Toast.LENGTH_SHORT).show();
            if(items.isEmpty()){
                if(emptyView.getVisibility()!=View.VISIBLE){
                    emptyView.setVisibility(View.VISIBLE);
                }
            }else {
                if(emptyView.getVisibility()!=View.GONE){
                    emptyView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {

    }

    @Override
    public void onSearchConfirmed(CharSequence text) {
        qq = text.toString();
        startSearch(qq);
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode) {
            case MaterialSearchBar.BUTTON_NAVIGATION:
                onBackPressed();
                break;
            case MaterialSearchBar.BUTTON_BACK:
                materialSearchBar.disableSearch();
                items.clear();
                searchItemAdapter.notifyDataSetChanged();
                break;

        }
    }

    @Override
    public void OnItemClickListener(int position, View v) {
        String q = (String) lastSearches.get(position);
        qq = q;
        materialSearchBar.setText(qq);
        materialSearchBar.hideSuggestionsList();
        startSearch(q);
    }

    @Override
    public void OnItemDeleteListener(int position, View v) {
        lastSearches.remove(position);
        materialSearchBar.setLastSuggestions(lastSearches);
        if (lastSearches.isEmpty()) {
            materialSearchBar.hideSuggestionsList();
            materialSearchBar.clearSuggestions();
        }

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
                                searchItemAdapter.notifyItemChanged(position);
                            }else {
                                //like
                                item.setLiked(true);
                                items.get(position).setLiked(true);
                                items.get(position).setLikes(item.getLikes()+1);
                                searchItemAdapter.notifyItemChanged(position);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            Toast.makeText(SearchActivity.this, Constants.CONNECTION_ERROR, Toast.LENGTH_SHORT).show();
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(followingSubscription!=null&&!followingSubscription.isUnsubscribed()){
            followingSubscription.unsubscribe();
        }
        if(searchSubscription!=null&&!searchSubscription.isUnsubscribed()){
            searchSubscription.unsubscribe();
        }
        if(sponserSubscription!=null&&!sponserSubscription.isUnsubscribed()){
            sponserSubscription.unsubscribe();
        }
    }
}
