package com.xs.ai.aiheathagent.dto.req;

import lombok.Data;

@Data
public class CapabilitySetReq {
    private String capabilityKey;
    private String state;
    private String reason;
}

