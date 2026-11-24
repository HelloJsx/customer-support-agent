package com.support.agent.model.dto.chat;

import com.support.agent.model.enums.AgentType;

public record AgentStepView(AgentType agent, String action, String decision, long latencyMs) {
}
