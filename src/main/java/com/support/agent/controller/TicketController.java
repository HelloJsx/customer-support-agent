package com.support.agent.controller;

import com.support.agent.common.api.ApiResponse;
import com.support.agent.common.api.PageResult;
import com.support.agent.model.dto.chat.AgentStepView;
import com.support.agent.model.dto.ticket.CreateTicketRequest;
import com.support.agent.model.dto.ticket.TicketDetailResponse;
import com.support.agent.model.dto.ticket.TicketResponse;
import com.support.agent.model.dto.ticket.UpdateTicketStatusRequest;
import com.support.agent.model.enums.TicketStatus;
import com.support.agent.service.AgentTraceService;
import com.support.agent.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tickets", description = "工单查询与人工处理")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService ticketService;
    private final AgentTraceService agentTraceService;

    @GetMapping
    public ApiResponse<PageResult<TicketResponse>> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String customerNo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(PageResult.from(ticketService.list(status, customerNo, pageable)));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.ok(ticketService.statusCounts());
    }

    @GetMapping("/{ticketNo}")
    public ApiResponse<TicketDetailResponse> detail(@PathVariable String ticketNo) {
        return ApiResponse.ok(ticketService.detail(ticketNo));
    }

    @PostMapping
    public ApiResponse<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.created(TicketResponse.from(ticketService.createTicket(request)));
    }

    @PatchMapping("/{ticketNo}/status")
    public ApiResponse<TicketResponse> updateStatus(
            @PathVariable String ticketNo,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ApiResponse.ok(TicketResponse.from(ticketService.updateStatus(ticketNo, request)));
    }

    @GetMapping("/{ticketNo}/traces")
    public ApiResponse<List<AgentStepView>> traces(@PathVariable String ticketNo) {
        return ApiResponse.ok(agentTraceService.byTicket(ticketService.getByTicketNo(ticketNo).getId()));
    }
}
