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
        // 避免应用启动早期并发开库导致锁冲突：
        // 1) 不再初始化旧 DBHelper(ancient_poetry.db)
        // 2) 诗词大数据预热改为在主界面延迟启动

        // ===== 新增：初始化今日诗词SDK =====
        //JinrishiciFactory.init(this);
    }
}
