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
import android.widget.TextView;
import android.widget.Toast;

import com.youth.banner.BannerConfig;
import com.youth.banner.listener.OnBannerListener;
import com.youth.banner.Banner;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.ArrayList;

import ee.example.ancient.utils.RandomPoemClient;

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

    // 新增：随机诗词相关
    private RandomPoemClient randomPoemClient;
    private TextView tvRandomPoem;
    private TextView tvRandomPoemSource;
    private TextView tvBannerPoem;
    private String currentPoemId; // 保存当前显示的古诗ID
    
    // 本地名句列表
    private static final String[][] LOCAL_POEMS = {
        {"春眠不觉晓，处处闻啼鸟。", "孟浩然", "春晓"},
        {"床前明月光，疑是地上霜。", "李白", "静夜思"},
        {"锄禾日当午，汗滴禾下土。", "李绅", "悯农"},
        {"欲穷千里目，更上一层楼。", "王之涣", "登鹳雀楼"},
        {"飞流直下三千尺，疑是银河落九天。", "李白", "望庐山瀑布"},
        {"桃花潭水深千尺，不及汪伦送我情。", "李白", "赠汪伦"},
        {"莫愁前路无知己，天下谁人不识君。", "高适", "别董大"},
        {"两个黄鹂鸣翠柳，一行白鹭上青天。", "杜甫", "绝句"},
        {"随风潜入夜，润物细无声。", "杜甫", "春夜喜雨"},
        {"等闲识得东风面，万紫千红总是春。", "朱熹", "春日"},
        {"问渠那得清如许？为有源头活水来。", "朱熹", "观书有感"},
        {"纸上得来终觉浅，绝知此事要躬行。", "陆游", "冬夜读书示子聿"},
        {"山重水复疑无路，柳暗花明又一村。", "陆游", "游山西村"},
        {"人生自古谁无死？留取丹心照汗青。", "文天祥", "过零丁洋"},
        {"落红不是无情物，化作春泥更护花。", "龚自珍", "己亥杂诗"}
    };

    // 轮播图对应的古诗列表
    private static final String[][] BANNER_POEMS = {
        {"江南春", "杜牧", "1"}, // 对应b1.jpg
        {"望岳", "杜甫", "3"},   // 对应b10.jpg
        {"赠汪伦", "李白", "5"}, // 对应b8.jpg
        {"早发白帝城", "李白", "6"}, // 对应b9.jpg
        {"春望", "杜甫", "4"},   // 对应b5.jpg
        {"望天门山", "李白", "7"}, // 对应b6.jpg
        {"送友人", "李白", "8"},   // 对应b3.jpg
        {"黄鹤楼送孟浩然之广陵", "李白", "9"}, // 对应b4.jpg
        {"静夜思", "李白", "10"}   // 对应b7.jpg
    };

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

        // 初始化轮播图古诗名字
        tvBannerPoem = getActivity().findViewById(R.id.tv_banner_poem);

        // 设置轮播图点击事件
        banner.setOnBannerListener(new OnBannerListener() {
            @Override
            public void OnBannerClick(int position) {
                // 跳转到对应的古诗详情页面
                if (position < BANNER_POEMS.length) {
                    String[] poem = BANNER_POEMS[position];
                    String poemId = poem[2];
                    
                    Intent intent = new Intent(getActivity(), GuShiDetailActivity.class);
                    intent.putExtra("id", poemId);
                    startActivity(intent);
                }
            }
        }).start();

        // 监听轮播图页面变化，更新古诗名字
        banner.setOnPageChangeListener(new androidx.viewpager.widget.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // 更新轮播图上的古诗名字
                if (position < BANNER_POEMS.length) {
                    String[] poem = BANNER_POEMS[position];
                    String poemTitle = poem[0];
                    String poemAuthor = poem[1];
                    tvBannerPoem.setText(poemTitle + " · " + poemAuthor);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // 初始显示第一张轮播图的古诗名字
        if (BANNER_POEMS.length > 0) {
            String[] firstPoem = BANNER_POEMS[0];
            String poemTitle = firstPoem[0];
            String poemAuthor = firstPoem[1];
            tvBannerPoem.setText(poemTitle + " · " + poemAuthor);
        }

        // ===== 新增：初始化随机诗词卡片 =====
        tvRandomPoem = getActivity().findViewById(R.id.tv_random_poem);
        tvRandomPoemSource = getActivity().findViewById(R.id.tv_random_poem_source);
        randomPoemClient = new RandomPoemClient();

        // 加载随机诗词
        loadRandomPoem();

        // 点击卡片刷新
        getActivity().findViewById(R.id.card_random_poem).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadRandomPoem();
            }
        });

        // 详情标签点击事件
        getActivity().findViewById(R.id.btn_poem_detail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到古诗详情页面，只在本地有对应古诗时才允许跳转
                if (currentPoemId != null) {
                    // 使用保存的古诗ID直接跳转
                    Intent intent = new Intent(getActivity(), GuShiDetailActivity.class);
                    intent.putExtra("id", currentPoemId);
                    startActivity(intent);
                }
            }
        });

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

        // AI辅助学习计划按钮点击事件
        getActivity().findViewById(R.id.card_ai_study_plan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AiStudyPlanActivity.class);
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

    // ===== 新增：加载随机诗词方法 =====
    private void loadRandomPoem() {
        tvRandomPoem.setText("正在加载...");
        tvRandomPoemSource.setText("");
        currentPoemId = null;

        randomPoemClient.getRandomPoem(new RandomPoemClient.RandomPoemCallback() {
            @Override
            public void onSuccess(String content, String author, String title) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvRandomPoem.setText(content);
                            tvRandomPoemSource.setText("—— " + title + " · " + author);
                            // 从数据库中查找对应的古诗ID
                            PlaceDatabase database = new PlaceDatabase(getContext(), PlaceDatabase.DATABASE_NAME, null, 1);
                            List<PlaceBean> list = database.find(title);
                            if (list != null && !list.isEmpty()) {
                                currentPoemId = list.get(0).getId();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 从本地名句列表中随机选择一首
                            loadLocalPoem();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (randomPoemClient != null) {
            randomPoemClient.shutdown();
        }
    }

    // 从本地名句列表中随机选择一首
    private void loadLocalPoem() {
        if (getActivity() == null) return;
        
        // 随机生成索引
        int randomIndex = (int) (Math.random() * LOCAL_POEMS.length);
        String[] poem = LOCAL_POEMS[randomIndex];
        
        String content = poem[0];
        String author = poem[1];
        String title = poem[2];
        
        // 显示本地名句
        tvRandomPoem.setText(content);
        tvRandomPoemSource.setText("—— " + title + " · " + author + " (本地名句)");
        
        // 从数据库中查找对应的古诗ID
        PlaceDatabase database = new PlaceDatabase(getContext(), PlaceDatabase.DATABASE_NAME, null, 1);
        List<PlaceBean> list = database.find(title);
        if (list != null && !list.isEmpty()) {
            currentPoemId = list.get(0).getId();
        } else {
            currentPoemId = null;
        }
    }
}