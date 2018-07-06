package com.example.xyzreader.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.DynamicHeightNetworkImageView;
import com.example.xyzreader.ui.ImageLoaderHelper;
import com.example.xyzreader.utils.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ArticleViewHolder> {
    private Cursor mCursor;
    private Context mContext;
    private int mResource;
    private int mPosition;

    public ArticleListAdapter(Cursor cursor, Context context, int resource) {
        mCursor = cursor;
        mContext = context;
        mResource = resource;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(mResource, parent, false);


        final ArticleViewHolder vh = new ArticleViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                mPosition = vh.getAdapterPosition();
                ActivityOptionsCompat activityOptionsCompat
                        = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                (Activity) mContext,
                                vh.thumbnailView,
                                vh.thumbnailView.getTransitionName());

                Intent intent = new Intent(Intent.ACTION_VIEW,
                        ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                intent.putExtra(Utils.STARTING_ARTICLE_POSITION, mPosition);
                mContext.startActivity(intent, activityOptionsCompat.toBundle());
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ArticleViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        mPosition = position;

        //Setting Transition Name and Tag
        holder.thumbnailView.setTransitionName(mContext.getString(R.string.book_image) + position);
        holder.thumbnailView.setTag(mContext.getString(R.string.book_image) + position);

        //Getting from DB
        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        String subtitle = DateUtils.getRelativeTimeSpanString(
                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString();
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        String image = mCursor.getString(ArticleLoader.Query.THUMB_URL);

        //Setting fields
        holder.titleView.setText(title);
        holder.subtitleView.setText(subtitle);
        holder.authorView.setText(author);
        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

        //Getting image's DarkMutedColor
        ImageLoader loader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
        holder.thumbnailView.setImageUrl(image, loader);
        loader.get(image, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                Bitmap bitmap = imageContainer.getBitmap();
                if(bitmap != null) {
                    Palette p = Palette.from(bitmap).generate();
                    int mMutedColor = p.getDarkVibrantColor(Utils.DEFAULT_COLOR);
                    if(mMutedColor == Utils.DEFAULT_COLOR) {
                        mMutedColor = p.getLightVibrantColor(Utils.DEFAULT_COLOR);
                        if(mMutedColor == Utils.DEFAULT_COLOR) {
                            mMutedColor = p.getVibrantColor(Utils.DEFAULT_COLOR);
                        }
                    }
                    holder.itemView.setBackgroundColor(mMutedColor);
                    holder.thumbnailView.setBackgroundColor(mMutedColor);
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }


    class ArticleViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail) DynamicHeightNetworkImageView thumbnailView;
        @BindView(R.id.article_title) TextView titleView;
        @BindView(R.id.article_subtitle) TextView subtitleView;
        @BindView(R.id.article_author) TextView authorView;
        @BindView(R.id.card_view) CardView cardView;

        public ArticleViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
