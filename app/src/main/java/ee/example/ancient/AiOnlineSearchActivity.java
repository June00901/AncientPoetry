package ee.example.ancient;

import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;

public class AiOnlineSearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private EditText etDescSearch;
    private Button btnSearch;
    private TextView tvResult;
    private ScrollView scrollView;
    private TextView tvRecommend1;
    private TextView tvRecommend2;
    private TextView tvRecommend3;
    private PoemApiClient poemApiClient;
    private Handler mainHandler;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_online_search);

        etSearchQuery = findViewById(R.id.et_search_query);
        etDescSearch = findViewById(R.id.et_desc_search);
        btnSearch = findViewById(R.id.btn_search);
        tvResult = findViewById(R.id.tv_result);
        scrollView = findViewById(R.id.scroll_view);
        tvRecommend1 = findViewById(R.id.tv_recommend_1);
        tvRecommend2 = findViewById(R.id.tv_recommend_2);
        tvRecommend3 = findViewById(R.id.tv_recommend_3);

        poemApiClient = new PoemApiClient();
        mainHandler = new Handler(Looper.getMainLooper());
        dbHelper = new DBHelper(this);
        dbHelper.createDatabase();

        // 设置推荐古诗点击事件
        setupRecommendClickListeners();
        // 加载智能推荐
        loadIntelligentRecommendations();

        // 接收从本地搜索传递过来的描述型搜索内容
        Intent intent = getIntent();
        if (intent != null) {
            String descSearchQuery = intent.getStringExtra("desc_search_query");
            if (!TextUtils.isEmpty(descSearchQuery)) {
                etDescSearch.setText(descSearchQuery);
            }
        }

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = etSearchQuery.getText().toString().trim();
                String descQuery = etDescSearch.getText().toString().trim();
                
                if (!TextUtils.isEmpty(descQuery)) {
                    // 优先使用描述型查找
                    performDescSearch(descQuery);
                    // 记录搜索历史
                    dbHelper.addSearchHistory(descQuery);
                } else if (!TextUtils.isEmpty(query)) {
                    // 使用普通搜索
                    performAiSearch(query);
                    // 记录搜索历史
                    dbHelper.addSearchHistory(query);
                } else {
                    Toast.makeText(AiOnlineSearchActivity.this, "请输入搜索内容或描述", Toast.LENGTH_SHORT).show();
                }
                // 更新推荐
                loadIntelligentRecommendations();
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
                // 最终结果已通过onStream实时更新，这里可以做一些收尾工作
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

            @Override
            public void onStream(String partialContent) {
                // 实时更新搜索结果
                tvResult.setText(partialContent);
            }
        });
    }

    private void performDescSearch(String descQuery) {
        tvResult.setText("🔍 正在根据描述查找古诗「" + descQuery + "」...\n⏳ 请稍候（约5-8秒）");
        btnSearch.setEnabled(false);

        String prompt = "你是一位博学的古诗词研究专家，擅长根据描述和意境找到对应的古诗词。\n\n"
                + "用户描述：「" + descQuery + "」\n\n"
                + "【核心要求】\n"
                + "1. 根据用户的描述，找出最匹配的古诗词\n"
                + "2. 如果描述对应多首诗词，列出最相关的1-3首\n"
                + "3. 解释为什么这首诗词符合用户的描述\n"
                + "4. 提供诗词的完整信息（诗名、作者、朝代、全文）\n"
                + "5. 给出诗词的赏析和名句解读\n\n"
                + "【输出格式】\n\n"
                + "【查找结果】\n"
                + "根据您的描述「" + descQuery + "」，为您找到以下诗词：\n\n"
                + "【匹配诗词】\n"
                + "诗名：《》\n"
                + "作者：\n"
                + "朝代：\n\n"
                + "【全文】\n"
                + "（诗词全文，注意换行，保持原格式）\n\n"
                + "【匹配说明】\n"
                + "（解释这首诗词为什么符合用户的描述，100字左右）\n\n"
                + "【诗词赏析】\n"
                + "（详细赏析，80字左右）\n\n"
                + "【名句解读】\n"
                + "（解读与描述相关的名句）";

        poemApiClient.callApiWithPrompt(prompt, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String result) {
                // 最终结果已通过onStream实时更新，这里可以做一些收尾工作
                btnSearch.setEnabled(true);
                mainHandler.postDelayed(() -> {
                    scrollView.fullScroll(ScrollView.FOCUS_UP);
                }, 100);
            }

            @Override
            public void onError(String error) {
                tvResult.setText("查找失败，请稍后重试。\n\n错误信息：" + error);
                btnSearch.setEnabled(true);
                Toast.makeText(AiOnlineSearchActivity.this, "API调用失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStream(String partialContent) {
                // 实时更新搜索结果
                tvResult.setText(partialContent);
            }
        });
    }

    private void setupRecommendClickListeners() {
        tvRecommend1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String poemName = tvRecommend1.getText().toString();
                etSearchQuery.setText(poemName);
                performAiSearch(poemName);
                // 记录搜索历史
                dbHelper.addSearchHistory(poemName);
            }
        });

        tvRecommend2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String poemName = tvRecommend2.getText().toString();
                etSearchQuery.setText(poemName);
                performAiSearch(poemName);
                // 记录搜索历史
                dbHelper.addSearchHistory(poemName);
            }
        });

        tvRecommend3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String poemName = tvRecommend3.getText().toString();
                etSearchQuery.setText(poemName);
                performAiSearch(poemName);
                // 记录搜索历史
                dbHelper.addSearchHistory(poemName);
            }
        });
    }

    private void loadIntelligentRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // 1. 从搜索历史获取推荐
        List<String> recentSearches = dbHelper.getRecentSearchHistory(10);
        for (String search : recentSearches) {
            if (search.length() > 1 && !recommendations.contains(search)) {
                recommendations.add(search);
                if (recommendations.size() >= 3) break;
            }
        }
        
        // 2. 从笔记中获取推荐
        if (recommendations.size() < 3) {
            List<String> poemsFromNotes = dbHelper.getPoemsFromNotes();
            for (String poem : poemsFromNotes) {
                if (poem.length() > 1 && !recommendations.contains(poem)) {
                    recommendations.add(poem);
                    if (recommendations.size() >= 3) break;
                }
            }
        }
        
        // 3. 如果还不够，使用默认热门诗词
        List<String> defaultPoems = new ArrayList<>();
        defaultPoems.add("静夜思");
        defaultPoems.add("春晓");
        defaultPoems.add("登鹳雀楼");
        defaultPoems.add("望庐山瀑布");
        defaultPoems.add("敕勒歌");
        defaultPoems.add("赋得古原草送别");
        
        for (String poem : defaultPoems) {
            if (!recommendations.contains(poem)) {
                recommendations.add(poem);
                if (recommendations.size() >= 3) break;
            }
        }
        
        // 更新推荐显示
        if (recommendations.size() > 0) tvRecommend1.setText(recommendations.get(0));
        if (recommendations.size() > 1) tvRecommend2.setText(recommendations.get(1));
        if (recommendations.size() > 2) tvRecommend3.setText(recommendations.get(2));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
