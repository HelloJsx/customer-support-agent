package com.support.agent.controller;

import com.support.agent.common.api.ApiResponse;
import com.support.agent.model.dto.chat.Citation;
import com.support.agent.model.dto.knowledge.KnowledgeSearchRequest;
import com.support.agent.model.dto.knowledge.ReindexResult;
import com.support.agent.service.KnowledgeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Knowledge", description = "知识库检索与向量索引")
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/search")
    public ApiResponse<List<Citation>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.ok(knowledgeService.search(request.query(), request.topK()));
    }

    @PostMapping("/reindex")
    public ApiResponse<ReindexResult> reindex() {
        return ApiResponse.ok(knowledgeService.reindex());
    }
}
