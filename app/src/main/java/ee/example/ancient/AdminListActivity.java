package ee.example.ancient;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

//注意：此模块已经停止使用
//功能：管理员古诗列表界面。
//主要功能：
//显示古诗的列表，允许管理员查看和管理古诗。
//提供添加新古诗的功能。
//支持根据主题查询古诗。

//管理员古诗列表
public class AdminListActivity extends AppCompatActivity {

    private TextView txt_title;
    private FrameLayout fl_content;
    private Context mContext;
    private ArrayList<NewsBean> datas = null;
    private FragmentManager fManager = null;
    private long exitTime = 0;
    private SQLiteHelper mSQLiteHelper;
    private String theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        // 获取传入的主题
        theme = getIntent().getStringExtra("theme");

        mContext = AdminListActivity.this;
        fManager = getFragmentManager();
        bindViews();
        initData();
    }

    private void initData() {
        datas = new ArrayList<NewsBean>();
        mSQLiteHelper = new SQLiteHelper(this); //创建数据库
        if (datas != null) {
            datas.clear();
        }
        // 根据主题获取数据
        if (theme != null) {
            datas = mSQLiteHelper.queryByTheme(theme);
        } else {
            datas = mSQLiteHelper.query();
        }
        NewListFragment nlFragment = new NewListFragment(fManager, datas);
        FragmentTransaction ft = fManager.beginTransaction();
        ft.replace(R.id.fl_content, nlFragment);
        ft.commit();
    }


    private void bindViews() {
        txt_title = (TextView) findViewById(R.id.txt_title);
        fl_content = (FrameLayout) findViewById(R.id.fl_content);
        ImageView add = (ImageView) findViewById(R.id.add);
        if(Data.sta_np==true) {
            if (Data.sta_name.equals("admin")) {
                add.setVisibility(View.VISIBLE);
            }
        }
        //设置点击事件
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //页面跳转
                Intent intent = new Intent(AdminListActivity.this,
                        AddGuShiActivity.class);
                startActivityForResult(intent, 1);
            }
        });
    }


    //点击回退键的处理：判断Fragment栈中是否有Fragment
    //没，双击退出程序，否则像��Toast提示
    //有，popbackstack弹出栈
    @Override
    public void onBackPressed() {
        if (fManager.getBackStackEntryCount() == 0) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                super.onBackPressed();
            }
        } else {
            fManager.popBackStack();
            txt_title.setText("新闻列表");
        }
    }

    //页面回调监听刷新事件
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1&&resultCode==2){
            initData();
        }
    }
}