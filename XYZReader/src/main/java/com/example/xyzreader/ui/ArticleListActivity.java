package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.facebook.stetho.Stetho;
import com.squareup.picasso.Picasso;


/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    protected final String TAG = getClass().getSimpleName();


    private Bundle mTmpReenterState;
    private boolean mIsDetailsActivityStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        Stetho.initializeWithDefaults(this);

        //final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
                updateRefreshingUI();
            }
        });


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }


    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                Log.i(TAG, "Refresh menu item selected");

                updateRefreshingUI();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }





    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        Log.d(TAG, "mIsRefreshing : " + mIsRefreshing);
        // mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
        mSwipeRefreshLayout.setRefreshing(true);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        },3000);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));

                    ActivityOptionsCompat options = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        Pair<View, String> p1 = Pair.create(view.findViewById(R.id.thumbnail),
                                view.findViewById(R.id.thumbnail).getTransitionName());

                        Pair<View, String> p2 = Pair.create(view.findViewById(R.id.article_title),
                                view.findViewById(R.id.article_title).getTransitionName());

                        Pair<View, String> p3 = Pair.create(view.findViewById(R.id.article_subtitle),
                                view.findViewById(R.id.article_subtitle).getTransitionName());

//                        options = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                                ArticleListActivity.this,
//                                (View) view.findViewById(R.id.thumbnail),
//                                view.findViewById(R.id.thumbnail).getTransitionName());

                        options = ActivityOptionsCompat.makeSceneTransitionAnimation(ArticleListActivity.this, p1, p2, p3);

                        startActivity(intent, options.toBundle());
                    } else {
                        startActivity(intent);
                    }

                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder( final ViewHolder holder, int position) {
            int mWidth = holder.thumbnailView.getMeasuredWidth();
            ViewTreeObserver vto = holder.thumbnailView.getViewTreeObserver();

//            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                @Override
//                public boolean onPreDraw() {
//                    holder.thumbnailView.getViewTreeObserver().removeOnPreDrawListener();
//                    mWidth = holder.thumbnailView.getMeasuredWidth();
//                    return false;
//                }
//            });
            mCursor.moveToPosition(position);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.thumbnailView.setTransitionName(getString(R.string.imgTransitionName)+position);
                holder.titleView.setTransitionName(getString(R.string.titleTransitionName)+position);
                holder.subtitleView.setTransitionName(getString(R.string.subtitleTransitionName)+position);
                Log.v(TAG, "TransitionName List:" + holder.thumbnailView.getTransitionName());
            }
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));

//            holder.thumbnailView.setImageUrl(
//                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
//                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());


//            Transformation transformation = new Transformation( ) {
//
//
//                @Override
//                public Bitmap transform(Bitmap source) {
//                    int targetWidth = mWidth;
//
//                    float aspectRatio = (float) mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);
//                    Log.v(TAG, "width x height " + targetWidth + " x " + (int) (targetWidth / aspectRatio));
//
//                    int targetHeight = (int) (targetWidth / aspectRatio);
//                    Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
//
//                    if (result != source) {
//                        // Same bitmap is returned if sizes are the same
//                        source.recycle();
//                    }
//                    return result;
//                }
//
//
//                @Override
//                public String key() {
//                    return "transformation" + " desiredWidth";
//                }
//            };
            
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            Picasso.with(getApplicationContext()).load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                   .into(holder.thumbnailView);
        // holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
