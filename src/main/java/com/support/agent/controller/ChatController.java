package com.support.agent.controller;

import com.support.agent.agent.AgentOrchestrator;
import com.support.agent.common.api.ApiResponse;
import com.support.agent.model.dto.chat.ChatRequest;
import com.support.agent.model.dto.chat.ChatResponse;
import com.support.agent.model.dto.chat.MessageView;
import com.support.agent.service.ConversationMemoryService;
import com.support.agent.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "多轮对话入口")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AgentOrchestrator orchestrator;
    private final TicketService ticketService;
    private final ConversationMemoryService memoryService;

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(orchestrator.chat(request));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<MessageView>> history(@PathVariable String conversationId) {
        return ApiResponse.ok(ticketService.messages(conversationId));
    }

    @DeleteMapping("/{conversationId}/memory")
    public ApiResponse<Void> clearMemory(@PathVariable String conversationId) {
        memoryService.clear(conversationId);
        return ApiResponse.ok(null);
    }
}
