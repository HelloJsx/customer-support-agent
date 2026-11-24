package com.support.agent.agent;

import com.support.agent.model.enums.Intent;

public record RoutingDecision(Intent intent, double confidence, String reason) {
}
