package com.example.xyzreader.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.utils.Utils.*;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private Cursor mCursor;
    private long mStartId;

    @BindView(R.id.nestedScrollView)
    NestedScrollView mScrollView;
    @BindView(R.id.article_image_container)
    View mPhotoContainerView;
    @BindView(R.id.thumbnail)
    ImageView mPhotoView;
    @Nullable
    @BindView(R.id.article_app_bar)
    AppBarLayout parallaxBar;
    @BindView(R.id.share_fab)
    FloatingActionButton mShareFab;
    @BindView(R.id.meta_bar)
    LinearLayout metaBar;
    @BindView(R.id.article_title)
    TextView titleView;
    @BindView(R.id.article_byline)
    TextView bylineView;
    @BindView(R.id.article_detail_toolbar)
    Toolbar detailToolbar;
    @BindView(R.id.article_body)
    TextView bodyView;
    @BindView(R.id.body_progress_bar)
    ProgressBar loadingBody;


    private int mCurrentPosition;
    private String mCurrentImageTransitionName;


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
        }
        else finish();

        mPhotoView.setTransitionName(mCurrentImageTransitionName);
        loadingBody.setVisibility(View.VISIBLE);
        bodyView.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARTICLE_ID, mStartId);
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
        // Select the start ID
        /*if (mStartId > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (cursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = cursor.getPosition();
                    mCursor = cursor;
                    break;
                }
                cursor.moveToNext();
            }

        }*/
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

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                String body = Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)).toString();
                bodyView.setText(body);
                bodyView.setVisibility(View.VISIBLE);
                loadingBody.setVisibility(View.GONE);
                return null;
            }
        }.execute();

        String photo = cursor.getString(ArticleLoader.Query.PHOTO_URL);

        if(detailToolbar != null){
            detailToolbar.setTitle(title);
            detailToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            detailToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        titleView.setText(title);
        bylineView.setText(author);
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
                });;

        mShareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from((Activity) getApplicationContext())
                        .setType("text/plain")
                        .setText(getString(R.string.action_share))
                        .getIntent(), getString(R.string.action_share)));
            }
        });

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
    }
}


