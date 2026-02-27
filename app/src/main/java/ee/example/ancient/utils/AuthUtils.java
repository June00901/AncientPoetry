//package ee.example.ancient.utils;
//
////功能：提供生成认证URL的工具类。
////        主要功能：
////        生成RFC1123格式的日期。
////        创建用于API请求的签名，使用HMAC-SHA256算法。
////        生成包含认证信息的URL，便于进行API调用。
//
//import android.util.Base64;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//import java.util.TimeZone;
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//
//public class AuthUtils {
//    private static final String HMAC_SHA256 = "HmacSHA256";
//
//    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) {
//        try {
//            // 生成 RFC1123 格式的日期
//            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
//            format.setTimeZone(TimeZone.getTimeZone("GMT"));
//            String date = format.format(new Date());
//
//            // 修改签名原文格式
//            String signOrigin = "host: " + hostUrl + "\n" +
//                              "date: " + date + "\n" +
//                              "POST /v3.1/chat HTTP/1.1";
//
//            Mac mac = Mac.getInstance(HMAC_SHA256);
//            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
//            mac.init(spec);
//            byte[] hexDigits = mac.doFinal(signOrigin.getBytes(StandardCharsets.UTF_8));
//            String signature = Base64.encodeToString(hexDigits, Base64.NO_WRAP);
//
//            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"host date request-line\", signature=\"%s\"",
//                    apiKey, "hmac-sha256", signature);
//            String authBase64 = Base64.encodeToString(authorization.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
//
//            // 修改URL格式，使用 RFC1123 格式的日期
//            return "https://" + hostUrl + "/v3.1/chat" +
//                   "?authorization=" + authBase64 +
//                   "&date=" + java.net.URLEncoder.encode(date, "UTF-8") +
//                   "&host=" + hostUrl;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//}