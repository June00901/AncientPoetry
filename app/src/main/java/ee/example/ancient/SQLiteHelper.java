package ee.example.ancient;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

//功能：SQLite 数据库的帮助类。
//主要功能：
//管理数据库的创建和版本管理。
//提供对古诗和用户数据表的操作，包括创建、更新和删除数据。

//首页古诗 - 数据库帮助类
public class SQLiteHelper extends SQLiteOpenHelper {
    private SQLiteDatabase sqLiteDatabase;
    //创建数据库
    public SQLiteHelper(Context context){
        //上下文   数据库名称   null  数据库版本号
        super(context, DBUtils.DATABASE_NAME, null, DBUtils.DATABASE_VERION);
        //获取编辑数据库对象 sqLiteDatabase
        sqLiteDatabase = this.getWritableDatabase();
    }
    //创建表
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "+DBUtils.DATABASE_TABLE+"("+DBUtils.NOTEPAD_ID+
                " integer primary key autoincrement,"+ DBUtils.NOTEPAD_CONTENT +
                " text," + DBUtils.NOTEPAD_TIME +
                " text," + DBUtils.NOTEPAD_THEME +
                " text)");
        // 创建收藏表
        db.execSQL("CREATE TABLE IF NOT EXISTS collections (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id TEXT NOT NULL," +
                "poem_id TEXT," +
                "title TEXT NOT NULL," +
                "content TEXT," +
                "author TEXT)");
    }
    //数据更新
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+DBUtils.DATABASE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS collections");
        onCreate(db);
    }
    //添加数据
    public boolean insertData(String userContent, String userTime, String theme) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBUtils.NOTEPAD_CONTENT, userContent);
        contentValues.put(DBUtils.NOTEPAD_TIME, userTime);
        contentValues.put(DBUtils.NOTEPAD_THEME, theme);
        return sqLiteDatabase.insert(DBUtils.DATABASE_TABLE, null, contentValues) > 0;
    }
    //删除数据
    public boolean deleteData(String id){
        String sql=DBUtils.NOTEPAD_ID+"=?";//条件
        String[] contentValuesArray=new String[]{String.valueOf(id)};//条件值
        return sqLiteDatabase.delete(DBUtils.DATABASE_TABLE,sql,contentValuesArray)>0;
    }
    //修改数据
    public boolean updateData(String id, String content, String userYear, String theme) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBUtils.NOTEPAD_CONTENT, content);
        contentValues.put(DBUtils.NOTEPAD_TIME, userYear);
        contentValues.put(DBUtils.NOTEPAD_THEME, theme);
        String sql = DBUtils.NOTEPAD_ID + "=?";
        String[] strings = new String[]{id};
        return sqLiteDatabase.update(DBUtils.DATABASE_TABLE, contentValues, sql, strings) > 0;
    }
    //查询数据
    public ArrayList<NewsBean> query(){
        ArrayList<NewsBean> list=new ArrayList<NewsBean>();
        Cursor cursor=sqLiteDatabase.query(DBUtils.DATABASE_TABLE,null,null,null,
                null,null,DBUtils.NOTEPAD_ID+" desc");
        if (cursor!=null){
            while (cursor.moveToNext()){//移动到首行 循环遍历
                //创建空实体类对象
                NewsBean noteInfo=new NewsBean();
                //获取表字段每行对应的值
                @SuppressLint("Range") String id = String.valueOf(cursor.getInt
                        (cursor.getColumnIndex(DBUtils.NOTEPAD_ID)));
                @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex
                        (DBUtils.NOTEPAD_CONTENT));
                @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex
                        (DBUtils.NOTEPAD_TIME));
                //赋值
                noteInfo.setId(id);
                noteInfo.setContent(content);
                noteInfo.setTime(time);
                //添加数组
                list.add(noteInfo);
            }
            cursor.close();//关闭游标 防止内存泄漏
        }
        return list;
    }
    // 按主查询数据
    public ArrayList<NewsBean> queryByTheme(String theme) {
        ArrayList<NewsBean> list = new ArrayList<NewsBean>();
        Cursor cursor = sqLiteDatabase.query(DBUtils.DATABASE_TABLE, null,
                DBUtils.NOTEPAD_THEME + "=?", new String[]{theme},
                null, null, DBUtils.NOTEPAD_ID + " desc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                NewsBean noteInfo = new NewsBean();
                @SuppressLint("Range") String id = String.valueOf(cursor.getInt(
                        cursor.getColumnIndex(DBUtils.NOTEPAD_ID)));
                @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex(
                        DBUtils.NOTEPAD_CONTENT));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(
                        DBUtils.NOTEPAD_TIME));
                @SuppressLint("Range") String noteTheme = cursor.getString(cursor.getColumnIndex(
                        DBUtils.NOTEPAD_THEME));
                noteInfo.setId(id);
                noteInfo.setContent(content);
                noteInfo.setTitle(title);
                noteInfo.setTheme(noteTheme);
                list.add(noteInfo);
            }
            cursor.close();
        }
        return list;
    }
    // 删除收藏
    public boolean deleteCollection(String title, String userId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("collections", 
            "title=? AND user_id=?", 
            new String[]{title, userId}) > 0;
    }
    @SuppressLint("Range")
    public List<NewsBean> queryCollectionsByUserId() {
        List<NewsBean> collections = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        if (Data.userId == null) {
            return collections;
        }
        
        String currentUserId = String.valueOf(Data.userId);
        Cursor cursor = db.query("collections", 
            new String[]{"_id", "title", "content"}, 
            "user_id=?", 
            new String[]{currentUserId}, 
            null, null, null);
        
        while (cursor.moveToNext()) {
            NewsBean collection = new NewsBean();
            collection.setId(cursor.getString(cursor.getColumnIndex("_id")));
            collection.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            collection.setContent(cursor.getString(cursor.getColumnIndex("content")));
            collections.add(collection);
        }
        cursor.close();
        return collections;
    }
}
