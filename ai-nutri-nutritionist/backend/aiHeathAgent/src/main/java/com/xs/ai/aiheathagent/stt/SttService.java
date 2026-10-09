package com.xs.ai.aiheathagent.stt;

import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class SttService {
    @Value("${tencent.secretId:}")
    private String secretId;

    @Value("${tencent.secretKey:}")
    private String secretKey;

    private static String detectVoiceFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return "";
        int b0 = bytes[0] & 0xff;
        int b1 = bytes[1] & 0xff;
        int b2 = bytes[2] & 0xff;
        int b3 = bytes[3] & 0xff;
        String s4 = "" + (char) b0 + (char) b1 + (char) b2 + (char) b3;
        if ("RIFF".equals(s4)) return "wav";
        if ("ID3".equals(s4)) return "mp3";
        if (b0 == 0xff && (b1 & 0xe0) == 0xe0) return "mp3";
        return "";
    }

    private static byte[] wrapPcmToWav(byte[] pcm, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataLen = pcm.length;
        int riffChunkSize = 36 + dataLen;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataLen);
        out.write('R'); out.write('I'); out.write('F'); out.write('F');
        writeLeInt(out, riffChunkSize);
        out.write('W'); out.write('A'); out.write('V'); out.write('E');
        out.write('f'); out.write('m'); out.write('t'); out.write(' ');
        writeLeInt(out, 16);
        writeLeShort(out, 1);
        writeLeShort(out, (short) channels);
        writeLeInt(out, sampleRate);
        writeLeInt(out, byteRate);
        writeLeShort(out, (short) blockAlign);
        writeLeShort(out, (short) bitsPerSample);
        out.write('d'); out.write('a'); out.write('t'); out.write('a');
        writeLeInt(out, dataLen);
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static void writeLeInt(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
        out.write((v >> 16) & 0xff);
        out.write((v >> 24) & 0xff);
    }

    private static void writeLeShort(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
    }

    public String recognize(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "音频为空");
        }

        byte[] bytes = file.getBytes();
        if (bytes == null || bytes.length == 0) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "音频为空");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String voiceFormat = detectVoiceFormat(bytes);
        if (filename.endsWith(".wav")) voiceFormat = "wav";
        else if (filename.endsWith(".mp3")) voiceFormat = "mp3";
        else if (filename.endsWith(".pcm")) voiceFormat = "pcm";
        if (voiceFormat == null || voiceFormat.isEmpty()) voiceFormat = "mp3";
        if (!"wav".equals(voiceFormat) && !"mp3".equals(voiceFormat) && !"pcm".equals(voiceFormat)) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "不支持的音频格式，请使用 wav/mp3/pcm");
        }

        int minBytes = 2048;
        if (bytes.length < minBytes) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "录音太短");
        }
        int maxBytes = 450 * 1024;
        if (bytes.length > maxBytes) {
            throw new BizEx(ErrorCode.PARAM_INVALID, "录音太长，请缩短到 10 秒内");
        }

        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("asr.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        com.tencentcloudapi.asr.v20190614.AsrClient client =
                new com.tencentcloudapi.asr.v20190614.AsrClient(cred, "ap-guangzhou", clientProfile);

        com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest req =
                new com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest();
        req.setSubServiceType(2L);
        req.setEngSerViceType("16k_zh");
        req.setSourceType(1L);
        byte[] voiceBytes = bytes;
        String fn = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean looksLikePcm = "pcm".equals(voiceFormat) || fn.endsWith(".pcm");
        if (looksLikePcm) {
            voiceBytes = wrapPcmToWav(bytes, 16000, 1, 16);
            voiceFormat = "wav";
        }
        req.setVoiceFormat(voiceFormat);
        req.setData(Base64.getEncoder().encodeToString(voiceBytes));
        req.setDataLen((long) voiceBytes.length);

        com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse resp = client.SentenceRecognition(req);
        String result = resp == null ? "" : resp.getResult();
        return result == null ? "" : result.trim();
    }
}
