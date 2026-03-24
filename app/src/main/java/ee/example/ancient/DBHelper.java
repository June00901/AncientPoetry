package ee.example.ancient;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

//功能：另一个SQLite数据库的帮助类。
//主要功能：
//负责创建和管理古诗相关的数据库。
//提供对古诗表的操作，包括创建、查询和删除古诗数据。

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ancient_poetry.db";
    private static final int DATABASE_VERSION = 3;  // 升级版本号，触发onUpgrade
    private Context context;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建笔记表（原有的）
        db.execSQL("CREATE TABLE IF NOT EXISTS note (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "content TEXT," +
                "translation TEXT," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // 新增：创建诗词表
        db.execSQL("CREATE TABLE IF NOT EXISTS poems (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "author TEXT," +
                "content TEXT," +
                "dynasty TEXT)");

        // 新增：创建搜索历史表
        db.execSQL("CREATE TABLE IF NOT EXISTS search_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "query TEXT," +
                "search_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 如果旧版本是1，新版本是2，需要添加诗词表
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS poems (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "author TEXT," +
                    "content TEXT," +
                    "dynasty TEXT)");
        }
        
        // 如果旧版本小于3，添加搜索历史表
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS search_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "query TEXT," +
                    "search_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        }
    }

    // 确保数据库文件存在并可访问
    public void createDatabase() {
        boolean dbExists = checkDatabase();
        if (!dbExists) {
            this.getReadableDatabase();
            try {
                copyDatabase();
            } catch (IOException e) {
                throw new RuntimeException("Error copying database", e);
            }
        }
    }

    private boolean checkDatabase() {
        SQLiteDatabase checkDB = null;
        try {
            String dbPath = context.getDatabasePath(DATABASE_NAME).getPath();
            checkDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            // 数据库不存在
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null;
    }

    private void copyDatabase() throws IOException {
        InputStream myInput = context.getAssets().open(DATABASE_NAME);
        String outFileName = context.getDatabasePath(DATABASE_NAME).getPath();
        OutputStream myOutput = new FileOutputStream(outFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    // 搜索历史相关方法
    public void addSearchHistory(String query) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO search_history (query) VALUES (?)", new Object[]{query});
        db.close();
    }

    public List<String> getRecentSearchHistory(int limit) {
        List<String> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT query FROM search_history ORDER BY search_time DESC LIMIT ?", new String[]{String.valueOf(limit)});
        if (cursor.moveToFirst()) {
            do {
                history.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return history;
    }

    // 从笔记中提取可能的诗词名称
    public List<String> getPoemsFromNotes() {
        List<String> poems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, content FROM note", null);
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(0);
                String content = cursor.getString(1);
                // 简单提取可能的诗词名称（这里需要更复杂的逻辑，暂时简化）
                if (title != null && title.length() > 0) {
                    poems.add(title);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return poems;
    }
}