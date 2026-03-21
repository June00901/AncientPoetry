package ee.example.ancient;

import static ee.example.ancient.MyAdapter.getDrawableId;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

//功能：显示古诗的详细信息。
//主要功能：
//显示古诗的标题、内容、译文和诗人信息。
//提供收藏功能，允许用户将古诗收藏到数据库。
//根据用户的登录状态控制收藏按钮的可见性。

//发现 古诗详情
public class GuShiDetailActivity extends AppCompatActivity {
 private ImageView imageView1;
 private TextView textView1,textView2;
 private SQLiteHelper dbHelper;
 private List<PlaceBean> list;
 private Button btnCollect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        
        dbHelper = new SQLiteHelper(this);
        
        // 初始化按钮
        btnCollect = findViewById(R.id.btnCollect);
        
        // 根据登录状态控制收藏按钮的可见性
        if (!Data.sta_np || Data.sta_name == null) {
            btnCollect.setVisibility(View.GONE);
        } else {
            btnCollect.setVisibility(View.VISIBLE);
        }
        
        btnCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = ((TextView)findViewById(R.id.conText1)).getText().toString();
                String content = ((TextView)findViewById(R.id.conText2)).getText().toString();
                saveCollection(title, content);
            }
        });
        
        // 获取数据
        Intent intent = getIntent();
        String place = intent.getStringExtra("id");
        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        list = database.findById(place);
        
        // 初始化视图
        initData();
        
        // 检查收藏状态
        checkCollectionStatus();
    }

    private void initData(){
        imageView1=(ImageView)findViewById(R.id.conImage1);
        textView1=findViewById(R.id.conText1);
        textView2=findViewById(R.id.conText2);
        TextView translationView = findViewById(R.id.translation);
        TextView poetInfoView = findViewById(R.id.poetInfo);
        
        if (list != null && !list.isEmpty()) {
            PlaceBean data = list.get(0);
            textView1.setText(data.getTitle());
            textView2.setText(data.getContent());
            translationView.setText("译文：" + data.getTranslation());
            poetInfoView.setText("诗人信息：" + data.getPoetInfo());
            imageView1.setImageResource(getDrawableId(this, data.getPic()));
        }
    }

    private void saveCollection(String title, String content) {
        if (!Data.sta_np) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        PlaceBean poetry = list.get(0);
        
        if (database.isCollected(Data.userId.intValue(), poetry.getId())) {
            // 已收藏，执行取消收藏
            int result = database.removeCollection(Data.userId.intValue(), poetry.getId());
            if (result > 0) {
                Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
                btnCollect.setText("收藏");
            }
        } else {
            // 未收藏，执行收藏
            long result = database.addCollection(
                Data.userId.intValue(),
                poetry.getId(),
                title,
                content
            );
            
            if (result != -1) {
                Toast.makeText(this, "收藏成功", Toast.LENGTH_SHORT).show();
                btnCollect.setText("已收藏");
            } else {
                Toast.makeText(this, "收藏失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkCollectionStatus() {
        if (!Data.sta_np || Data.userId == null || list == null || list.isEmpty()) {
            btnCollect.setVisibility(View.GONE);
            return;
        }
        
        btnCollect.setVisibility(View.VISIBLE);
        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        PlaceBean poetry = list.get(0);
        
        if (database.isCollected(Data.userId.intValue(), poetry.getId())) {
            btnCollect.setText("已收藏");
        } else {
            btnCollect.setText("收藏");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
        checkCollectionStatus();
    }

    private void checkLoginStatus() {
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        boolean isLoggedIn = sp.getBoolean("isLoggedIn", false);
        
        if (!isLoggedIn) {
            Data.sta_np = false;
            Data.sta_name = null;
            Data.userId = null;
        }
    }
}
