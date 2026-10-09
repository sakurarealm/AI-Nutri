package com.xs.ai.aiheathagent.vision;

import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodVisionService {
    private final RestTemplate restTemplate;

    @Value("${baidu.vision.api-key:}")
    private String apiKey;

    @Value("${baidu.vision.secret-key:}")
    private String secretKey;

    @Value("${baidu.vision.token-url:https://aip.baidubce.com/oauth/2.0/token}")
    private String tokenUrl;

    @Value("${baidu.vision.dish-url:https://aip.baidubce.com/rest/2.0/image-classify/v2/dish}")
    private String dishUrl;

    private volatile String cachedAccessToken;
    private volatile long cachedAccessTokenExpireAtMs;

    public List<FoodCandidate> recognizeDish(MultipartFile file) throws Exception {
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "百度识图未配置");
        }
        if (file == null || file.isEmpty()) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "图片为空");
        }
        if (file.getSize() > 8L * 1024L * 1024L) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "图片过大");
        }

        String accessToken = getAccessToken();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "读取图片失败");
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String encoded = URLEncoder.encode(base64, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<>("image=" + encoded, headers);

        Map body;
        try {
            String url = dishUrl + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            body = resp.getBody();
        } catch (RestClientException e) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "百度识图服务异常");
        }
        if (body == null) {
            throw new BizEx(ErrorCode.BIZ_ERROR, "百度识图返回为空");
        }
        Object errorCode = body.get("error_code");
        if (errorCode != null) {
            String msg = String.valueOf(body.getOrDefault("error_msg", "百度识图失败"));
            throw new BizEx(ErrorCode.BIZ_ERROR, msg);
        }
        Object resultObj = body.get("result");
        List<FoodCandidate> out = new ArrayList<>();
        if (!(resultObj instanceof List<?> list)) {
            return out;
        }
        for (Object it : list) {
            if (!(it instanceof Map<?, ?> m)) {
                continue;
            }
            FoodCandidate c = new FoodCandidate();
            Object nameObj = m.get("name");
            c.setName(nameObj == null ? "" : String.valueOf(nameObj));
            c.setProbability(toDouble(m.get("probability")));
            c.setCalorie(toDouble(m.get("calorie")));
            if (c.getName() == null || c.getName().isBlank()) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        String token = cachedAccessToken;
        if (token != null && !token.isBlank() && now < (cachedAccessTokenExpireAtMs - 60_000L)) {
            return token;
        }
        synchronized (this) {
            long now2 = System.currentTimeMillis();
            String token2 = cachedAccessToken;
            if (token2 != null && !token2.isBlank() && now2 < (cachedAccessTokenExpireAtMs - 60_000L)) {
                return token2;
            }
            String url = tokenUrl
                    + "?grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(String.valueOf(apiKey), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(String.valueOf(secretKey), StandardCharsets.UTF_8);
            Map body;
            try {
                ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
                body = resp.getBody();
            } catch (RestClientException e) {
                throw new BizEx(ErrorCode.BIZ_ERROR, "百度鉴权服务异常");
            }
            if (body == null) {
                throw new BizEx(ErrorCode.BIZ_ERROR, "百度鉴权失败");
            }
            Object accessToken = body.get("access_token");
            Object expiresIn = body.get("expires_in");
            if (accessToken == null) {
                String msg = String.valueOf(body.getOrDefault("error_description", "百度鉴权失败"));
                throw new BizEx(ErrorCode.BIZ_ERROR, msg);
            }
            String t = String.valueOf(accessToken);
            long expSec = toLong(expiresIn, 0L);
            cachedAccessToken = t;
            cachedAccessTokenExpireAtMs = System.currentTimeMillis() + Math.max(0L, expSec) * 1000L;
            return t;
        }
    }

    private static Double toDouble(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static long toLong(Object v, long def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}
