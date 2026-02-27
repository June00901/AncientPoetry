package ee.example.ancient;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//功能：自定义适配器，用于在 RecyclerView 中展示 PlaceBean 数据。
//主要功能：
//继承自 RecyclerView.Adapter，实现对数据的绑定和展示。
//提供对列表项的点击事件处理，支持设置点击监听器

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.InnerHolder> {

    private final List<PlaceBean> mData = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    private Context context;

    public MyAdapter(Context context) {
        this.context = context;
    }

    public void setNewData(List<PlaceBean> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public List<PlaceBean> getmData() {
        return mData;
    }

    @NonNull
    @Override
    public InnerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.item_stagger, null);
        return new InnerHolder(view);
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    @Override
    public void onBindViewHolder(@NonNull final InnerHolder holder, int position) {
        holder.setData(mData.get(position));
        if (mOnItemClickListener != null) {
            //为ItemView设置监听器
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getLayoutPosition(); // 1
                    mOnItemClickListener.onItemClick(holder.itemView, position); // 2
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        //返回条目个数
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    public class InnerHolder extends RecyclerView.ViewHolder {
        private TextView mTitle;
        private ImageView mIcon;

        public InnerHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mIcon = (ImageView) itemView.findViewById(R.id.icon);
        }

        public void setData(PlaceBean itemBean) {
            mTitle.setText(itemBean.getName());
            mIcon.setImageResource(getDrawableId(context, itemBean.getPic()));
        }

    }

    public static int getDrawableId(Context context, String var) {

        try {
            int imageId = context.getResources().getIdentifier(var, "mipmap", context.getPackageName());
            return imageId;
        } catch (Exception e) {
            return 0;
        }
    }
}
