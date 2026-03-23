package ee.example.ancient.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import ee.example.ancient.Data;
import ee.example.ancient.PlaceDatabase;
import ee.example.ancient.R;

public class AddNoteActivity extends AppCompatActivity {
    private EditText etTitle;
    private EditText etContent;
    private PlaceDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        
        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "标题和内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        long result = database.addNote(
            Data.userId.intValue(),
            title,
            content,
            "",  // 空的诗词ID
            "",  // 空的诗词内容
            "",  // 空的诗词翻译
            "",  // 空的诗人信息
            ""   // 空的主题
        );

        if (result != -1) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        
        if (!title.isEmpty() || !content.isEmpty()) {
            // 有输入内容，显示保存提示
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("保存笔记")
                    .setMessage("笔记内容已输入，是否保存？")
                    .setPositiveButton("保存", (dialog, which) -> saveNote())
                    .setNegativeButton("不保存", (dialog, which) -> finish())
                    .setNeutralButton("取消", null)
                    .show();
        } else {
            // 无输入内容，直接返回
            super.onBackPressed();
        }
    }
} 
