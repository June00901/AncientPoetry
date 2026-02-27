package ee.example.ancient;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.youth.banner.loader.ImageLoader;

//功能：自定义图像加载器，使用 Glide 库加载图片。
//主要功能：
//继承自 ImageLoader，重写 displayImage 方法，将图片加载到指定的 ImageView 中。


public class GlideImageLoader extends ImageLoader {
    @Override
    public void displayImage(Context context, Object path, ImageView imageView) {
        Glide.with(context).load(path).into(imageView);
    }
}

