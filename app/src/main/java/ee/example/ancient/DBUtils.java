package ee.example.ancient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//功能：数据库工具类。
//主要功能：
//定义数据库的常量（如数据库名、表名等）。
//提供获取当前时间的工具方法，可能用于记录数据的创建时间。

//古诗- 数据库
public class DBUtils {
    public static final String DATABASE_NAME = "Notepad";//数据库名
    public static final String DATABASE_TABLE = "Note";  //表名
    public static final int DATABASE_VERION = 1;          //数据库版本
    //数据库表中的列名
    public static final String NOTEPAD_ID = "id";//主键
    public static final String NOTEPAD_CONTENT = "content";//内容
    public static final String NOTEPAD_TIME = "title";//标题
    public static final String NOTEPAD_THEME = "theme";  // 添加主题字段常量
    
    // 获取当前时间
    public static String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return format.format(date);
    }
}
