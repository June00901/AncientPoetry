package ee.example.ancient;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


//功能：实现用户修改密码的界面。
//主要功能：
//提供输入旧密码和新密码的界面。
//显示修改结果的提示信息。

//修改密码页面
public class MainChangemima extends AppCompatActivity {
    private EditText old_password;
    private EditText new_password;
    private EditText new_password2;
    private Button btn_sure;
    private Button btn_cancel;
    private DataBaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_changemima);
        initView();
        dbHelper = DataBaseHelper.getInstance(this);
    }

    private void initView() {
        old_password = findViewById(R.id.old_password);
        new_password = findViewById(R.id.new_password);
        new_password2 = findViewById(R.id.new_password2);
        btn_sure = findViewById(R.id.btn_sure);
        btn_cancel = findViewById(R.id.btn_cancel);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btn_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldPwd = old_password.getText().toString();
                String newPwd = new_password.getText().toString();
                String confirmPwd = new_password2.getText().toString();

                // 验证输入
                if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                    Toast.makeText(MainChangemima.this, "请填写所有密码字段", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPwd.equals(confirmPwd)) {
                    Toast.makeText(MainChangemima.this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 调用后端API修改密码
                changePassword(oldPwd, newPwd);
            }
        });
    }

    private void changePassword(String oldPassword, String newPassword) {
        PlaceDatabase database = new PlaceDatabase(this, PlaceDatabase.DATABASE_NAME, null, 1);
        boolean success = database.changePassword(Data.userId.intValue(), oldPassword, newPassword);
        
        if (success) {
            new AlertDialog.Builder(MainChangemima.this)
                .setTitle("提示")
                .setMessage("修改密码成功")
                .setPositiveButton("确定", (dialog, which) -> {
                    Data.sta_password = newPassword;
                    finish();
                })
                .show();
        } else {
            Toast.makeText(this, "原密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    //按两次back键退出
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //获取按键并比较两次按back的时间大于2s不退出，否则退出
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == 0) {
            Intent intent =new Intent(MainChangemima.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
