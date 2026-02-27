package ee.example.ancient;

import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import ee.example.ancient.utils.PoemApiClient;

//功能：实现社区功能的 Fragment，主要用于生成诗词。
//主要功能：
//提供用户输入关键词和选择诗词体裁的界面。
//使用 PoemApiClient 生成诗词，并显示生成的结果。
//支持保存上次生成的诗句，以便在 Fragment 切换时恢复显示。

//消息
public class TabFragment_Community extends Fragment {
    private static final String TAG = "TabFragment_Community";
    protected View mView;
    protected Context mContext;

    private EditText inputKeywords;
    private Spinner spinnerStyle;
    private Button btnGenerate;
    private TextView textResult;
    private PoemApiClient poemApiClient;

    // 添加诗词体裁数组
    private static final String[] POEM_STYLES = {
            "五言绝句", "七言绝句", "五言律诗", "七言律诗", "词牌", "现代诗"
    };

    // 保存生成的诗句
    private static String lastGeneratedPoem = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        poemApiClient = new PoemApiClient();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        mView = inflater.inflate(R.layout.community, container, false);

        initViews();
        setupSpinner();
        setupListeners();

        // 恢复上次生成的诗句
        if (!lastGeneratedPoem.isEmpty()) {
            textResult.setText(lastGeneratedPoem);
        }

        return mView;
    }

    private void initViews() {
        inputKeywords = mView.findViewById(R.id.input_keywords);
        spinnerStyle = mView.findViewById(R.id.spinner_style);
        btnGenerate = mView.findViewById(R.id.btn_generate);
        textResult = mView.findViewById(R.id.text_result);

        // 设置TextView可滚动
        textResult.setMovementMethod(new ScrollingMovementMethod());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                mContext,
                android.R.layout.simple_spinner_item,
                POEM_STYLES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStyle.setAdapter(adapter);
        spinnerStyle.setSelection(0); // 设置默认选中第一项
    }

    private void setupListeners() {
        btnGenerate.setOnClickListener(v -> {
            String keywords = inputKeywords.getText().toString().trim();
            String style = spinnerStyle.getSelectedItem().toString();

            if (keywords.isEmpty()) {
                Toast.makeText(mContext, "请输入关键词", Toast.LENGTH_SHORT).show();
                return;
            }

            btnGenerate.setEnabled(false);
            textResult.setText("正在创作中...");

            poemApiClient.generatePoem(keywords, style, new PoemApiClient.PoemCallback() {
                @Override
                public void onSuccess(String poem) {
                    // 保存生成的诗句到静态变量
                    lastGeneratedPoem = poem;
                    textResult.setText(poem);
                    btnGenerate.setEnabled(true);
                }

                @Override
                public void onError(String error) {
                    textResult.setText("创作失败：" + error);
                    btnGenerate.setEnabled(true);
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 切换回来时恢复显示
        if (!lastGeneratedPoem.isEmpty()) {
            textResult.setText(lastGeneratedPoem);
        }
    }
}



