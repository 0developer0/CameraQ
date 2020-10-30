package com.example.qcameraq;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GalleyAdapter extends RecyclerView.Adapter<GalleyAdapter.GalleryViewHolder> {
    ArrayList<Bitmap> gallery = new ArrayList<>();

    private static final String TAG = "GalleyAdapter";

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.gallery_preview,
                parent,
                false
        );
        Log.e(TAG, "onCreateViewHolder: ");
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        Log.e(TAG, "onBindViewHolder: position =>" + position);
        holder.bindGallery(gallery.get(position));
    }

    @Override
    public int getItemCount() {
        return gallery.size();
    }

    public void addItem(Bitmap bitmap){
        gallery.add(bitmap);
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<Bitmap> gallery){
        this.gallery.addAll(gallery);
        notifyDataSetChanged();
    }

    public class GalleryViewHolder extends RecyclerView.ViewHolder{
        ImageView img_preview;
        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            img_preview = itemView.findViewById(R.id.img_pre_view);
        }

        public void bindGallery(Bitmap bitmap){
            img_preview.setImageBitmap(bitmap);
        }
    }
}
