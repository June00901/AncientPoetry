package ee.example.ancient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

//功能：展示特定主题的古诗列表。
//主要功能：
//根据传入的主题从数据库中查询相关的古诗。
//使用 ListView 显示古诗的标题，支持点击查看古诗的详细信息。
//提供用户反馈，当没有相关古诗时显示提示信息。

public class ThemeListActivity extends AppCompatActivity {
    private ListView listView;
    private PlaceDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_list);

        try {
            String theme = getIntent().getStringExtra("theme");
            database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);

            // 设置标题
            TextView titleView = findViewById(R.id.title);
            titleView.setText(theme);

            // 获取并显示列表
            listView = findViewById(R.id.list_poems);
            List<PlaceBean> poems = database.findByTheme(theme);
            
            if (poems.isEmpty()) {
                Toast.makeText(this, "暂无" + theme + "主题的诗词", Toast.LENGTH_SHORT).show();
            }
            
            ArrayAdapter<PlaceBean> adapter = new ArrayAdapter<PlaceBean>(this,
                    android.R.layout.simple_list_item_1, poems) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = view.findViewById(android.R.id.text1);
                    text.setText(getItem(position).getTitle());
                    return view;
                }
            };
            
            listView.setAdapter(adapter);

            // 点击跳转到详情
            listView.setOnItemClickListener((parent, view, position, id) -> {
                PlaceBean poem = poems.get(position);
                Intent intent = new Intent(ThemeListActivity.this, GuShiDetailActivity.class);
                intent.putExtra("id", poem.getId());
                startActivity(intent);
            });
        } catch (Exception e) {
            Log.e("ThemeListActivity", "Error loading theme list", e);
            Toast.makeText(this, "加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 
