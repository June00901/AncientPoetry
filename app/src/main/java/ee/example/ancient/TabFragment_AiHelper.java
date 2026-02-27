package ee.example.ancient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

import ee.example.ancient.utils.PoemApiClient;

public class TabFragment_AiHelper extends Fragment {

    private EditText etEmotion;
    private Button btnMatch;
    private TextView tvResult;

    private PoemApiClient poemApiClient;
    private Handler mainHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_helper, container, false);

        etEmotion = view.findViewById(R.id.et_emotion);
        btnMatch = view.findViewById(R.id.btn_match);
        tvResult = view.findViewById(R.id.tv_result);

        // 初始化API客户端
        poemApiClient = new PoemApiClient();
        mainHandler = new Handler(Looper.getMainLooper());

        // 点击按钮匹配诗词
        btnMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userInput = etEmotion.getText().toString().trim();
                if (TextUtils.isEmpty(userInput)) {
                    Toast.makeText(getActivity(), "请输入情感或主题", Toast.LENGTH_SHORT).show();
                    return;
                }
                matchPoemByEmotion(userInput);
            }
        });

        return view;
    }

    private void matchPoemByEmotion(String emotion) {
        // 显示加载中
        tvResult.setText("🔍 正在从千年诗库中寻觅「" + emotion + "」...\n⏳ 请稍候（约5-8秒）");
        btnMatch.setEnabled(false);

        // 优化后的prompt，强化文笔要求，加入具体例句示范
        String prompt = "你是一位高考作文满分得主、著名散文家、古诗词研究专家。\n"
                + "用户需要一首表达'" + emotion + "'情感或哲理的中国古诗词。\n\n"
                + "【核心要求】\n"
                + "1. 必须是古人创作的经典作品（李白、杜甫、苏轼、李清照等名家）\n"
                + "2. 绝对不要自己创作新诗\n"
                + "3. 选择最能表达'" + emotion + "'情感的一首\n"
                + "4. 必须包含完整的诗词原文\n\n"
                + "请严格按照以下格式返回：\n\n"
                + "【诗词信息】\n"
                + "诗名：《》\n"
                + "作者：\n"
                + "朝代：\n\n"
                + "【全文】\n"
                + "（诗词全文，注意换行，保持原格式）\n\n"
                + "【推荐理由】\n"
                + "（为什么这首诗表达了" + emotion + "情感，60字左右，语言要优美）\n\n"
                + "【哲理赏析】\n"
                + "（这首诗蕴含的人生哲理，60字左右，要有深度）\n\n"
                + "【作文例句】\n"
                + "请用优美的文学语言写一个完整的作文段落（80-100字），要求：\n"
                + "- 语言如诗如画，富有感染力\n"
                + "- 善用比喻、拟人、排比等修辞手法\n"
                + "- 自然融入诗词原句，不能生硬插入\n"
                + "- 让读者感受到文字的温度和力量\n\n"
                + "参考示例（以'思乡'为例）：\n"
                + "『每当夜深人静，独倚窗前，总会想起李白那句“举头望明月，低头思故乡”。\n"
                + "那轮皎洁的明月，照过盛唐的宫殿，照过李白的酒杯，今夜又静静照着我这异乡人。\n"
                + "月光如水，流淌千年，却流不走游子心头那抹淡淡的乡愁。』\n\n"
                + "请用类似的文采为'" + emotion + "'主题创作一个例句。";

        // 调用API
        poemApiClient.callApiWithPrompt(prompt, new PoemApiClient.PoemCallback() {
            @Override
            public void onSuccess(String poem) {
                tvResult.setText(poem);
                btnMatch.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                tvResult.setText("寻觅失败，请稍后重试。\n\n错误信息：" + error);
                btnMatch.setEnabled(true);
                Toast.makeText(getActivity(), "API调用失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理资源
    }
}