package ee.example.ancient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ee.example.ancient.utils.PoemApiClient;

public class AiOnlineSearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private Button btnSearch;
    private TextView tvResult;
    private ScrollView scrollView;
    private PoemApiClient poemApiClient;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_online_search);

        etSearchQuery = findViewById(R.id.et_search_query);
        btnSearch = findViewById(R.id.btn_search);
        tvResult = findViewById(R.id.tv_result);
        scrollView = findViewById(R.id.scroll_view);

        poemApiClient = new PoemApiClient();
        mainHandler = new Handler(Looper.getMainLooper());

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = etSearchQuery.getText().toString().trim();
                if (TextUtils.isEmpty(query)) {
                    Toast.makeText(AiOnlineSearchActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                performAiSearch(query);
            }
        });
    }

    private void performAiSearch(String query) {
        tvResult.setText("🔍 正在深度联网搜索「" + query + "」...\n⏳ 请稍候（约5-8秒）");
        btnSearch.setEnabled(false);

        String prompt = "你是一位博学的古诗词研究专家，拥有丰富的古诗词知识库和联网搜索能力。\n\n"
                + "用户需要搜索关于「" + query + "」的古诗词信息。\n\n"
                + "【核心要求】\n"
                + "1. 搜索并整理相关的经典古诗词\n"
                + "2. 提供诗词的完整信息（诗名、作者、朝代、全文）\n"
                + "3. 给出详细的赏析和解读\n"
                + "4. 如果查询的是诗句，提供出处和背景\n"
                + "5. 如果查询的是诗人，介绍其生平和代表作品\n\n"
                + "【输出格式】\n\n"
                + "【搜索结果】\n"
                + "根据您的搜索「" + query + "」，为您找到以下内容：\n\n"
                + "【诗词信息】\n"
                + "诗名：《》\n"
                + "作者：\n"
                + "朝代：\n\n"
                + "【全文】\n"
                + "（诗词全文，注意换行，保持原格式）\n\n"
                + "【诗词赏析】\n"
                + "（详细赏析，100字左右）\n\n"
                + "【创作背景】\n"
                + "（创作背景，80字左右）\n\n"
                + "【相关推荐】\n"
                + "（推荐1-2首相关诗词，每首包含诗名和作者）";

        poemApiClient.callApiWithPrompt(prompt, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String result) {
                tvResult.setText(result);
                btnSearch.setEnabled(true);
                mainHandler.postDelayed(() -> {
                    scrollView.fullScroll(ScrollView.FOCUS_UP);
                }, 100);
            }

            @Override
            public void onError(String error) {
                tvResult.setText("搜索失败，请稍后重试。\n\n错误信息：" + error);
                btnSearch.setEnabled(true);
                Toast.makeText(AiOnlineSearchActivity.this, "API调用失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
