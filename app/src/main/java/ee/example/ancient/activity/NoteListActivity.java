package ee.example.ancient.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.util.ArrayList;
import java.util.List;

public class NoteListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private PlaceDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        database = new PlaceDatabase(this, "poetry.db", null, 1);
        
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

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
} 