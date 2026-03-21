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
} 
