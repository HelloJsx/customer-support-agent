package com.support.agent.model.dto.chat;

/**
 * A citation returned by the knowledge agent, pointing at the source document.
 */
public record Citation(String source, String title, String snippet, double score) {
}
