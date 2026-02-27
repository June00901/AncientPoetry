package ee.example.ancient;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

//功能：SQLite数据库的帮助类。
//主要功能：
//管理数据库的创建和版本管理。
//提供对用户表的操作，包括创建、更新和删除用户数据。

//用户-数据库
public  class DataBaseHelper extends SQLiteOpenHelper {
    private static DataBaseHelper instance;
    private static final String DATABASE_NAME = "travel.db";
    private static final int DATABASE_VERSION = 2;

    public DataBaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建users表
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +  // 注意这里是id而不是_id
            "name TEXT NOT NULL," +
            "password TEXT NOT NULL)");
            
        // 添加默认管理员账号
        db.execSQL("INSERT OR IGNORE INTO users (name, password) VALUES ('admin', '123456')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public static DataBaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DataBaseHelper(context);
        }
        return instance;
    }
}