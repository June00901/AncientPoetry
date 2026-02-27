package ee.example.ancient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

//功能：自定义适配器，用于在列表中展示 NewsBean 数据。
//主要功能：
//继承自 BaseAdapter，实现对数据的绑定和展示。
//提供对列表项的点击事件处理。

public class MyAdapter2 extends BaseAdapter{

    private List<NewsBean> mData;
    private Context mContext;

    public MyAdapter2(List<NewsBean> mData, Context mContext) {
        this.mData = mData;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.txt_item_title = (TextView) convertView.findViewById(R.id.txt_item_title);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.txt_item_title.setText(mData.get(position).getTitle());
        return convertView;
    }

    private class ViewHolder{
        TextView txt_item_title;
    }

}