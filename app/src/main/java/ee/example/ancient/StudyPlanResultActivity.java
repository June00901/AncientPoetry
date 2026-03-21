package ee.example.ancient;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import ee.example.ancient.Data;
import ee.example.ancient.PlaceDatabase;

public class StudyPlanResultActivity extends AppCompatActivity {

    private TextView tvExpectedTime;
    private TextView tvStudyPlanContent;
    private ImageButton btnBack;
    private ImageButton btnShare;
    private CardView btnSavePlan;

    private String studyPlanContent;
    private String expectedTime;
    private PlaceDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan_result);

        database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        initViews();
        loadIntentData();
        displayContent();
        // 自动保存到学习笔记
        autoSaveToNotes();
    }

    private void initViews() {
        tvExpectedTime = findViewById(R.id.tv_expected_time);
        tvStudyPlanContent = findViewById(R.id.tv_study_plan_content);
        btnBack = findViewById(R.id.btn_back);
        btnShare = findViewById(R.id.btn_share);
        btnSavePlan = findViewById(R.id.btn_save_plan);

        btnBack.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> sharePlan());

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            studyPlanContent = intent.getStringExtra("study_plan_content");
            expectedTime = intent.getStringExtra("expected_time");
        }

        if (studyPlanContent == null) {
            studyPlanContent = "暂无学习计划内容";
        }
        if (expectedTime == null) {
            expectedTime = "3个月（约90天）";
        }
    }

    private void displayContent() {
        tvExpectedTime.setText(expectedTime);

        // 将Markdown风格的文本转换为HTML显示
        String formattedText = formatMarkdownToHtml(studyPlanContent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvStudyPlanContent.setText(Html.fromHtml(formattedText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvStudyPlanContent.setText(Html.fromHtml(formattedText));
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

    private void sharePlan() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "我的古诗词学习计划");
        shareIntent.putExtra(Intent.EXTRA_TEXT, studyPlanContent);
        startActivity(Intent.createChooser(shareIntent, "分享学习计划"));
    }

    private void savePlan() {
        autoSaveToNotes();
    }

    private void autoSaveToNotes() {
        if (Data.userId == null) {
            // 未登录，不自动保存
            return;
        }

        // 生成笔记标题
        String title = "学习计划 - " + expectedTime;
        
        // 检查是否已经存在相同标题的笔记
        if (database.isNoteExists(Data.userId.intValue(), title)) {
            // 已经存在，不重复保存
            return;
        }
        
        // 保存到数据库
        long result = database.addNote(
            Data.userId.intValue(),
            title,
            studyPlanContent,
            "",  // 空的诗词ID
            "",  // 空的诗词内容
            "",  // 空的诗词翻译
            "",  // 空的诗人信息
            "学习计划"   // 主题
        );

        if (result != -1) {
            // 保存成功，显示提示
            Toast.makeText(this, "学习计划已保存到学习笔记与计划", Toast.LENGTH_SHORT).show();
        } else {
            // 保存失败，显示提示
            Toast.makeText(this, "保存失败，请稍后重试", Toast.LENGTH_SHORT).show();
        }
    }
}
