package ee.example.ancient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTabHost;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import android.view.WindowManager;
import android.content.SharedPreferences;
import android.content.Intent;

//- 功能：实现应用的主界面。
//        - 主要功能：
//        - 设置和管理底部导航栏（TabHost），允许用户在不同的Fragment之间切换。
//        - 初始化各个Tab及其对应的Fragment。
//        - 处理状态栏的样式设置。

//导航栏页面
public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";

    private Bundle mBundle = new Bundle();
    private FragmentTabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        
        // 检查登录状态，如果未登录则直接跳转到登录界面
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        boolean isLoggedIn = sp.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        Data.initView();
        Bundle mBundle = new Bundle();
        mBundle.putString("tag", TAG);

        //初始化tabHost
        mTabHost = (FragmentTabHost) findViewById(R.id.tabhost);
        //将tabHost和FrameLayout关联
        mTabHost.setup(getApplicationContext(), getSupportFragmentManager(), R.id.tabcontent_page);

        //添加tab和其对应的fragment
        //addTab(标签，跳转的Fragment，传递参数的Bundle)
        mTabHost.addTab(
                getTabView(R.string.tab_home, R.drawable.shouye_2),
                TabFragment_Home.class,
                mBundle);
        mTabHost.addTab(
                getTabView(R.string.tab_find, R.drawable.faxian),

                TabFragment_Find.class,
                mBundle);

        mTabHost.addTab(
                getTabView(R.string.tab_community, R.drawable.shequ),
                TabFragment_Community.class,
                mBundle);
        mTabHost.addTab(
                getTabView(R.string.tab_me, R.drawable.wode_1),
                TabFragment_Me.class,
                mBundle);
        mTabHost.addTab(getTabView(R.string.tab_ai_helper, R.drawable.ai_helper), TabFragment_AiHelper.class, mBundle);
        //设置tabs之间的分隔线不显示
        mTabHost.getTabWidget().setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

        getApplicationContext().deleteDatabase("your_database_name.db");
    }
    private TabHost.TabSpec getTabView(int textId, int imgId) {
        String text = getResources().getString(textId);
        Drawable drawable = getResources().getDrawable(imgId);
        //必须设置图片大小，否则不显示
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        View view_tabitem = getLayoutInflater().inflate(R.layout.item_tabbar, null);
        TextView tv_item = (TextView) view_tabitem.findViewById(R.id.tv_item_tabbar);
        tv_item.setText(text);
        tv_item.setCompoundDrawables(null, drawable, null, null);
        TabHost.TabSpec spec = mTabHost.newTabSpec(text).setIndicator(view_tabitem);
        return spec;
    }
   @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBar(){
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//注意要清除 FLAG_TRANSLUCENT_STATUS flag
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.xml_color));//设置要显示的颜色（Color.TRANSPARENT为透明）
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    private void checkLoginStatus() {
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        boolean isLoggedIn = sp.getBoolean("isLoggedIn", false);
        
        if (!isLoggedIn) {
            // 未登录状态，清除所有数据
            Data.sta_np = false;
            Data.sta_name = null;
            Data.userId = null;
            
            // 跳转到登录界面
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // 结束当前Activity
            return;
        }
        
        // 已登录状态，更新数据
        Data.sta_np = true;
        Data.sta_name = sp.getString("username", null);
        Data.userId = Long.valueOf(sp.getInt("userId", -1));
    }
}
