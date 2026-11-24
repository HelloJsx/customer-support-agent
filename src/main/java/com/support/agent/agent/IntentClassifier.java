package com.support.agent.agent;

public interface IntentClassifier {

    RoutingDecision classify(String message);
}
