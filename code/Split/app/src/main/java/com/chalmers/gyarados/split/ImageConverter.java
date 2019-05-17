package com.chalmers.gyarados.split;

import android.content.Context;

import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ImageConverter {
    public static void loadRoundedImage(Context context, String photourl, ImageView imageview, int width, int height){
        Picasso.with(context).load(photourl).resize(width,height).transform(new CircleTransform()).into(imageview);
    }
    public static void loadRoundedImage(Context context, String photourl, ImageView imageview){
        Picasso.with(context).load(photourl).transform(new CircleTransform()).into(imageview);
    }

    public static void loadRoundedDrawable(Context context, int drawable_id, ImageView imageView, int width, int height) {
        Picasso.with(context).load(drawable_id).resize(width, height).transform(new CircleTransform()).into(imageView);
    }

    public static void loadRoundedDrawable(Context context, int drawable_id, ImageView imageView) {
        Picasso.with(context).load(drawable_id).transform(new CircleTransform()).into(imageView);
    }
}

