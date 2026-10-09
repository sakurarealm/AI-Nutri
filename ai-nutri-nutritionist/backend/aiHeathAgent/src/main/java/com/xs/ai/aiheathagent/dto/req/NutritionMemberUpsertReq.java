package com.xs.ai.aiheathagent.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NutritionMemberUpsertReq {
    private String memberKey;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "关系不能为空")
    private String relation;

    public String getMemberKey() { return memberKey; }
    public void setMemberKey(String memberKey) { this.memberKey = memberKey; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
}

