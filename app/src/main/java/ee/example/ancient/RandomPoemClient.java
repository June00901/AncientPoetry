package ee.example.ancient.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RandomPoemClient {
    private static final String TAG = "RandomPoemClient";
    // 改用这个可用的地址
    private static final String API_URL = "https://v2.jinrishici.com/random.json";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RandomPoemCallback {
        void onSuccess(String content, String author, String title);
        void onError(String error);
    }

    public void getRandomPoem(RandomPoemCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    Log.d(TAG, "开始请求: " + API_URL);

                    URL url = new URL(API_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "响应码: " + responseCode);

                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), "UTF-8"));
                        String line = reader.readLine();
                        reader.close();

                        Log.d(TAG, "响应内容: " + line);

                        // 古诗词API返回格式：诗句 —— 诗名·作者
                        if (line != null && line.contains("——")) {
                            String[] parts = line.split("——");
                            String content = parts[0].trim();
                            String[] source = parts[1].split("·");
                            String title = source[0].trim();
                            String author = source.length > 1 ? source[1].trim() : "佚名";

                            mainHandler.post(() -> callback.onSuccess(content, author, title));
                        } else {
                            mainHandler.post(() -> callback.onError("解析失败"));
                        }
                    } else {
                        mainHandler.post(() -> callback.onError("HTTP错误: " + responseCode));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "请求失败", e);
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        });
    }

    public void shutdown() {
        if (executorService != null) executorService.shutdown();
    }
}