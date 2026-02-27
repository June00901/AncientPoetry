package ee.example.ancient.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ee.example.ancient.GuShiDetailActivity;
import ee.example.ancient.R;
import ee.example.ancient.model.Collection;
import java.util.List;

public class MyCollectionAdapter extends RecyclerView.Adapter<MyCollectionAdapter.ViewHolder> {
    private List<Collection> collections;
    private OnItemLongClickListener onItemLongClickListener;

    public MyCollectionAdapter(List<Collection> collections) {
        this.collections = collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
        notifyDataSetChanged();
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(Collection collection, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Collection collection = collections.get(position);
        holder.tvTitle.setText(collection.getTitle());
        holder.tvContent.setText(collection.getContent());

        // 点击查看详情
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), GuShiDetailActivity.class);
            intent.putExtra("id", collection.getPoetryId());
            v.getContext().startActivity(intent);
        });

        // 长按删除
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                return onItemLongClickListener.onItemLongClick(collection, position);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvContent;

        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvContent = view.findViewById(R.id.tvContent);
        }
    }
} 