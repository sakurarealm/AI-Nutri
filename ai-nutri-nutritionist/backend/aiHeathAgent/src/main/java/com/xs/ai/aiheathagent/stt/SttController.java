package com.xs.ai.aiheathagent.stt;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SttController {
    private final SttService sttService;

    @PostMapping(
            value = {"/api/stt", "/api/stt/", "/api/api/stt", "/api/api/stt/", "/stt", "/stt/"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> recognize(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @NotNull @RequestPart("file") MultipartFile file
    ) throws Exception {
        String text = sttService.recognize(file);
        Map<String, Object> out = new HashMap<>();
        out.put("text", text == null ? "" : text);
        return out;
    }
}

