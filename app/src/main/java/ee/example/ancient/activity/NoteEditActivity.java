package ee.example.ancient.activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import ee.example.ancient.Data;
import ee.example.ancient.PlaceDatabase;
import ee.example.ancient.R;

public class NoteEditActivity extends AppCompatActivity {
    private EditText etTitle;
    private EditText etContent;
    private PlaceDatabase database;
    private int noteId;
    private String originalTitle;
    private String originalContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        
        // 获取传递过来的笔记数据
        noteId = getIntent().getIntExtra("noteId", -1);
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        
        // 保存原始内容
        originalTitle = title;
        originalContent = content;
        
        // 设置现有的笔记内容
        etTitle.setText(title);
        etContent.setText(content);
        
        findViewById(R.id.btnSave).setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "标题和内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 确保 userId 是有效的
        if (Data.userId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        int result = database.updateNote(Data.userId.intValue(), noteId, title, content);

        if (result > 0) {
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 检查内容是否被修改
    private boolean isContentModified() {
        String currentTitle = etTitle.getText().toString().trim();
        String currentContent = etContent.getText().toString().trim();
        return !currentTitle.equals(originalTitle) || !currentContent.equals(originalContent);
    }

    @Override
    public void onBackPressed() {
        if (isContentModified()) {
            // 内容被修改，显示保存提示
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("保存笔记")
                    .setMessage("笔记内容已修改，是否保存？")
                    .setPositiveButton("保存", (dialog, which) -> saveNote())
                    .setNegativeButton("不保存", (dialog, which) -> finish())
                    .setNeutralButton("取消", null)
                    .show();
        } else {
            // 内容未修改，直接返回
            super.onBackPressed();
        }
    }
} 
