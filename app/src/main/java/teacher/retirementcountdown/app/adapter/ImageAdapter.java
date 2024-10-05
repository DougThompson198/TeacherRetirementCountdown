package teacher.retirementcountdown.app.adapter;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;


import java.util.List;
import java.util.Set;

import teacher.retirementcountdown.app.R;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<String> imageList;
    private final Set<String> selectedImages;
    private final OnImageDeleteListener deleteListener;
    private final OnImageSelectListener selectListener;

    public interface OnImageDeleteListener {
        void onDelete(String imageUri);
    }

    public interface OnImageSelectListener {
        void onSelect(String imageUri);
    }

    public ImageAdapter(List<String> imageList, Set<String> selectedImages,
                        OnImageDeleteListener deleteListener, OnImageSelectListener selectListener) {
        this.imageList = imageList;
        this.selectedImages = selectedImages;
        this.deleteListener = deleteListener;
        this.selectListener = selectListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUri = imageList.get(position);

        try {
            int drawableResId = Integer.parseInt(imageUri);
            Glide.with(holder.itemView.getContext()).load(drawableResId).into(holder.imageView);
            Log.d("yeni", "drawableResId1 " + drawableResId);
        } catch (NumberFormatException e) {
            Glide.with(holder.itemView.getContext()).load(Uri.parse(imageUri)).into(holder.imageView);
            Log.d("yeni", "drawableResId2 " + Uri.parse(imageUri));
        }

        if (selectedImages.contains(imageUri)) {
            holder.selectButton.setVisibility(View.VISIBLE);
        } else {
            holder.selectButton.setVisibility(View.INVISIBLE);
        }

        holder.imageView.setOnClickListener(v -> {
                    selectListener.onSelect(imageUri);
                }
        );

        if (!imageUri.startsWith("android.resource")) {
            if (holder.selectButton.getVisibility() == View.INVISIBLE) {
                holder.deleteButton.setVisibility(View.VISIBLE);
            }

            holder.deleteButton.setOnClickListener(v -> {

                        if (selectedImages.contains(imageUri)) {
                            selectListener.onSelect(imageUri);
                        } else {
                            deleteListener.onDelete(imageUri);
                        }
                    }
            );
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        ImageView deleteButton;
        ImageView selectButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            selectButton = itemView.findViewById(R.id.selectButton);
        }
    }
}
