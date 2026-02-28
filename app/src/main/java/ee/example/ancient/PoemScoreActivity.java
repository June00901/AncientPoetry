package ee.example.ancient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import ee.example.ancient.utils.PoemApiClient;

public class PoemScoreActivity extends AppCompatActivity {

    private static final String[] POEM_TYPES = {
            "五言绝句", "七言绝句", "五言律诗", "七言律诗", "词牌", "现代诗", "古体诗"
    };

    // 体裁权重（手动设定，确保稳定性）
    private static final Map<String, double[]> STABILITY_WEIGHTS = new HashMap<>();
    static {
        STABILITY_WEIGHTS.put("五言绝句", new double[]{0.25, 0.25, 0.20, 0.20, 0.10});
        STABILITY_WEIGHTS.put("七言绝句", new double[]{0.25, 0.25, 0.20, 0.20, 0.10});
        STABILITY_WEIGHTS.put("五言律诗", new double[]{0.30, 0.25, 0.20, 0.15, 0.10});
        STABILITY_WEIGHTS.put("七言律诗", new double[]{0.30, 0.25, 0.20, 0.15, 0.10});
        STABILITY_WEIGHTS.put("词牌", new double[]{0.20, 0.30, 0.20, 0.20, 0.10});
        STABILITY_WEIGHTS.put("现代诗", new double[]{0.10, 0.30, 0.25, 0.25, 0.10});
        STABILITY_WEIGHTS.put("古体诗", new double[]{0.15, 0.30, 0.25, 0.20, 0.10});
    }

    // 测试用例
    private static final String[][] TEST_CASES = {
            {"五言绝句", "春日", "春风拂柳绿，细雨润花红。独坐亭台晚，诗心入梦中。"},
            {"七言绝句", "秋思", "银烛秋光冷画屏，轻罗小扇扑流萤。天阶夜色凉如水，卧看牵牛织女星。"},
            {"五言律诗", "山居", "空山新雨后，天气晚来秋。明月松间照，清泉石上流。"}
    };

    private Spinner spinnerType;
    private EditText inputBackground;
    private EditText inputPoem;
    private Button btnScore;
    private Button btnLoadTest;
    private ProgressBar progressBar;
    private LinearLayout layoutResult;
    private TextView textTotalScore, textLevel, textScoreDetail, textComment, textSuggestion;

    private PoemApiClient apiClient;
    private Handler mainHandler;
    private String currentPoemHash = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poem_score);

        apiClient = new PoemApiClient();
        mainHandler = new Handler(Looper.getMainLooper());
        initViews();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        spinnerType = findViewById(R.id.spinner_poem_type);
        inputBackground = findViewById(R.id.input_background);
        inputPoem = findViewById(R.id.input_original_poem);
        btnScore = findViewById(R.id.btn_start_score);
        btnLoadTest = findViewById(R.id.btn_load_test);
        progressBar = findViewById(R.id.progress_bar);
        layoutResult = findViewById(R.id.layout_result);
        textTotalScore = findViewById(R.id.text_total_score);
        textLevel = findViewById(R.id.text_level);
        textScoreDetail = findViewById(R.id.text_score_detail);
        textComment = findViewById(R.id.text_comment);
        textSuggestion = findViewById(R.id.text_suggestion);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, POEM_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
    }

    private void setupListeners() {
        btnScore.setOnClickListener(v -> startScoring());
        btnLoadTest.setOnClickListener(v -> loadRandomTestCase());
    }

    private void startScoring() {
        String type = spinnerType.getSelectedItem().toString();
        String background = inputBackground.getText().toString().trim();
        String poem = inputPoem.getText().toString().trim();

        if (poem.isEmpty()) {
            Toast.makeText(this, "请输入诗词内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算哈希（用于稳定性）
        currentPoemHash = calculateHash(poem + type);

        showLoading(true);
        layoutResult.setVisibility(View.GONE);

        apiClient.scorePoem(type, background, poem, currentPoemHash, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    parseAndDisplayResult(result, type);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PoemScoreActivity.this, "评分失败：" + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void parseAndDisplayResult(String jsonStr, String poemType) {
        try {
            // 提取JSON
            int start = jsonStr.indexOf('{');
            int end = jsonStr.lastIndexOf('}');
            if (start != -1 && end != -1) {
                jsonStr = jsonStr.substring(start, end + 1);
            }
            JSONObject json = new JSONObject(jsonStr);

            int totalScore = json.getInt("total_score");
            String level = json.getString("level");
            JSONArray dimensions = json.getJSONArray("dimensions");

            // 应用权重调整
            double[] weights = STABILITY_WEIGHTS.getOrDefault(poemType, STABILITY_WEIGHTS.get("古体诗"));
            int meterScore = 0, imageryScore = 0, wordScore = 0, emotionScore = 0, creativeScore = 0;

            for (int i = 0; i < dimensions.length(); i++) {
                JSONObject dim = dimensions.getJSONObject(i);
                String name = dim.getString("name");
                int score = dim.getInt("score");
                if (name.contains("格律")) meterScore = score;
                else if (name.contains("意境")) imageryScore = score;
                else if (name.contains("用词")) wordScore = score;
                else if (name.contains("情感")) emotionScore = score;
                else if (name.contains("创新")) creativeScore = score;
            }

            // 加权计算
            double weightedTotal = meterScore * weights[0] + imageryScore * weights[1] +
                    wordScore * weights[2] + emotionScore * weights[3] + creativeScore * weights[4];
            totalScore = Math.min(100, Math.max(0, (int) Math.round(weightedTotal)));
            level = getLevel(totalScore);

            // 显示
            textTotalScore.setText(String.valueOf(totalScore));
            textLevel.setText(level);
            textLevel.setBackgroundColor(getLevelColor(totalScore));
            textScoreDetail.setText(String.format("格律：%d/20  意境：%d/20  用词：%d/20  情感：%d/20  创新：%d/20",
                    meterScore, imageryScore, wordScore, emotionScore, creativeScore));
            textComment.setText(json.optString("overall_comment", "暂无评语"));
            textSuggestion.setText(json.optString("suggestions", "暂无建议"));

            layoutResult.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "解析结果失败", Toast.LENGTH_SHORT).show();
        }
    }

    private String getLevel(int score) {
        if (score >= 90) return "传世之作";
        if (score >= 85) return "上乘佳作";
        if (score >= 80) return "优秀";
        if (score >= 70) return "良好";
        if (score >= 60) return "合格";
        return "待改进";
    }

    private int getLevelColor(int score) {
        if (score >= 85) return 0xFFE74C3C;
        if (score >= 70) return 0xFF3498DB;
        if (score >= 60) return 0xFF2ECC71;
        return 0xFF95A5A6;
    }

    private String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnScore.setEnabled(!show);
    }

    private void loadRandomTestCase() {
        int index = (int)(Math.random() * TEST_CASES.length);
        String[] testCase = TEST_CASES[index];

        for (int i = 0; i < POEM_TYPES.length; i++) {
            if (POEM_TYPES[i].equals(testCase[0])) {
                spinnerType.setSelection(i);
                break;
            }
        }
        inputBackground.setText("测试用例：" + testCase[1]);
        inputPoem.setText(testCase[2]);
        Toast.makeText(this, "已加载测试用例", Toast.LENGTH_SHORT).show();
    }
}