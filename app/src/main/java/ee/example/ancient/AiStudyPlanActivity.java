package ee.example.ancient;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import ee.example.ancient.utils.PoemApiClient;

public class AiStudyPlanActivity extends AppCompatActivity {

    private EditText etLearningNeeds;
    private EditText etLearningTime;
    private EditText etLearningGoal;
    private CardView btnGeneratePlan;
    private TextView tvGenerateBtn;
    private CardView btnLoadTestCase;
    private CardView cardResult;
    private TextView tvStudyPlanResult;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private PoemApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_study_plan);

        initViews();
        apiClient = new PoemApiClient();
    }

    private void initViews() {
        etLearningNeeds = findViewById(R.id.et_learning_needs);
        etLearningTime = findViewById(R.id.et_learning_time);
        etLearningGoal = findViewById(R.id.et_learning_goal);
        btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        tvGenerateBtn = findViewById(R.id.tv_generate_btn);
        btnLoadTestCase = findViewById(R.id.btn_load_test_case);
        cardResult = findViewById(R.id.card_result);
        tvStudyPlanResult = findViewById(R.id.tv_study_plan_result);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        btnGeneratePlan.setOnClickListener(v -> generateStudyPlan());

        btnLoadTestCase.setOnClickListener(v -> loadTestCase());
    }

    private void generateStudyPlan() {
        String needs = etLearningNeeds.getText().toString().trim();
        String time = etLearningTime.getText().toString().trim();
        String goal = etLearningGoal.getText().toString().trim();

        if (needs.isEmpty() && time.isEmpty() && goal.isEmpty()) {
            Toast.makeText(this, "请至少填写一项信息，让AI更好地为你制定计划", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        cardResult.setVisibility(View.GONE);

        String prompt = buildStudyPlanPrompt(needs, time, goal);

        apiClient.callApiWithPrompt(prompt, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    // 跳转到结果页面
                    navigateToResultPage(result, time);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(AiStudyPlanActivity.this, "生成失败：" + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void navigateToResultPage(String planContent, String timeInfo) {
        Intent intent = new Intent(this, StudyPlanResultActivity.class);
        intent.putExtra("study_plan_content", planContent);
        
        // 提取预期时间信息
        String expectedTime = extractExpectedTime(timeInfo);
        intent.putExtra("expected_time", expectedTime);
        
        startActivity(intent);
    }

    private String extractExpectedTime(String timeInfo) {
        if (timeInfo == null || timeInfo.isEmpty()) {
            return "3个月（约90天）";
        }
        
        // 尝试从时间信息中提取数字和单位
        if (timeInfo.contains("个月")) {
            int start = timeInfo.indexOf("个月");
            int numStart = start - 1;
            while (numStart >= 0 && Character.isDigit(timeInfo.charAt(numStart))) {
                numStart--;
            }
            if (numStart < start - 1) {
                String months = timeInfo.substring(numStart + 1, start);
                try {
                    int monthCount = Integer.parseInt(months);
                    int days = monthCount * 30;
                    return monthCount + "个月（约" + days + "天）";
                } catch (NumberFormatException e) {
                    // 解析失败，使用默认值
                }
            }
        }
        
        return "3个月（约90天）";
    }

    private String buildStudyPlanPrompt(String needs, String time, String goal) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的古诗词学习规划师，请根据以下信息为用户制定一个低压、实用、量身定制的古诗词学习计划。\n\n");

        if (!needs.isEmpty()) {
            prompt.append("【学习需求】\n").append(needs).append("\n\n");
        }
        if (!time.isEmpty()) {
            prompt.append("【时间安排】\n").append(time).append("\n\n");
        }
        if (!goal.isEmpty()) {
            prompt.append("【期待效果】\n").append(goal).append("\n\n");
        }

        prompt.append("【输出要求】\n");
        prompt.append("请按照以下格式输出学习计划，使用Markdown格式让内容清晰美观：\n\n");

        prompt.append("## 🎯 学习目标\n");
        prompt.append("- 明确2-3个具体、可衡量的学习目标\n\n");

        prompt.append("## 📅 学习周期\n");
        prompt.append("- 建议的学习周期和阶段划分\n\n");

        prompt.append("## 📖 每周学习计划\n");
        prompt.append("### 第1周\n");
        prompt.append("- 具体学习内容\n");
        prompt.append("- 推荐诗词列表\n");
        prompt.append("- 每日任务安排\n\n");

        prompt.append("### 第2周\n");
        prompt.append("- ...\n\n");

        prompt.append("## 📝 学习方法建议\n");
        prompt.append("- 背诵技巧\n");
        prompt.append("- 理解方法\n");
        prompt.append("- 复习策略\n\n");

        prompt.append("## ✅ 进度检查点\n");
        prompt.append("- 每周/每月的自我检测方式\n\n");

        prompt.append("## 💡 使用App的建议\n");
        prompt.append("- 如何利用本App的功能辅助学习\n");
        prompt.append("- 推荐使用的功能模块\n\n");

        prompt.append("## 🌟 激励小贴士\n");
        prompt.append("- 保持学习动力的方法\n\n");

        prompt.append("【注意事项】\n");
        prompt.append("1. 计划要切实可行，不要给用户太大压力\n");
        prompt.append("2. 内容要有条理，使用表情符号增加可读性\n");
        prompt.append("3. 推荐的诗词要经典且适合用户水平\n");
        prompt.append("4. 强调循序渐进，享受学习过程\n");
        prompt.append("5. 控制内容长度，不要过长，保持在1000字以内\n");
        prompt.append("6. 重点突出，条理清晰，避免冗长描述\n");

        return prompt.toString();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            tvGenerateBtn.setText("AI正在思考中...（预计等待10-15秒）");
            btnGeneratePlan.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            tvGenerateBtn.setText("🚀 生成学习计划");
            btnGeneratePlan.setEnabled(true);
        }
    }

    private void displayResult(String result) {
        cardResult.setVisibility(View.VISIBLE);

        // 将Markdown风格的文本转换为HTML显示
        String formattedText = formatMarkdownToHtml(result);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvStudyPlanResult.setText(Html.fromHtml(formattedText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvStudyPlanResult.setText(Html.fromHtml(formattedText));
        }
    }

    private String formatMarkdownToHtml(String markdown) {
        String html = markdown;

        // 处理标题
        html = html.replaceAll("## (.*?)$", "<br/><b><font color='#1976D2'>$1</font></b><br/>");
        html = html.replaceAll("### (.*?)$", "<br/><b><font color='#388E3C'>$1</font></b><br/>");

        // 处理列表项
        html = html.replaceAll("^- (.*?)$", "<br/>• $1");
        html = html.replaceAll("^\\* (.*?)$", "<br/>• $1");

        // 处理换行
        html = html.replaceAll("\n\n", "<br/><br/>");
        html = html.replaceAll("\n", "<br/>");

        // 处理加粗
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

        return html;
    }

    private void loadTestCase() {
        etLearningNeeds.setText("我想深入学习苏轼的诗词，重点了解他的豪放派风格，包括《念奴娇·赤壁怀古》、《水调歌头·明月几时有》等经典作品，希望能理解他的豁达人生观和文学成就。");
        etLearningTime.setText("每天学习30分钟，每周学习5天，希望在3个月内能够背诵20首苏轼的代表作，并理解其中的意境和哲理。");
        etLearningGoal.setText("以后写作文时能够引用苏轼的诗句，提升文章的文学素养和深度，同时培养自己的文学鉴赏能力，能够在日常交流中自然地运用诗词。");
        
        Toast.makeText(this, "测试用例已加载", Toast.LENGTH_SHORT).show();
    }
}
