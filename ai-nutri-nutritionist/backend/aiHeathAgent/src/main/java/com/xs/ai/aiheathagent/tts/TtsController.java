package com.xs.ai.aiheathagent.tts;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {
    private final TtsService ttsService;

    @PostMapping
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody TtsRequest request) throws Exception {
        byte[] audio = ttsService.synthesize(request.getText());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("audio/mpeg"));
        headers.set("Cache-Control", "no-store");
        headers.setContentLength(audio.length);
        return new ResponseEntity<>(audio, headers, HttpStatus.OK);
    }

    @Data
    public static class TtsRequest {
        @NotBlank
        private String text;
    }
}
