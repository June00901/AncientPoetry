package ee.example.ancient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
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
    private TextView textMeterScore, textImageryScore, textWordScore, textEmotionScore, textCreativeScore;

    // 新增进度条变量
    private ProgressBar progressMeter, progressImagery, progressWord, progressEmotion, progressCreative;

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

        // 分项分数TextView
        textMeterScore = findViewById(R.id.text_meter_score);
        textImageryScore = findViewById(R.id.text_imagery_score);
        textWordScore = findViewById(R.id.text_word_score);
        textEmotionScore = findViewById(R.id.text_emotion_score);
        textCreativeScore = findViewById(R.id.text_creative_score);

        // 初始化进度条
        progressMeter = findViewById(R.id.progress_meter);
        progressImagery = findViewById(R.id.progress_imagery);
        progressWord = findViewById(R.id.progress_word);
        progressEmotion = findViewById(R.id.progress_emotion);
        progressCreative = findViewById(R.id.progress_creative);
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
                    setDefaultResult();
                });
            }

            @Override
            public void onStream(String partialContent) {
                // 实时更新可以在这里实现，但由于是解析JSON结果，暂时不需要
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

            // 新增：获取格式检查结果
            boolean isValid = json.optBoolean("valid", true);
            String formatMessage = json.optString("format_message", "");

            // 如果格式有问题，显示提示（但不中断流程）
            if (!isValid) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "格式提示：" + formatMessage, Toast.LENGTH_LONG).show();
                });
            }

            // 获取总分（如果没有则用维度计算）
            int totalScore = json.optInt("total_score", 0);
            JSONArray dimensions = json.optJSONArray("dimensions");

            // 初始化维度分数（20分制）
            int meterScore = 0, imageryScore = 0, wordScore = 0, emotionScore = 0, creativeScore = 0;

            if (dimensions != null) {
                for (int i = 0; i < dimensions.length(); i++) {
                    JSONObject dim = dimensions.getJSONObject(i);
                    String name = dim.optString("name", "");
                    int score = dim.optInt("score", 0);
                    if (name.contains("格律")) meterScore = score;
                    else if (name.contains("意境")) imageryScore = score;
                    else if (name.contains("用词")) wordScore = score;
                    else if (name.contains("情感")) emotionScore = score;
                    else if (name.contains("创新")) creativeScore = score;
                }
            } else {
                JSONObject dims = json.optJSONObject("dimensions");
                if (dims != null) {
                    if (dims.has("meter")) meterScore = dims.getJSONObject("meter").optInt("score", 0);
                    if (dims.has("imagery")) imageryScore = dims.getJSONObject("imagery").optInt("score", 0);
                    if (dims.has("wording")) wordScore = dims.getJSONObject("wording").optInt("score", 0);
                    if (dims.has("emotion")) emotionScore = dims.getJSONObject("emotion").optInt("score", 0);
                    if (dims.has("creative")) creativeScore = dims.getJSONObject("creative").optInt("score", 0);
                }
            }

            // 计算加权总分（20分制）
            double[] weights = STABILITY_WEIGHTS.getOrDefault(poemType, STABILITY_WEIGHTS.get("古体诗"));
            double weightedTotal = meterScore * weights[0] + imageryScore * weights[1] +
                    wordScore * weights[2] + emotionScore * weights[3] + creativeScore * weights[4];

            // 转百分制
            int calculatedTotal = (int)Math.round(weightedTotal * 5);

            // 如果AI返回的总分存在且合理，则使用AI总分，否则用计算值
            if (totalScore > 0 && totalScore <= 100) {
                // 已经是百分制，直接使用
            } else {
                totalScore = calculatedTotal;
            }

            // 如果格式有问题，可以适当降低总分
            if (!isValid && totalScore > 60) {
                totalScore = Math.min(60, totalScore - 10);
            }

            // 应用稳定性校验
            totalScore = applyStabilityCheck(totalScore, poemType);
            totalScore = Math.min(100, Math.max(0, totalScore));

            // 更新等级
            String level = calculateLevel(totalScore);

            // 显示总分
            textTotalScore.setText(String.valueOf(totalScore));
            textLevel.setText(level);
            textLevel.setBackgroundColor(getLevelColor(totalScore));

            // 将20分制维度分数转换为百分制显示
            int meterPercent = meterScore * 5;
            int imageryPercent = imageryScore * 5;
            int wordPercent = wordScore * 5;
            int emotionPercent = emotionScore * 5;
            int creativePercent = creativeScore * 5;

            // 显示详细分数（百分制）
            textScoreDetail.setText(String.format("格律：%d/100  意境：%d/100  用词：%d/100  情感：%d/100  创新：%d/100",
                    meterPercent, imageryPercent, wordPercent, emotionPercent, creativePercent));

            // 更新分项评析的分数显示（百分制）
            textMeterScore.setText(meterPercent + "/100");
            textImageryScore.setText(imageryPercent + "/100");
            textWordScore.setText(wordPercent + "/100");
            textEmotionScore.setText(emotionPercent + "/100");
            textCreativeScore.setText(creativePercent + "/100");

            // 设置进度条进度
            progressMeter.setProgress(meterPercent);
            progressImagery.setProgress(imageryPercent);
            progressWord.setProgress(wordPercent);
            progressEmotion.setProgress(emotionPercent);
            progressCreative.setProgress(creativePercent);

            // 评语和建议（如果格式有问题，可以在评语中追加提示）
            String comment = json.optString("overall_comment", "暂无评语");
            String suggestion = json.optString("suggestions", "暂无建议");

            if (!isValid) {
                comment = "【格式提示】" + formatMessage + "\n\n" + comment;
            }

            textComment.setText(comment);
            textSuggestion.setText(suggestion);

            layoutResult.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "解析结果失败，使用默认评分", Toast.LENGTH_SHORT).show();
            setDefaultResult();
        }
    }

    private void setDefaultResult() {
        int defaultScore = 75;
        textTotalScore.setText(String.valueOf(defaultScore));
        textLevel.setText(calculateLevel(defaultScore));
        textLevel.setBackgroundColor(getLevelColor(defaultScore));
        textScoreDetail.setText("格律：75/100  意境：75/100  用词：75/100  情感：75/100  创新：75/100");

        // 分项分数也设为默认值
        textMeterScore.setText("75/100");
        textImageryScore.setText("75/100");
        textWordScore.setText("75/100");
        textEmotionScore.setText("75/100");
        textCreativeScore.setText("75/100");

        // 默认进度条进度
        progressMeter.setProgress(75);
        progressImagery.setProgress(75);
        progressWord.setProgress(75);
        progressEmotion.setProgress(75);
        progressCreative.setProgress(75);

        textComment.setText("AI评析暂时无法获取，请重试。");
        textSuggestion.setText("检查网络或稍后重试。");
        layoutResult.setVisibility(View.VISIBLE);
    }

    private int applyStabilityCheck(int rawScore, String poemType) {
        int hashOffset = (currentPoemHash.hashCode() % 5) - 2;
        int stableScore = rawScore + hashOffset;
        return Math.min(100, Math.max(0, stableScore));
    }

    private String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private String calculateLevel(int score) {
        if (score >= 95) return "传世之作";
        if (score >= 90) return "上乘佳作";
        if (score >= 85) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 70) return "合格";
        if (score >= 60) return "尚可";
        return "待改进";
    }

    private int getLevelColor(int score) {
        if (score >= 90) return 0xFFE74C3C;
        if (score >= 80) return 0xFF3498DB;
        if (score >= 70) return 0xFF2ECC71;
        if (score >= 60) return 0xFFF39C12;
        return 0xFF95A5A6;
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