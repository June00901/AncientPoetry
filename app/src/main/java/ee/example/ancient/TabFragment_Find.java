package ee.example.ancient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private ExecutorService queryExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int querySeq = 0;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getContext() == null || mView == null) {
            return;
        }
        if (queryExecutor == null || queryExecutor.isShutdown()) {
            queryExecutor = Executors.newSingleThreadExecutor();
        }

        database = new PlaceDatabase(getContext(), PlaceDatabase.DATABASE_NAME, null, 1);
        mlist = mView.findViewById(R.id.recycler_view);
        showStagger();

        Button btnAiSearch = mView.findViewById(R.id.btn_ai_search);
        Button btnSearch = mView.findViewById(R.id.btn_search);
        EditText etDescSearch = mView.findViewById(R.id.et_desc_search);

        btnAiSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String descQuery = etDescSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(descQuery)) {
                    // 先在本地搜索
                    String searchKey = descQuery;
                    List<PlaceBean> list = database.find(searchKey);
                    if (list.isEmpty()) {
                        // 本地无结果，提示跳转到联网搜索
                        showNoResultDialog(descQuery);
                    } else {
                        // 本地有结果，显示结果
                        adapter.setNewData(list);
                    }
                } else {
                    // 无输入，直接跳转到联网搜索
                    Intent intent = new Intent(getActivity(), AiOnlineSearchActivity.class);
                    startActivity(intent);
                }
            }
        });

        // 搜索按钮点击事件
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchView sv = mView.findViewById(R.id.sv);
                String query = sv.getQuery().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    // 先在本地搜索
                    List<PlaceBean> list = database.find(query);
                    if (list.isEmpty()) {
                        // 本地无结果，提示跳转到联网搜索
                        showNoResultDialog(query);
                    } else {
                        // 本地有结果，显示结果
                        adapter.setNewData(list);
                    }
                }
            }
        });

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
                String searchQuery = query == null ? "" : query.trim();
                if (!TextUtils.isEmpty(searchQuery)) {
                    // 先在本地搜索
                    List<PlaceBean> list = database.find(searchQuery);
                    if (list.isEmpty()) {
                        // 本地无结果，提示跳转到联网搜索
                        showNoResultDialog(searchQuery);
                    } else {
                        // 本地有结果，显示结果
                        adapter.setNewData(list);
                    }
                }
                return true;
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
        if (database == null || adapter == null || queryExecutor == null || queryExecutor.isShutdown()) {
            return;
        }
        String searchKey = keyword == null ? "" : keyword.trim();
        int limit = TextUtils.isEmpty(searchKey) ? 120 : 500;
        int currentSeq = ++querySeq;
        queryExecutor.execute(() -> {
            List<PlaceBean> list = database.find(searchKey, limit);
            mainHandler.post(() -> {
                if (!isAdded() || adapter == null) {
                    return;
                }
                if (currentSeq != querySeq) {
                    return;
                }
                adapter.setNewData(list);
            });
        });
    }

    private void showNoResultDialog(String searchQuery) {
        new AlertDialog.Builder(getContext())
                .setTitle("搜索提示")
                .setMessage("是否跳转到联网搜索界面？")
                .setPositiveButton("是", (dialog, which) -> {
                    // 跳转到联网搜索界面，并传递搜索内容
                    Intent intent = new Intent(getActivity(), AiOnlineSearchActivity.class);
                    intent.putExtra("desc_search_query", searchQuery);
                    startActivity(intent);
                })
                .setNegativeButton("否", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (queryExecutor != null) {
            queryExecutor.shutdownNow();
            queryExecutor = null;
        }
    }

}
