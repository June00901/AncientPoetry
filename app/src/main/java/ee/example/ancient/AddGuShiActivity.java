package ee.example.ancient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//注意：此模块已停止使用
//功能：实现添加古诗的界面。
//主要功能：
//提供用户输入古诗标题和内容的界面。
//支持保存新添加的古诗到数据库。
//允许用户修改已有古诗的内容。

//添加古诗
public class AddGuShiActivity extends AppCompatActivity implements View.OnClickListener{

    ImageView note_back;
    EditText content,editTitle;
    ImageView delete;
    ImageView note_save;
    SQLiteHelper mSQLiteHelper;
    TextView noteName;
    String id;
    private Spinner spinnerTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        //声明控件
        note_back = (ImageView) findViewById(R.id.note_back);
        editTitle = (EditText) findViewById(R.id.note_title);
        content = (EditText) findViewById(R.id.note_content);
        delete = (ImageView) findViewById(R.id.delete);
        note_save = (ImageView) findViewById(R.id.note_save);
        noteName = (TextView) findViewById(R.id.note_name);
        spinnerTheme = findViewById(R.id.spinner_theme);
        //设置控件监听事件
        note_back.setOnClickListener(this);
        delete.setOnClickListener(this);
        note_save.setOnClickListener(this);
        //初始化数据
        initData();
    }
    protected void initData() {

        mSQLiteHelper = new SQLiteHelper(this);
        noteName.setText("添加");
        //NotepadActivity  获取上个页面传递过来数据
        Intent intent = getIntent();
        if(intent!= null){
            id = intent.getStringExtra("id");
            if (id != null){
                noteName.setText("修改");
                content.setText(intent.getStringExtra("content"));
                editTitle.setText(intent.getStringExtra("time"));

            }
        }
    }

    //点击事件监听
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.note_back:
                finish();//关闭
                break;
            case R.id.delete:
                content.setText("");//清空
                editTitle.setText("");//清空
                if (id != null){
                    mSQLiteHelper.deleteData(id);
                    showToast("删除成功!");
                    setResult(2);
                }
                break;
            case R.id.note_save:
                //获取输入内容
                String mTitle=editTitle.getText().toString().trim();
                String noteContent=content.getText().toString().trim();
                //获取主题
                String theme = spinnerTheme.getSelectedItem().toString();
                
                //保存数据时加入主题
                NewsBean bean = new NewsBean();
                bean.setTitle(mTitle);
                bean.setContent(noteContent);
                bean.setTheme(theme);
                bean.setTime(getCurrentTime());
                
                if (id != null){//修改操作
                    if (noteContent.length()>0){
                        //更新数据
                        if (mSQLiteHelper.updateData(id, noteContent, mTitle, theme)){
                            showToast("修改成功");
                            //回调给上个页面  刷新数据
                            setResult(2);
                            finish();
                        }else {
                            showToast("修改失败");
                        }
                    }else {
                        showToast("修改内容不能为空!");
                    }
                }else {
                    //向数据库中添加数据
                    if (noteContent.length()>0){
                        if (mSQLiteHelper.insertData(noteContent, mTitle, theme)){
                            showToast("保存成功");
                            setResult(2);
                            finish();
                        }else {
                            showToast("保存失败");
                        }
                    }else {
                        showToast("修改内容不能为空!");
                    }
                }
                break;
        }
    }
    public void showToast(String message){
        Toast.makeText(AddGuShiActivity.this,message,Toast.LENGTH_SHORT).show();
    }

    // 添加获取时间的方法
    private String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return format.format(date);
    }
}