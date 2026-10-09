package com.xs.ai.aiheathagent.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NutritionMemberResp {
    private Long id;
    private String memberKey;
    private String nickname;
    private String relation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMemberKey() { return memberKey; }
    public void setMemberKey(String memberKey) { this.memberKey = memberKey; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
}

