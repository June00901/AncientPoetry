package ee.example.ancient;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

//功能：实现管理员古诗列表的 Fragment。
//主要功能：
//显示古诗的列表，使用 ListView 展示 NewsBean 数据。
//支持点击列表项进入编辑界面或显示详细内容。
//根据用户的权限（如是否为管理员）决定操作的可用性

//管理员古诗列表 - 下面Fragment
@SuppressLint("ValidFragment")
public class NewListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private FragmentManager fManager;
    private ArrayList<NewsBean> datas;
    private ListView list_news;

    public NewListFragment(FragmentManager fManager, ArrayList<NewsBean> datas) {
        this.fManager = fManager;
        this.datas = datas;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fg_newlist, container, false);
        list_news = (ListView) view.findViewById(R.id.list_news);
        MyAdapter2 myAdapter = new MyAdapter2(datas, getActivity());
        list_news.setAdapter((ListAdapter) myAdapter);
        list_news.setOnItemClickListener(this);
        return view;
    }


    @SuppressLint("ResourceType")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(Data.sta_np==true){
            if (Data.sta_name.equals("admin")){
                //获取行对应数据
                NewsBean notepadBean = datas.get(position);
                Intent intent = new Intent(getActivity(), AddGuShiActivity.class);
                //传递数据
                intent.putExtra("id", notepadBean.getId());
                intent.putExtra("time", notepadBean.getTitle()); //记录的时间
                intent.putExtra("content", notepadBean.getContent()); //记录的内容
                getActivity().startActivityForResult(intent, 1);
            }else {
                FragmentTransaction fTransaction = fManager.beginTransaction();
                NewContentFragment ncFragment = new NewContentFragment();
                Bundle bd = new Bundle();
                bd.putString("time", datas.get(position).getTitle());
                bd.putString("content", datas.get(position).getContent());
                ncFragment.setArguments(bd);
                //获取Activity的控件
//                TextView txt_title = (TextView) getActivity().findViewById(R.id.txt_title);
//
//                txt_title.setText(datas.get(position).getContent());
//                txt_title.setText(datas.get(position).getContent_title());

                //加上Fragment替换动画
                fTransaction.setCustomAnimations(R.anim.fragment_slide_left_enter, R.anim.fragment_slide_left_exit);
                fTransaction.replace(R.id.fl_content, ncFragment);
                //调用addToBackStack将Fragment添加到栈中
                fTransaction.addToBackStack(null);
                fTransaction.commit();
            }
        }
    }
}
