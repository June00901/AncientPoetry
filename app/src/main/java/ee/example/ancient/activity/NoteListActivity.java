package ee.example.ancient.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ee.example.ancient.Data;
import ee.example.ancient.PlaceDatabase;
import ee.example.ancient.R;
import ee.example.ancient.adapter.NoteAdapter;
import ee.example.ancient.model.Note;
import ee.example.ancient.utils.PoemApiClient;
import java.util.ArrayList;
import java.util.List;

public class NoteListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private PlaceDatabase database;
    private PoemApiClient poemApiClient;
    private Handler mainHandler;
    private List<Note> studyPlanNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        poemApiClient = new PoemApiClient();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        // 新建笔记按钮
        findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            if (!Data.sta_np || Data.userId == null) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            // 确保正确启动添加笔记界面
            Intent intent = new Intent(NoteListActivity.this, AddNoteActivity.class);
            startActivity(intent);
        });
        
        // 应用学习计划按钮
        findViewById(R.id.btn_apply_plan).setOnClickListener(v -> showStudyPlanDialog());
        
        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new NoteAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((note, position) -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.putExtra("noteId", note.getId());
            intent.putExtra("title", note.getTitle());
            intent.putExtra("content", note.getContent());
            startActivity(intent);
        });

        adapter.setOnItemLongClickListener((note, position) -> {
            new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除这条笔记吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    deleteNote(note.getId());
                })
                .setNegativeButton("取消", null)
                .show();
            return true;
        });

        loadNotes();
    }

    private void loadNotes() {
        if (!Data.sta_np || Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        List<Note> notes = database.getUserNotes(Data.userId.intValue());
        Log.d("NoteListActivity", "Loading notes for user: " + Data.userId + ", found: " + (notes != null ? notes.size() : 0));
        
        if (notes != null && !notes.isEmpty()) {
            adapter.setNotes(notes);
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "还没有笔记，点击右上角添加", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteNote(int noteId) {
        if (!Data.sta_np || Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int result = database.deleteNote(Data.userId.intValue(), noteId);
        if (result > 0) {
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
            loadNotes();
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showStudyPlanDialog() {
        if (!Data.sta_np || Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 查找所有学习计划
        studyPlanNotes = database.getUserNotes(Data.userId.intValue());
        List<Note> plans = new ArrayList<>();
        for (Note note : studyPlanNotes) {
            if (note.getTitle() != null && note.getTitle().contains("学习计划")) {
                plans.add(note);
            }
        }

        if (plans.isEmpty()) {
            Toast.makeText(this, "还没有学习计划，请先在首页生成", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建对话框选择学习计划
        String[] planTitles = new String[plans.size()];
        for (int i = 0; i < plans.size(); i++) {
            planTitles[i] = plans.get(i).getTitle();
        }

        new AlertDialog.Builder(this)
            .setTitle("选择学习计划")
            .setItems(planTitles, (dialog, which) -> {
                showProgressDialog(plans.get(which));
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showProgressDialog(Note studyPlan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("应用学习计划");
        
        // 创建输入框
        EditText etProgress = new EditText(this);
        etProgress.setHint("例如：学习第3天");
        etProgress.setPadding(50, 30, 50, 30);
        
        builder.setView(etProgress);
        builder.setPositiveButton("确定", (dialog, which) -> {
            String progress = etProgress.getText().toString().trim();
            if (progress.isEmpty()) {
                Toast.makeText(this, "请输入学习进度", Toast.LENGTH_SHORT).show();
                return;
            }
            generateTodayPlan(studyPlan, progress);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void generateTodayPlan(Note studyPlan, String progress) {
        // 显示加载提示
        Toast.makeText(this, "AI正在生成今日学习计划...", Toast.LENGTH_SHORT).show();

        String prompt = "你是一位专业的古诗词学习规划师，根据以下信息生成今日的简单学习执行方案。\n\n"
                + "【学习计划内容】\n"
                + studyPlan.getContent() + "\n\n"
                + "【当前学习进度】\n"
                + progress + "\n\n"
                + "【输出要求】\n"
                + "1. 生成今日的具体学习任务\n"
                + "2. 任务要简单明确、切实可行\n"
                + "3. 控制在200字以内\n"
                + "4. 使用简洁的列表格式\n"
                + "5. 不要包含冗长的解释\n\n"
                + "【输出格式】\n\n"
                + "## 今日学习任务\n"
                + "- 任务1\n"
                + "- 任务2\n"
                + "- 任务3";

        poemApiClient.callApiWithPrompt(prompt, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String result) {
                // 保存今日学习计划到SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("today_plan", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("plan_content", result);
                editor.putLong("plan_time", System.currentTimeMillis());
                editor.apply();
                
                mainHandler.post(() -> showTodayPlanPopup(result));
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(NoteListActivity.this, "生成失败：" + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showTodayPlanPopup(String plan) {
        new AlertDialog.Builder(this)
            .setTitle("📅 今日学习计划")
            .setMessage(plan)
            .setPositiveButton("确定", null)
            .setCancelable(true)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
} 
