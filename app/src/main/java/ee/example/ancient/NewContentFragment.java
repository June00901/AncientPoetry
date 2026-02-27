package ee.example.ancient;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

//功能：显示古诗的内容和标题的 Fragment。
//主要功能：
//从传递的参数中获取古诗的标题和内容，并在界面上展示

//管理员古诗列表 - 下面Fragment - 详情Fragment
public class NewContentFragment extends Fragment {

    @SuppressLint("ValidFragment")
    NewContentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fg_content, container, false);
        TextView txt_title = (TextView) view.findViewById(R.id.txt_title);
        TextView txt_content = (TextView) view.findViewById(R.id.txt_content);
        //getArgument获取传递过来的Bundle对象
        txt_title.setText(getArguments().getString("time"));
        txt_content.setText(getArguments().getString("content"));
        return view;
    }

}
