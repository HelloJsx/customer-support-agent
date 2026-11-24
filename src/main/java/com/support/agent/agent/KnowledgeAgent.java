package com.support.agent.agent;

import com.support.agent.config.PromptLoader;
import com.support.agent.model.dto.chat.Citation;
import com.support.agent.model.enums.AgentType;
import com.support.agent.service.KnowledgeService;
import com.support.agent.service.LlmClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class KnowledgeAgent implements SupportAgent {

    private final KnowledgeService knowledgeService;
    private final LlmClient llmClient;
    private final PromptLoader promptLoader;

    @Override
    public AgentResponse handle(AgentContext context) {
        List<Citation> citations = knowledgeService.search(context.getUserMessage(), 4);
        if (citations.isEmpty()) {
            return AgentResponse.builder()
                    .agent(AgentType.KNOWLEDGE)
                    .content("抱歉，我暂时没有在知识库中找到与您问题直接相关的资料。"
                            + "您可以补充更多细节，或回复“转人工”由人工客服协助。")
                    .citations(List.of())
                    .toolsUsed(List.of("knowledgeSearch"))
                    .build();
        }

        String fallback = composeExtractiveAnswer(citations);
        String answer = fallback;
        if (llmClient.available()) {
            String systemPrompt = promptLoader.get("knowledge") + "\n\n检索资料：\n" + formatContext(citations);
            answer = llmClient.complete(systemPrompt, context.getHistory())
                    .filter(StringUtils::hasText)
                    .orElse(fallback);
        }

        return AgentResponse.builder()
                .agent(AgentType.KNOWLEDGE)
                .content(answer)
                .citations(citations)
                .toolsUsed(List.of("knowledgeSearch"))
                .build();
    }

    private String composeExtractiveAnswer(List<Citation> citations) {
        StringBuilder answer = new StringBuilder("根据知识库资料：\n\n");
        int limit = Math.min(2, citations.size());
        for (int i = 0; i < limit; i++) {
            Citation citation = citations.get(i);
            answer.append(citation.snippet()).append("\n\n");
        }
        answer.append("参考资料：");
        for (int i = 0; i < citations.size(); i++) {
            Citation citation = citations.get(i);
            if (i > 0) {
                answer.append("；");
            }
            answer.append("[").append(i + 1).append("] ").append(citation.title())
                    .append("（").append(citation.source()).append("）");
        }
        return answer.toString();
    }

    private String formatContext(List<Citation> citations) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < citations.size(); i++) {
            Citation citation = citations.get(i);
            context.append("[").append(i + 1).append("] 标题：").append(citation.title())
                    .append("，来源：").append(citation.source()).append("\n")
                    .append(citation.snippet()).append("\n\n");
        }
        return context.toString();
    }
}
