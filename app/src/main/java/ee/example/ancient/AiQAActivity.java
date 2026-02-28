package ee.example.ancient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ee.example.ancient.utils.PoemApiClient;

public class AiQAActivity extends AppCompatActivity {

    private EditText etQuestion;
    private Button btnAsk;
    private TextView tvAnswer;
    private PoemApiClient poemApiClient;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_qa);

        etQuestion = findViewById(R.id.et_question);
        btnAsk = findViewById(R.id.btn_ask);
        tvAnswer = findViewById(R.id.tv_answer);

        poemApiClient = new PoemApiClient();
        mainHandler = new Handler(Looper.getMainLooper());

        btnAsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = etQuestion.getText().toString().trim();
                if (TextUtils.isEmpty(question)) {
                    Toast.makeText(AiQAActivity.this, "请输入问题", Toast.LENGTH_SHORT).show();
                    return;
                }
                askQuestion(question);
            }
        });
    }

    private void askQuestion(String question) {
        tvAnswer.setText("正在思考中...");
        btnAsk.setEnabled(false);

        // 构造针对诗词问答的prompt
        String prompt = "你是一位专业的古诗词研究专家。请回答以下关于中国古诗词的问题：\n\n"
                + "问题：" + question + "\n\n"
                + "请用通俗易懂的语言详细解答，如果问题涉及具体诗词，请引用原句并解释。\n"
                + "回答格式：\n"
                + "【答案】\n"
                + "（详细解答内容）";

        poemApiClient.callApiWithPrompt(prompt, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String answer) {
                tvAnswer.setText(answer);
                btnAsk.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                tvAnswer.setText("提问失败，请稍后重试。\n\n错误信息：" + error);
                btnAsk.setEnabled(true);
                Toast.makeText(AiQAActivity.this, "API调用失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源（如有需要）
    }
}