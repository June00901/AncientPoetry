package ee.example.ancient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

//功能：实现搜索古诗词的功能。
// 主要功能：
// 监听用户输入的搜索关键词，并实时更新搜索结果。
//直接使用本地数据库进行搜索。

//发现
@SuppressLint("ValidFragment")
public class TabFragment_Find extends Fragment {
    private static final String TAG = "TabFirstFragment";
    protected View mView;
    MyAdapter adapter;
    private RecyclerView mlist;//转换为成员变量
    protected Context mContext;

    private PlaceDatabase database;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getContext() == null || mView == null) {
            return;
        }

        database = new PlaceDatabase(getContext(), PlaceDatabase.DATABASE_NAME, null, 1);
        mlist = mView.findViewById(R.id.recycler_view);
        showStagger();

        // 设置点击事件
        adapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (getActivity() == null || position < 0 || position >= adapter.getmData().size()) {
                    return;
                }
                PlaceBean data = adapter.getmData().get(position);
                Intent intent = new Intent();
                intent.putExtra("id", data.getId());
                intent.setClass(getActivity(), GuShiDetailActivity.class);
                startActivity(intent);
            }
        });

        // 设置搜索监听
        SearchView sv = mView.findViewById(R.id.sv);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                key = newText == null ? "" : newText.trim();
                safeSearchAndBind(key);
                return true;
            }
        });

        // 初始加载数据
        loadPoetryData();
    }

    private void showStagger() {

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, LinearLayout.VERTICAL);
        //这只布局管理器方向
        layoutManager.setReverseLayout(false);
        mlist.setLayoutManager(layoutManager);

        adapter = new MyAdapter(getContext());
        mlist.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        mContext = getActivity();
        mView = inflater.inflate(R.layout.find, container, false);

        return mView;
    }

    private String key = "";

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        safeSearchAndBind(key);
    }

    private void loadPoetryData() {
        safeSearchAndBind("");
    }

    private void searchPoetry(String key) {
        safeSearchAndBind(key);
    }

    private void safeSearchAndBind(String keyword) {
        if (database == null || adapter == null) {
            return;
        }
        String searchKey = keyword == null ? "" : keyword.trim();
        List<PlaceBean> list = database.find(searchKey);
        adapter.setNewData(list);
    }

}
