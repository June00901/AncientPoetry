package ee.example.ancient.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import okhttp3.*;

//功能：实现与外部诗词生成API的交互。
//主要功能：
//发送请求以生成诗词，支持根据关键词和风格生成。
//构建请求参数，包括系统消息和用户消息。
//处理API的响应，解析生成的诗词内容。
//提供回调接口 PoemCallback，用于处理成功和错误的响应。

public class PoemApiClient {
    private static final String TAG = "PoemApiClient";
    private static final String API_URL = "https://spark-api-open.xf-yun.com/v1/chat/completions";
    private static final String API_KEY = "SPTnokHiCqBTmlZNreTX:ZtxevckEPvsAhCqaTXtV";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PoemCallback {
        void onSuccess(String poem);
        void onError(String error);
    }

    // 原有的生成诗词方法（完全保留，一字未改）
    public void generatePoem(String keywords, String style, PoemCallback callback) {
        try {
            // 构建请求参数
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("max_tokens", 2000);
            jsonBody.put("top_k", 4);
            jsonBody.put("temperature", 0.5);
            jsonBody.put("model", "4.0Ultra");
            jsonBody.put("stream", true);

            JSONArray messages = new JSONArray();

            // 添加系统角色消息
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一个诗词创作助手，请按照以下格式创作诗词：\n");
            promptBuilder.append("1. 第一行是诗词标题，使用《》括起来\n");
            promptBuilder.append("2. 空一行后是诗词原文，保持合适的格式和换行\n");
            promptBuilder.append("3. 空一行后写注释：然后是诗词的译文解释\n");
            promptBuilder.append("注意：\n");
            promptBuilder.append("- 不要使用#号\n");
            promptBuilder.append("- 标题必须用《》括起来\n");
            promptBuilder.append("- 译文要通俗易懂，帮助读者理解诗词意境");

            systemMessage.put("content", promptBuilder.toString());
            messages.put(systemMessage);

            // 添加用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            String userPrompt = "请以\"" + keywords + "\"为主题，创作一首" + style + "。";
            userMessage.put("content", userPrompt);
            messages.put(userMessage);

            jsonBody.put("messages", messages);

            Log.d(TAG, "Request Body: " + jsonBody.toString());

            // 构建请求
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"),
                            jsonBody.toString()))
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed", e);
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String responseBody = response.body().string();
                        mainHandler.post(() -> callback.onError("请求失败: " + response.code() + "\n" + responseBody));
                        return;
                    }

                    // 处理流式响应
                    StringBuilder poemContent = new StringBuilder();
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(responseBody.byteStream(), "UTF-8"));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                try {
                                    // 解析每一行的JSON数据
                                    if (line.startsWith("data:")) {
                                        String jsonStr = line.substring(5).trim(); // 去掉 "data:" 前缀
                                        if (jsonStr.equals("[DONE]")) {
                                            continue;
                                        }

                                        JSONObject json = new JSONObject(jsonStr);
                                        if (json.has("choices")) {
                                            JSONArray choices = json.getJSONArray("choices");
                                            if (choices.length() > 0) {
                                                JSONObject choice = choices.getJSONObject(0);
                                                if (choice.has("delta")) {
                                                    JSONObject delta = choice.getJSONObject("delta");
                                                    if (delta.has("content")) {
                                                        String content = delta.getString("content");
                                                        poemContent.append(content);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Parse line failed: " + line, e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Read response failed", e);
                        mainHandler.post(() -> callback.onError("读取响应失败: " + e.getMessage()));
                        return;
                    }

                    // 返回最终的诗词内容
                    String finalPoem = poemContent.toString().trim();
                    if (finalPoem.isEmpty()) {
                        mainHandler.post(() -> callback.onError("未能生成诗词"));
                    } else {
                        mainHandler.post(() -> callback.onSuccess(finalPoem));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Generate poem failed", e);
            callback.onError("生成诗词失败: " + e.getMessage());
        }
    }

    // ========== 新增方法：使用自定义prompt调用API（不影响原有功能）==========
    /**
     * 通用方法：使用自定义prompt调用API
     * @param customPrompt 自定义的提示词
     * @param callback 回调接口
     */
    public void callApiWithPrompt(String customPrompt, PoemCallback callback) {
        try {
            // 构建请求参数
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("max_tokens", 2000);
            jsonBody.put("top_k", 4);
            jsonBody.put("temperature", 0.5);
            jsonBody.put("model", "4.0Ultra");
            jsonBody.put("stream", true);

            JSONArray messages = new JSONArray();

            // 添加系统角色消息（可以自定义，这里用通用提示）
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的古诗词专家，擅长推荐经典诗词并进行赏析。");
            messages.put(systemMessage);

            // 添加用户消息（使用自定义prompt）
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", customPrompt);
            messages.put(userMessage);

            jsonBody.put("messages", messages);

            Log.d(TAG, "Request Body: " + jsonBody.toString());

            // 构建请求
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"),
                            jsonBody.toString()))
                    .build();

            // 发送请求（后面的处理逻辑和generatePoem完全一样）
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed", e);
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String responseBody = response.body().string();
                        mainHandler.post(() -> callback.onError("请求失败: " + response.code() + "\n" + responseBody));
                        return;
                    }

                    // 处理流式响应
                    StringBuilder content = new StringBuilder();
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(responseBody.byteStream(), "UTF-8"));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                try {
                                    if (line.startsWith("data:")) {
                                        String jsonStr = line.substring(5).trim();
                                        if (jsonStr.equals("[DONE]")) {
                                            continue;
                                        }

                                        JSONObject json = new JSONObject(jsonStr);
                                        if (json.has("choices")) {
                                            JSONArray choices = json.getJSONArray("choices");
                                            if (choices.length() > 0) {
                                                JSONObject choice = choices.getJSONObject(0);
                                                if (choice.has("delta")) {
                                                    JSONObject delta = choice.getJSONObject("delta");
                                                    if (delta.has("content")) {
                                                        content.append(delta.getString("content"));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Parse line failed: " + line, e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Read response failed", e);
                        mainHandler.post(() -> callback.onError("读取响应失败: " + e.getMessage()));
                        return;
                    }

                    String finalContent = content.toString().trim();
                    if (finalContent.isEmpty()) {
                        mainHandler.post(() -> callback.onError("未能生成内容"));
                    } else {
                        mainHandler.post(() -> callback.onSuccess(finalContent));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "API call failed", e);
            callback.onError("调用失败: " + e.getMessage());
        }
    }
}