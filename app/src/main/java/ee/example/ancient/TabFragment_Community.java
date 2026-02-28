package ee.example.ancient;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import ee.example.ancient.utils.PoemApiClient;

public class TabFragment_Community extends Fragment {
    private static final String TAG = "TabFragment_Community";
    protected View mView;
    protected Context mContext;

    private EditText inputKeywords;
    private Spinner spinnerStyle;
    private Button btnGenerate;
    private TextView textResult;
    private PoemApiClient poemApiClient;

    // 新增：功能切换按钮
    private Button btnAiGenerate;
    private Button btnAiScore;
    private LinearLayout layoutGenerate;
    private LinearLayout layoutScoreHint;
    private Button btnGoScore;

    private static final String[] POEM_STYLES = {
            "五言绝句", "七言绝句", "五言律诗", "七言律诗", "词牌", "现代诗"
    };

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

        if (!lastGeneratedPoem.isEmpty()) {
            textResult.setText(lastGeneratedPoem);
        }

        return mView;
    }

    private void initViews() {
        // 原有控件
        inputKeywords = mView.findViewById(R.id.input_keywords);
        spinnerStyle = mView.findViewById(R.id.spinner_style);
        btnGenerate = mView.findViewById(R.id.btn_generate);
        textResult = mView.findViewById(R.id.text_result);
        textResult.setMovementMethod(new ScrollingMovementMethod());

        // 新增控件
        btnAiGenerate = mView.findViewById(R.id.btn_ai_generate);
        btnAiScore = mView.findViewById(R.id.btn_ai_score);
        layoutGenerate = mView.findViewById(R.id.layout_generate);
        layoutScoreHint = mView.findViewById(R.id.layout_score_hint);
        btnGoScore = mView.findViewById(R.id.btn_go_score);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                mContext,
                android.R.layout.simple_spinner_item,
                POEM_STYLES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStyle.setAdapter(adapter);
        spinnerStyle.setSelection(0);
    }

    private void setupListeners() {
        // 原有的生成逻辑
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

        // 新增：功能切换
        btnAiGenerate.setOnClickListener(v -> {
            layoutGenerate.setVisibility(View.VISIBLE);
            layoutScoreHint.setVisibility(View.GONE);
            btnAiGenerate.setAlpha(1.0f);
            btnAiScore.setAlpha(0.6f);
        });

        btnAiScore.setOnClickListener(v -> {
            layoutGenerate.setVisibility(View.GONE);
            layoutScoreHint.setVisibility(View.VISIBLE);
            btnAiGenerate.setAlpha(0.6f);
            btnAiScore.setAlpha(1.0f);
        });

        // 跳转到评分页面
        btnGoScore.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PoemScoreActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!lastGeneratedPoem.isEmpty()) {
            textResult.setText(lastGeneratedPoem);
        }
    }
}