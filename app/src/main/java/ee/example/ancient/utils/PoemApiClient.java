package ee.example.ancient.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class PoemApiClient {
    private static final String TAG = "PoemApiClient";
    private static final String API_URL = "https://spark-api-open.xf-yun.com/v1/chat/completions";
    private static final String API_KEY = "SPTnokHiCqBTmlZNreTX:ZtxevckEPvsAhCqaTXtV";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PoemCallback {
        void onSuccess(String poem);
        void onError(String error);
        void onStream(String partialContent); // 实时流式更新
    }

    // ========== 原生成诗词方法 ==========
    public void generatePoem(String keywords, String style, PoemCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("max_tokens", 2000);
            jsonBody.put("top_k", 4);
            jsonBody.put("temperature", 0.5);
            jsonBody.put("model", "4.0Ultra");
            jsonBody.put("stream", true);

            JSONArray messages = new JSONArray();

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

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            String userPrompt = "请以\"" + keywords + "\"为主题，创作一首" + style + "。";
            userMessage.put("content", userPrompt);
            messages.put(userMessage);

            jsonBody.put("messages", messages);

            Log.d(TAG, "Request Body: " + jsonBody.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"),
                            jsonBody.toString()))
                    .build();

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

                    StringBuilder poemContent = new StringBuilder();
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(responseBody.byteStream(), "UTF-8"));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                try {
                                    if (line.startsWith("data:")) {
                                        String jsonStr = line.substring(5).trim();
                                        if (jsonStr.equals("[DONE]")) continue;

                                        JSONObject json = new JSONObject(jsonStr);
                                        if (json.has("choices")) {
                                            JSONArray choices = json.getJSONArray("choices");
                                            if (choices.length() > 0) {
                                                JSONObject choice = choices.getJSONObject(0);
                                                if (choice.has("delta") && choice.getJSONObject("delta").has("content")) {
                                                    String partialContent = choice.getJSONObject("delta").getString("content");
                                                    poemContent.append(partialContent);
                                                    // 实时流式更新
                                                    mainHandler.post(() -> callback.onStream(poemContent.toString()));
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

    // ========== 通用自定义Prompt调用方法 ==========
    public void callApiWithPrompt(String customPrompt, PoemCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("max_tokens", 2000);
            jsonBody.put("top_k", 4);
            jsonBody.put("temperature", 0.5);
            jsonBody.put("model", "4.0Ultra");
            jsonBody.put("stream", true);

            JSONArray messages = new JSONArray();

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的古诗词专家，擅长推荐经典诗词并进行赏析。");
            messages.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", customPrompt);
            messages.put(userMessage);

            jsonBody.put("messages", messages);

            Log.d(TAG, "Request Body: " + jsonBody.toString());

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"),
                            jsonBody.toString()))
                    .build();

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
                                        if (jsonStr.equals("[DONE]")) continue;

                                        JSONObject json = new JSONObject(jsonStr);
                                        if (json.has("choices")) {
                                            JSONArray choices = json.getJSONArray("choices");
                                            if (choices.length() > 0) {
                                                JSONObject choice = choices.getJSONObject(0);
                                                if (choice.has("delta") && choice.getJSONObject("delta").has("content")) {
                                                    String partialContent = choice.getJSONObject("delta").getString("content");
                                                    content.append(partialContent);
                                                    // 实时流式更新
                                                    mainHandler.post(() -> callback.onStream(content.toString()));
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

    // ========== 诗词评分专用方法（带稳定性哈希） ==========
    public void scorePoem(String poemType, String background, String poemContent,
                          String poemHash, PoemCallback callback) {
        String prompt = buildScoringPrompt(poemType, background, poemContent, poemHash);
        callApiWithPrompt(prompt, callback);
    }

    // ========== 修改后的 buildScoringPrompt 方法（增加格式检查） ==========
    private String buildScoringPrompt(String type, String background, String poem, String hash) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是古诗词评审专家，对以下").append(type).append("进行专业评分。\n");
        if (!background.isEmpty()) {
            sb.append("创作背景：").append(background).append("\n");
        }
        sb.append("诗词内容：").append(poem).append("\n");

        // 格式检查部分
        sb.append("\n【第一步：格式检查】\n");
        sb.append("请判断输入内容是否符合诗词基本规范：\n");
        sb.append("1. 是否使用中文（必须包含中文字符）\n");
        sb.append("2. 是否符合").append(type).append("的基本格式：\n");
        sb.append("   - 五言绝句：4句，每句5字\n");
        sb.append("   - 七言绝句：4句，每句7字\n");
        sb.append("   - 五言律诗：8句，每句5字\n");
        sb.append("   - 七言律诗：8句，每句7字\n");
        sb.append("   - 词牌：符合该词牌的基本句式和字数\n");
        sb.append("   - 现代诗：自由格式，但应有诗意\n");
        sb.append("   - 古体诗：格式自由，但应有古风\n");
        sb.append("3. 是否有基本的押韵或平仄意识\n");
        sb.append("4. 内容是否完整，是否有意义\n\n");

        sb.append("评分维度（每项满分20分）：格律合规、意境营造、炼字用词、情感真挚、创意创新。\n");

        sb.append("【输出要求】\n");
        sb.append("请严格按照以下JSON格式返回，不要有任何其他文字：\n");
        sb.append("{\n");
        sb.append("  \"valid\": true,  // true=符合诗词规范，false=不符合\n");
        sb.append("  \"format_message\": \"\",  // 格式提示信息，如果valid为false，这里说明具体问题\n");
        sb.append("  \"total_score\": 85,\n");
        sb.append("  \"level\": \"佳作\",\n");
        sb.append("  \"dimensions\": [\n");
        sb.append("    {\"name\": \"格律\", \"score\": 18, \"comment\": \"格律工整\"},\n");
        sb.append("    {\"name\": \"意境\", \"score\": 17, \"comment\": \"画面清新\"},\n");
        sb.append("    {\"name\": \"用词\", \"score\": 15, \"comment\": \"用词精准\"},\n");
        sb.append("    {\"name\": \"情感\", \"score\": 18, \"comment\": \"情感真挚\"},\n");
        sb.append("    {\"name\": \"创新\", \"score\": 14, \"comment\": \"立意常规\"}\n");
        sb.append("  ],\n");
        sb.append("  \"overall_comment\": \"总体评价\",\n");
        sb.append("  \"suggestions\": \"修改建议\"\n");
        sb.append("}\n\n");

        sb.append("注意：如果valid为false，total_score可以给一个基础分（如30-50分），并在overall_comment和suggestions中给出改进建议。");

        return sb.toString();
    }
}