package ee.example.ancient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ee.example.ancient.adapter.MyCollectionAdapter;
import ee.example.ancient.model.Collection;

public class MyCollectionActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyCollectionAdapter adapter;
    private PlaceDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_collection);

        // 检查登录状态
        if (!Data.sta_np || Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = new PlaceDatabase(this, PlaceDatabase.COLLECTIONS_TABLE, null, 1);
        
        // 返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        recyclerView = findViewById(R.id.list_collections);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new MyCollectionAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 设置长按删除
        adapter.setOnItemLongClickListener((collection, position) -> {
            new AlertDialog.Builder(this)
                .setTitle("取消收藏")
                .setMessage("确定要取消收藏这首诗吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    removeCollection(collection.getPoetryId());
                })
                .setNegativeButton("取消", null)
                .show();
            return true;
        });

        loadCollections();
    }

    private void loadCollections() {
        if (!Data.sta_np || Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        List<Collection> collections = database.getAllCollections(Data.userId.intValue());
        if (collections != null && !collections.isEmpty()) {
            adapter.setCollections(collections);
        } else {
            Toast.makeText(this, "还没有收藏任何诗词", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeCollection(String poetryId) {
        if (!Data.sta_np || Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        int result = database.removeCollection(Data.userId.intValue(), poetryId);
        if (result > 0) {
            Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
            loadCollections();  // 重新加载收藏列表
        } else {
            Toast.makeText(this, "取消收藏失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时检查登录状态并刷新数据
        if (!Data.sta_np || Data.userId == null) {
            finish();
        } else {
            loadCollections();
        }
    }
} 