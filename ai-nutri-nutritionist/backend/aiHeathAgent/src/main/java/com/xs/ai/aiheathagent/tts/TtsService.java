package com.xs.ai.aiheathagent.tts;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class TtsService {
    @Value("${tencent.secretId:}")
    private String secretId;
    @Value("${tencent.secretKey:}")
    private String secretKey;

    public byte[] synthesize(String text) throws Exception {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("TTS_TEXT_EMPTY");
        }
        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("tts.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        TtsClient client = new TtsClient(cred, "ap-guangzhou", clientProfile);

        TextToVoiceRequest req = new TextToVoiceRequest();
        req.setText(t);
        req.setSessionId(String.valueOf(System.currentTimeMillis()));
        req.setModelType(1L);  // Long
        req.setVoiceType(601000L);  // Long
        req.setSpeed(0.0f);  // float
        req.setVolume(0.0f);  // float
        req.setPrimaryLanguage(1L);  // Long
        req.setSampleRate(16000L);  // Long
        req.setCodec("mp3");

        // 添加情感参数（针对多情感音色，必加以避免参数校验失败）


        TextToVoiceResponse resp = client.TextToVoice(req);
        String base64Audio = resp.getAudio();
        if (base64Audio == null || base64Audio.isEmpty()) {
            throw new IllegalStateException("TTS_EMPTY");
        }
        return Base64.getDecoder().decode(base64Audio);
    }
}
