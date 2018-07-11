package com.example.xyzreader.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.utils.Utils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.utils.Utils.*;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    @BindView(R.id.nestedScrollView)
    NestedScrollView mScrollView;
    @BindView(R.id.thumbnail)
    ImageView mPhotoView;
    @Nullable
    @BindView(R.id.article_app_bar)
    AppBarLayout parallaxBar;
    @BindView(R.id.share_fab)
    FloatingActionButton mShareFab;
    @BindView(R.id.article_title)
    TextView titleView;
    @BindView(R.id.article_subtitle)
    TextView subtitleView;
    @BindView(R.id.article_detail_toolbar)
    Toolbar detailToolbar;
    @BindView(R.id.article_body)
    TextView bodyView;
    @BindView(R.id.body_progress_bar)
    ProgressBar loadingBody;
    @BindView(R.id.collapising_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;


    private int mCurrentPosition;
    private String mCurrentImageTransitionName;
    private int mutedColor;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case LONG_TEXT:
                    bodyView.setText(msg.obj.toString());
                    bodyView.setVisibility(View.VISIBLE);
                    loadingBody.setVisibility(View.GONE);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        supportPostponeEnterTransition();

        getSupportLoaderManager().initLoader(0, null, this);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        if(intent.hasExtra(CURRENT_ARTICLE_POSITION)) {
            mCurrentPosition = intent.getIntExtra(CURRENT_ARTICLE_POSITION, 0);
            mCurrentImageTransitionName = intent.getStringExtra(CURRENT_ARTICLE_TRANSITION_NAME);
            mutedColor = intent.getIntExtra(MUTED_COLOR_VALUE, MUTED_COLOR);
        }
        else finish();

        mPhotoView.setTransitionName(mCurrentImageTransitionName);
        loadingBody.setVisibility(View.VISIBLE);
        bodyView.setVisibility(View.GONE);
        collapsingToolbarLayout.setContentScrimColor(mutedColor);

        Window window = getWindow();
        window.setStatusBarColor(mutedColor);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_ARTICLE_POSITION, mCurrentPosition);
    }

    @Override
    public void finishAfterTransition() {;
        Intent data = new Intent();
        data.putExtra(CURRENT_ARTICLE_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor cursor) {

        if(cursor == null || cursor.isClosed() || !cursor.moveToFirst()) {
            return;
        }

        cursor.moveToPosition(mCurrentPosition);

        final String title = cursor.getString(ArticleLoader.Query.TITLE);

        String author = Html.fromHtml(
                DateUtils.getRelativeTimeSpanString(
                        cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by "
                        + cursor.getString(ArticleLoader.Query.AUTHOR)).toString();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String body = Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")).toString();
                mHandler.sendMessage(mHandler.obtainMessage(LONG_TEXT, body));
            }
        });
        thread.start();

        String photo = cursor.getString(ArticleLoader.Query.PHOTO_URL);

        if(detailToolbar != null){
            detailToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            detailToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        titleView.setText(title);
        titleView.setTextColor(mutedColor);
        subtitleView.setText(author);
        subtitleView.setTextColor(mutedColor);

        Picasso.get().load(photo)
                .into(mPhotoView, new Callback() {
                    @Override
                    public void onSuccess() {
                        supportStartPostponedEnterTransition();
                    }

                    @Override
                    public void onError(Exception e) {
                        supportStartPostponedEnterTransition();
                    }
                });

        //final Activity activity = this;
        mShareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String shareString = getString(R.string.action_share);

                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText(shareString)
                        .getIntent(), shareString));
            }
        });

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}


