package ee.example.ancient;

import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.Toast;

import com.youth.banner.BannerConfig;
import com.youth.banner.listener.OnBannerListener;

import com.youth.banner.Banner;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.ArrayList;

import static ee.example.ancient.Data.initView;

//功能：实现首页的 Fragment，主要用于展示轮播图和导航功能。
//主要功能：
//使用 Banner 组件展示轮播图，支持点击事件。
//提供多个按钮，允许用户根据主题跳转到不同的主题列表页面。
//初始化数据库和轮播图的设置。

//首页
public class TabFragment_Home extends Fragment {
    private static final String TAG = "TabFirstFragment";
    protected View mView;
    protected Context mContext;
    private SQLiteHelper sqLiteHelper;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Banner banner = getActivity().findViewById(R.id.banner);
        super.onActivityCreated(savedInstanceState);
        
        // 设置图片加载器
        banner.setImageLoader(new GlideImageLoader());
        // 设置轮播图片
        banner.setImages(Data.images);
        // 设置指示器位置
        banner.setIndicatorGravity(BannerConfig.CENTER);
        // 设置自动轮播
        banner.isAutoPlay(true);
        // 设置轮播间隔时间
        banner.setDelayTime(2500);
        
        // 设置轮播图点击事件
        banner.setOnBannerListener(new OnBannerListener() {
            @Override
            public void OnBannerClick(int position) {
                Toast.makeText(getActivity(), "点击了第" + (position + 1) + "张轮播图", Toast.LENGTH_SHORT).show();
            }
        }).start();

        ImageButton imageButton = getActivity().findViewById(R.id.imb_queding);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getActivity(), "success2",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getActivity(), AdminListActivity.class);
                startActivity(intent);
            }
        });
        getActivity().findViewById(R.id.btnSiXiang).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ThemeListActivity.class);
                intent.putExtra("theme", "思乡");
                startActivity(intent);
            }
        });
        getActivity().findViewById(R.id.btnAiGuo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ThemeListActivity.class);
                intent.putExtra("theme", "爱国");
                startActivity(intent);
            }
        });
        getActivity().findViewById(R.id.btnLiBie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ThemeListActivity.class);
                intent.putExtra("theme", "离别");
                startActivity(intent);
            }
        });
        getActivity().findViewById(R.id.btnSongBie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ThemeListActivity.class);
                intent.putExtra("theme", "友情");
                startActivity(intent);
            }
        });
        getActivity().findViewById(R.id.btnLiZhi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ThemeListActivity.class);
                intent.putExtra("theme", "励志");
                startActivity(intent);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        sqLiteHelper = new SQLiteHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        mView = inflater.inflate(R.layout.home, container, false);
        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // 添加按主题筛选的方法
    private List<NewsBean> filterByTheme(String theme) {
        List<NewsBean> allPoems = sqLiteHelper.query();
        List<NewsBean> filteredPoems = new ArrayList<>();
        
        for (NewsBean poem : allPoems) {
            if (theme.equals(poem.getTheme())) {
                filteredPoems.add(poem);
            }
        }
        
        return filteredPoems;
    }
}

