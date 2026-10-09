package com.xs.ai.aiheathagent.dto.resp;


import lombok.*;
import java.util.List;


@Data @AllArgsConstructor @NoArgsConstructor
public class PageResp<T> {
    private long total;
    private List<T> list;
}
