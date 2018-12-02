package com.joseph.spare.callbacks;

/**
 * Click listeners for individual item
 */
public interface ItemCallBack {
    void onItemClicked(int position);
    void onItemBookmarked(int position);
    void onItemUnbookmarked(int position);
    void onItemLiked(int position);
    void onComment(int position);
    void onAvatarClicked(int position);
}
