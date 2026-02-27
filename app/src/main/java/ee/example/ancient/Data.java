package ee.example.ancient;

import java.util.ArrayList;
import java.util.List;

//功能：存储全局数据和状态。
//主要功能：
//提供静态变量和方法，用于管理用户的登录状态、用户信息等。


//轮播图数据
public class Data {

    public static Boolean sta_dl=true;
    public static Boolean sta_np=false;
    public static String sta_name;
    public static String sta_password;
    public static int sat_con=0;
    public static List<Integer> images = new ArrayList<>();
    public static Long userId;
    public static void initView()
  {

      images.clear();
      images.add(R.drawable.b1);
      images.add(R.drawable.b10);
      images.add(R.drawable.b8);
      images.add(R.drawable.b9);
      images.add(R.drawable.b5);
      images.add(R.drawable.b6);
      images.add(R.drawable.b3);
      images.add(R.drawable.b4);
      images.add(R.drawable.b7);


  }
}