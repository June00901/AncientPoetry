package ee.example.ancient;
//警告：此项目已禁用
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineSearchActivity extends AppCompatActivity {

    private EditText etKeyword;
    private Button btnSearch;
    private TextView tvResult;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_search);

        etKeyword = findViewById(R.id.et_search_keyword);
        btnSearch = findViewById(R.id.btn_search);
        tvResult = findViewById(R.id.tv_result);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etKeyword.getText().toString().trim();
                if (TextUtils.isEmpty(keyword)) {
                    Toast.makeText(OnlineSearchActivity.this, "请输入关键词", Toast.LENGTH_SHORT).show();
                    return;
                }
                searchPoems(keyword);
            }
        });
    }

    private void searchPoems(String keyword) {
        tvResult.setText("正在搜索「" + keyword + "」...");
        btnSearch.setEnabled(false);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 今日诗词 API 没有搜索功能，我们用随机获取多次来实现近似效果
                    // 这里改用唐诗宋词API（需要付费）或模拟数据
                    // 为了演示，先用模拟数据
                    Thread.sleep(1500); // 模拟网络延迟

                    String mockResult = "【搜索结果】\n\n"
                            + "1.《静夜思》·李白\n"
                            + "床前明月光，疑是地上霜。\n举头望明月，低头思故乡。\n\n"
                            + "2.《登鹳雀楼》·王之涣\n"
                            + "白日依山尽，黄河入海流。\n欲穷千里目，更上一层楼。\n\n"
                            + "3.《春晓》·孟浩然\n"
                            + "春眠不觉晓，处处闻啼鸟。\n夜来风雨声，花落知多少。\n\n"
                            + "4.《相思》·王维\n"
                            + "红豆生南国，春来发几枝。\n愿君多采撷，此物最相思。\n\n"
                            + "5.《江雪》·柳宗元\n"
                            + "千山鸟飞绝，万径人踪灭。\n孤舟蓑笠翁，独钓寒江雪。";

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvResult.setText(mockResult);
                            btnSearch.setEnabled(true);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvResult.setText("搜索失败：" + e.getMessage());
                            btnSearch.setEnabled(true);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}