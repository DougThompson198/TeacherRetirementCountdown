package teacher.retirementcountdown.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import teacher.retirementcountdown.app.model.BucketListItem;
import teacher.retirementcountdown.app.R;

public class BucketListAdapter extends RecyclerView.Adapter<BucketListAdapter.BucketListViewHolder> {

    private final List<BucketListItem> bucketListItems;
    private final OnItemDeleteListener deleteListener;
    private final OnItemCheckListener checkListener;

    public interface OnItemDeleteListener {
        void onDelete(BucketListItem item);
    }

    public interface OnItemCheckListener {
        void onCheck(BucketListItem item, boolean isChecked);
    }

    public BucketListAdapter(List<BucketListItem> bucketListItems,
                             OnItemDeleteListener deleteListener,
                             OnItemCheckListener checkListener) {
        this.bucketListItems = bucketListItems != null ? bucketListItems : new ArrayList<>();
        this.deleteListener = deleteListener;
        this.checkListener = checkListener;
    }

    @NonNull
    @Override
    public BucketListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bucket_list, parent, false);
        return new BucketListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BucketListViewHolder holder, int position) {
        BucketListItem item = bucketListItems.get(position);

        holder.textViewItem.setText(item.getName());
        holder.checkBox.setChecked(item.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                checkListener.onCheck(item, isChecked)
        );

        holder.deleteButton.setOnClickListener(v -> deleteListener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return bucketListItems.size();
    }

    static class BucketListViewHolder extends RecyclerView.ViewHolder {

        TextView textViewItem;
        CheckBox checkBox;
        ImageView deleteButton;

        public BucketListViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItem = itemView.findViewById(R.id.textViewItem);
            checkBox = itemView.findViewById(R.id.checkBox);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
