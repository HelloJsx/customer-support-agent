package com.support.agent.controller;

import com.support.agent.common.api.ApiResponse;
import com.support.agent.common.api.PageResult;
import com.support.agent.model.dto.approval.ApprovalDecisionRequest;
import com.support.agent.model.dto.approval.ApprovalView;
import com.support.agent.model.enums.ApprovalStatus;
import com.support.agent.service.ApprovalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Approvals", description = "敏感操作人工审批")
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Validated
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping
    public ApiResponse<PageResult<ApprovalView>> list(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResult.from(approvalService.list(status, pageable)));
    }

    @GetMapping("/{approvalNo}")
    public ApiResponse<ApprovalView> get(@PathVariable String approvalNo) {
        return ApiResponse.ok(approvalService.get(approvalNo));
    }

    @PostMapping("/{approvalNo}/decision")
    public ApiResponse<ApprovalView> decide(
            @PathVariable String approvalNo,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return ApiResponse.ok(approvalService.decide(approvalNo, request));
    }
}
