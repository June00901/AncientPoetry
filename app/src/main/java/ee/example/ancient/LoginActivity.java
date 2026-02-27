package ee.example.ancient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//- 功能：实现用户登录界面。
//        - **主要功能**：
//        - 提供用户输入用户名和密码的界面。
//        - 进行网络请求以验证用户的登录信息。
//        - 登录成功后保存用户信息，并跳转到主界面。
//        - 提供注册新用户的功能。
//        - 记住用户登录状态的选项（自动登录和记住密码）。


//登录
//这里是执行提交版本的前的注释
public class LoginActivity extends Activity {
    private EditText username;
    private EditText password;
    private Button login;
    private Button register;
    private CheckBox remember;
    private CheckBox autologin;
    static private String SPdata="SPdata";
    static private String SPname="name";
    static private String SPpassword="password";
    static private String SPauto="auto";
    static private String spname;
    static private String spassword;
    static private boolean spauto;
    private DataBaseHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化视图
        intiview();
        dbHelper = DataBaseHelper.getInstance(this);

        // 检查是否已经登录且开启了自动登录
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        boolean isLoggedIn = sp.getBoolean("isLoggedIn", false);
        boolean isAutoLogin = sp.getBoolean("isAutoLogin", false);

        if (isLoggedIn && isAutoLogin) {
            // 已登录且开启自动登录，直接进入主界面
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        // 添加默认用户
        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.USERS_TABLE, null, 1);
        SQLiteDatabase db = database.getWritableDatabase();

        // 检查用户是否已存在
        Cursor cursor = db.query(PlaceDatabase.USERS_TABLE,
                new String[]{"id"},
                "username=?",
                new String[]{"1"},
                null, null, null);

        if (cursor.getCount() == 0) {
            // 用户不存在，添加默认用户
            ContentValues values = new ContentValues();
            values.put("username", "1");
            values.put("password", "1");
            db.insert(PlaceDatabase.USERS_TABLE, null, values);
        }
        cursor.close();

        spauto=false;
        checkIfRemember();

        if(Data.sta_dl==true)
            if(spauto==true) {
                Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                Data.sta_np=true;
                startActivity(intent);
                finish();
            }
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = username.getText().toString();
                String pwd = password.getText().toString();

                if (name.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 执行登录
                login(name, pwd);
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 使用原有的 RegiterActivity（拼写错误版本）
                Intent intent = new Intent(LoginActivity.this, RegiterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intiview(){
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        register = findViewById(R.id.register);
        login = findViewById(R.id.login);
        remember = findViewById(R.id.remember);
        autologin = findViewById(R.id.autologin);
    }

    public void checkIfRemember(){
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        boolean isLoggedIn = sp.getBoolean("isLoggedIn", false);
        boolean isAutoLogin = sp.getBoolean("isAutoLogin", false);

        if (isLoggedIn && isAutoLogin) {
            // 恢复登录状态
            String savedUsername = sp.getString("username", "");
            int savedUserId = sp.getInt("userId", -1);

            if (savedUserId != -1) {
                Data.userId = Long.valueOf(savedUserId);
                Data.sta_name = savedUsername;
                Data.sta_np = true;

                // 直接进入主界面
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
    }

    public void rememberMe(String name1,String password1,boolean auto1){
        SharedPreferences sp = getSharedPreferences(SPdata,MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();	    //获得Editor
        editor.putString(SPname, name1);					    //将用户的帐号存入Preferences
        editor.putString(SPpassword, password1);					//将密码存入Preferences
        editor.putBoolean(SPauto , auto1);
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBar(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//注意要清除 FLAG_TRANSLUCENT_STATUS flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.xml_color));//设置要显示的颜色（Color.TRANSPARENT为透明）
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void saveUserInfo(String username) {
        SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("username", username);
        editor.putInt("userId", Data.userId.intValue());
        editor.putBoolean("isAutoLogin", remember.isChecked());
        editor.apply();

        // 同时更新全局状态
        Data.sta_np = true;
        Data.sta_name = username;
    }

    private void login(String username, String password) {
        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.USERS_TABLE, null, 1);
        int userId = database.loginUser(username, password);

        if (userId != -1) {
            // 登录成功，保存登录状态
            Data.userId = Long.valueOf(userId);
            Data.sta_name = username;
            Data.sta_password = password;
            Data.sta_np = true;

            // 保存到 SharedPreferences
            SharedPreferences sp = getSharedPreferences("userInfo", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("username", username);
            editor.putInt("userId", userId);
            editor.putBoolean("isAutoLogin", remember.isChecked());
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
        }
    }
}