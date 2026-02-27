package ee.example.ancient;

import android.app.Application;
//import com.jinrishici.sdk.android.JinrishiciFactory;  // 添加这一行

//功能：自定义应用程序类。
//主要功能：
//在应用启动时初始化数据库，确保数据库可用

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 在应用启动时初始化

        // 初始化数据库
        DBHelper dbHelper = new DBHelper(this);
        try {
            dbHelper.createDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 新增：初始化今日诗词SDK =====
        //JinrishiciFactory.init(this);
    }
}