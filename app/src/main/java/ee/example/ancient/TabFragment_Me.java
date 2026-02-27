package ee.example.ancient;

import android.Manifest;
import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import ee.example.ancient.activity.NoteListActivity;

//   功能：实现用户个人中心界面。
//        - **主要功能**：
//        - 显示用户的头像和昵称。
//        - 提供修改头像和昵称的功能。
//        - 显示用户的登录状态（已登录或未登录）。
//        - 提供进入收藏、学习笔记、修改密码和退出登录的功能。
//        - 头像和昵称的保存与加载，使用 `SharedPreferences` 存储用户信息。


//个人中心
public class TabFragment_Me extends Fragment {
    private View mView;
    private TextView tvUsername;
    private TextView tvLogout;
    private LinearLayout llMyCollection;
    private LinearLayout llStudyNotes;
    private LinearLayout llChangePassword;
    private LinearLayout llLogout;
    private ImageView ivAvatar;
    private TextView tvNickname;

    private static final int CAMERA_REQUEST = 1888;
    private static final int GALLERY_REQUEST = 1889;
    private static final String AVATAR_PREFS = "avatar_prefs";
    private static final String AVATAR_PATH = "avatar_path";
    private static final String NICKNAME_PREFS = "nickname_prefs";
    private static final String NICKNAME_KEY = "nickname";
    private static final String DEFAULT_NICKNAME = "清风";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("TabFragment_Me", "onCreateView started");
        mView = inflater.inflate(R.layout.fragment_me, container, false);
        initViews();
        setListeners();
        return mView;
    }

    private void initViews() {
        Log.d("TabFragment_Me", "initViews started");
        
        tvUsername = mView.findViewById(R.id.tv_username);
        tvLogout = mView.findViewById(R.id.tv_logout);
        llMyCollection = mView.findViewById(R.id.ll_my_collection);
        llStudyNotes = mView.findViewById(R.id.ll_study_notes);
        llChangePassword = mView.findViewById(R.id.ll_change_password);
        llLogout = mView.findViewById(R.id.ll_logout);
        ivAvatar = mView.findViewById(R.id.iv_avatar);
        tvNickname = mView.findViewById(R.id.tv_nickname);
        loadSavedAvatar();
        loadSavedNickname();

        updateLoginStatus();

        ivAvatar.setOnClickListener(v -> {
            Log.d("TabFragment_Me", "Avatar clicked");
            showImagePickerDialog();
        });

        tvNickname.setOnClickListener(v -> showNicknameDialog());

        mView.findViewById(R.id.ll_share_app).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "古诗词学习");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "我正在使用这个很棒的古诗词学习APP，推荐给你！\n" +
                "下载地址：[您的应用下载链接]");
            startActivity(Intent.createChooser(shareIntent, "分享应用"));
        });
    }

    private void updateLoginStatus() {
        Log.d("TabFragment_Me", "Updating login status, sta_np: " + Data.sta_np + ", sta_name: " + Data.sta_name);
        
        if (Data.sta_np && Data.sta_name != null) {
            tvUsername.setText(Data.sta_name);
            tvLogout.setText("退出登录");
        } else {
            tvUsername.setText("未登录");
            tvLogout.setText("登录");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TabFragment_Me", "onResume called");
        updateLoginStatus();
    }

    private void setListeners() {
        llMyCollection.setOnClickListener(v -> {
            Log.d("TabFragment_Me", "Collection clicked, login status: " + Data.sta_np);
            if (!Data.sta_np) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                return;
            }
            startActivity(new Intent(getActivity(), MyCollectionActivity.class));
        });

        llStudyNotes.setOnClickListener(v -> {
            Log.d("TabFragment_Me", "Notes clicked, login status: " + Data.sta_np);
            if (!Data.sta_np) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                return;
            }
            startActivity(new Intent(getActivity(), NoteListActivity.class));
        });

        llChangePassword.setOnClickListener(v -> {
            Log.d("TabFragment_Me", "Change password clicked, login status: " + Data.sta_np);
            if (!Data.sta_np) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                return;
            }
            startActivity(new Intent(getActivity(), MainChangemima.class));
        });

        llLogout.setOnClickListener(v -> {
            if (!Data.sta_np) {
                // 未登录状态，点击进入登录界面
                startActivity(new Intent(getActivity(), LoginActivity.class));
            } else {
                // 已登录状态，显示退出确认对话框
                new AlertDialog.Builder(getContext())
                    .setTitle("提示")
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        logout(); // 调用logout方法
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
    }

    private void showImagePickerDialog() {
        Log.d("TabFragment_Me", "showImagePickerDialog called");
        if (getContext() == null) {
            Log.e("TabFragment_Me", "Context is null");
            return;
        }
        
        String[] options = {"拍摄照片", "从手机相册选择", "取消"};
        new AlertDialog.Builder(getContext())
            .setTitle("选择图片来源")
            .setItems(options, (dialog, which) -> {
                Log.d("TabFragment_Me", "Dialog option selected: " + which);
                switch (which) {
                    case 0:
                        checkCameraPermissionAndOpen();
                        break;
                    case 1:
                        openGallery();
                        break;
                    case 2:
                        dialog.dismiss();
                        break;
                }
            })
            .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap photo = (Bitmap) extras.get("data");
                    if (photo != null) {
                        saveAndDisplayImage(photo);
                    }
                }
            } else if (requestCode == GALLERY_REQUEST && data != null) {
                try {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            getActivity().getContentResolver(), imageUri);
                        saveAndDisplayImage(bitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "加载图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveAndDisplayImage(Bitmap bitmap) {
        try {
            if (Data.userId == null) {
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用用户ID创建唯一的文件名
            String fileName = "avatar_" + Data.userId + ".jpg";
            File filesDir = getContext().getFilesDir();
            File imageFile = new File(filesDir, fileName);

            // 保存图片到文件
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            // 保存文件路径到 SharedPreferences,使用用户ID作为key
            SharedPreferences prefs = getContext().getSharedPreferences(AVATAR_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(AVATAR_PATH + "_" + Data.userId, imageFile.getAbsolutePath());
            editor.apply();

            // 显示图片
            ivAvatar.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "保存图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedAvatar() {
        if (Data.userId == null) {
            // 未登录时显示默认头像
            ivAvatar.setImageResource(R.drawable.backbg);
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences(AVATAR_PREFS, Context.MODE_PRIVATE);
        String avatarPath = prefs.getString(AVATAR_PATH + "_" + Data.userId, null);
        
        if (avatarPath != null) {
            File imageFile = new File(avatarPath);
            if (imageFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(avatarPath);
                    ivAvatar.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    ivAvatar.setImageResource(R.drawable.backbg);
                }
            } else {
                ivAvatar.setImageResource(R.drawable.backbg);
            }
        } else {
            ivAvatar.setImageResource(R.drawable.backbg);
        }
    }

    private void showNicknameDialog() {
        EditText input = new EditText(getContext());
        input.setText(tvNickname.getText());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(getContext())
            .setTitle("修改昵称")
            .setView(input)
            .setPositiveButton("确定", (dialog, which) -> {
                String newNickname = input.getText().toString().trim();
                if (!newNickname.isEmpty()) {
                    saveAndDisplayNickname(newNickname);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void saveAndDisplayNickname(String nickname) {
        if (Data.userId == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存昵称,使用用户ID作为key
        SharedPreferences prefs = getContext().getSharedPreferences(NICKNAME_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(NICKNAME_KEY + "_" + Data.userId, nickname);
        editor.apply();

        // 显示新昵称
        tvNickname.setText(nickname);
    }

    private void loadSavedNickname() {
        if (Data.userId == null) {
            // 未登录时显示默认昵称
            tvNickname.setText(DEFAULT_NICKNAME);
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences(NICKNAME_PREFS, Context.MODE_PRIVATE);
        String savedNickname = prefs.getString(NICKNAME_KEY + "_" + Data.userId, DEFAULT_NICKNAME);
        tvNickname.setText(savedNickname);
    }

    private void logout() {
        // 清除登录状态
        Data.sta_np = false;
        Data.sta_name = null;
        Data.userId = null;
        
        // 清除SharedPreferences中的登录信息
        SharedPreferences sp = getActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        sp.edit()
            .clear()
            .putBoolean("isLoggedIn", false)
            .apply();

        // 启动登录界面
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish(); // 结束当前Activity
        }
    }
}
