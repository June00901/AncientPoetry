package ee.example.ancient;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//功能：实现用户注册的界面。
//主要功能：
//提供用户输入用户名和密码的界面。
//提供注册成功或失败的反馈。

//注册页面
public class RegiterActivity extends AppCompatActivity {
    private EditText nameEdit;
    private EditText passwordEdit;
    private Button queren;
    private Button quxiao;
    private DataBaseHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_simple);  // 注意这里
        dbHelper=DataBaseHelper.getInstance(this);
        iniview();
        quxiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        queren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = nameEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegiterActivity.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 使用网络注册
                register(username, password);
            }
        });
    }

    private void iniview(){
        nameEdit=findViewById(R.id.username);
        passwordEdit=findViewById(R.id.password);
        queren=findViewById(R.id.queding);
        quxiao=findViewById(R.id.quxiao);
    }

    private void register(String username, String password) {
        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.USERS_TABLE, null, 1);
        long result = database.registerUser(username, password);
        
        if (result != -1) {
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "用户名已存在", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}