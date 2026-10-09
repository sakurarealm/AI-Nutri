package com.xs.ai.aiheathagent.conversation.service;

import com.xs.ai.aiheathagent.conversation.model.ConversationInput;
import com.xs.ai.aiheathagent.conversation.model.UserEvent;

public interface EventDetectService {
    UserEvent detectAndPersist(ConversationInput input);
}
