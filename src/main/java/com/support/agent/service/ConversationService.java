package com.support.agent.service;

import com.support.agent.common.exception.BusinessException;
import com.support.agent.model.entity.Conversation;
import com.support.agent.model.enums.ConversationStatus;
import com.support.agent.repository.ConversationRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Transactional
    public Conversation getOrCreate(String conversationId, String customerNo) {
        if (StringUtils.hasText(conversationId)) {
            return conversationRepository.findById(conversationId)
                    .map(existing -> {
                        if (!existing.getCustomerNo().equals(customerNo)) {
                            throw BusinessException.badRequest("会话不属于当前客户");
                        }
                        return existing;
                    })
                    .orElseGet(() -> create(conversationId, customerNo));
        }
        return create(UUID.randomUUID().toString(), customerNo);
    }

    @Transactional
    public void touch(String conversationId, String title) {
        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (!StringUtils.hasText(conversation.getTitle()) && StringUtils.hasText(title)) {
                conversation.setTitle(truncate(title, 100));
            }
            conversation.setLastMessageAt(Instant.now());
        });
    }

    public Conversation get(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在: " + conversationId));
    }

    private Conversation create(String id, String customerNo) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setCustomerNo(customerNo);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setLastMessageAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
