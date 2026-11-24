package com.support.agent.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.agent.agent.AgentOrchestrator;
import com.support.agent.model.dto.chat.ChatRequest;
import com.support.agent.model.dto.chat.ChatResponse;
import com.support.agent.model.dto.evaluation.EvaluationCaseResult;
import com.support.agent.model.dto.evaluation.EvaluationReport;
import com.support.agent.model.entity.CustomerOrder;
import com.support.agent.model.enums.OrderStatus;
import com.support.agent.repository.CustomerOrderRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

/**
 * Offline evaluation over a fixed 25-question dataset. The same runner powers both the REST
 * endpoint and the acceptance test, so routing regressions are caught in CI.
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);
    private static final String DEFAULT_CUSTOMER = "CUST-001";

    private final AgentOrchestrator orchestrator;
    private final CustomerOrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public EvaluationReport run() {
        prepareEvaluationOrders();
        List<EvaluationCase> cases = loadCases();
        List<EvaluationCaseResult> results = new ArrayList<>();

        int intentPass = 0;
        int toolTotal = 0;
        int toolPass = 0;
        int approvalPass = 0;
        int allPass = 0;

        for (EvaluationCase evalCase : cases) {
            try {
                ChatResponse response = orchestrator.chat(new ChatRequest(
                        DEFAULT_CUSTOMER,
                        "eval-" + evalCase.id() + "-" + UUID.randomUUID(),
                        evalCase.message()));

                boolean intentPassed = response.intent() == evalCase.expectedIntent();
                boolean expectsTool = StringUtils.hasText(evalCase.expectedTool());
                boolean toolInvoked = !expectsTool || response.toolsUsed().stream()
                        .anyMatch(tool -> tool.startsWith(evalCase.expectedTool()));
                boolean approvalPassed = response.requiresApproval() == evalCase.expectApproval();
                boolean passed = intentPassed && toolInvoked && approvalPassed;

                intentPass += intentPassed ? 1 : 0;
                if (expectsTool) {
                    toolTotal++;
                    toolPass += toolInvoked ? 1 : 0;
                }
                approvalPass += approvalPassed ? 1 : 0;
                allPass += passed ? 1 : 0;

                results.add(new EvaluationCaseResult(
                        evalCase.id(),
                        evalCase.message(),
                        evalCase.expectedIntent(),
                        response.intent(),
                        response.confidence(),
                        intentPassed,
                        evalCase.expectedTool(),
                        toolInvoked,
                        evalCase.expectApproval(),
                        response.requiresApproval(),
                        passed,
                        preview(response.answer())));
            } catch (Exception ex) {
                log.warn("Evaluation case {} failed: {}", evalCase.id(), ex.getMessage());
                results.add(new EvaluationCaseResult(
                        evalCase.id(), evalCase.message(), evalCase.expectedIntent(), null,
                        0.0, false, evalCase.expectedTool(), false, evalCase.expectApproval(), false,
                        false, "ERROR: " + ex.getMessage()));
            }
        }

        int total = cases.size();
        EvaluationReport report = new EvaluationReport(
                Instant.now(),
                total,
                rate(intentPass, total),
                rate(toolPass, toolTotal),
                rate(approvalPass, total),
                rate(allPass, total),
                results);
        // Evaluation must be repeatable and must not pollute business data.
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return report;
    }

    private void prepareEvaluationOrders() {
        Instant now = Instant.now();
        orderRepository.findByOrderNo("ORD-2026001").ifPresent(order -> {
            order.setStatus(OrderStatus.DELIVERED);
            order.setPaidAt(now.minus(Duration.ofDays(5)));
            order.setDeliveredAt(now.minus(Duration.ofDays(2)));
            orderRepository.save(order);
        });
        orderRepository.findByOrderNo("ORD-2026002").ifPresent(order -> {
            order.setStatus(OrderStatus.SHIPPED);
            order.setDeliveredAt(null);
            orderRepository.save(order);
        });
        orderRepository.findByOrderNo("ORD-2026003").ifPresent(order -> {
            order.setStatus(OrderStatus.PAID);
            order.setDeliveredAt(null);
            orderRepository.save(order);
        });
        orderRepository.findByOrderNo("ORD-2026004").ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        });
    }

    private List<EvaluationCase> loadCases() {
        ClassPathResource resource = new ClassPathResource("evaluation/cases.jsonl");
        try {
            List<String> lines = resource.getContentAsString(StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .filter(line -> !line.startsWith("#"))
                    .toList();
            List<EvaluationCase> cases = new ArrayList<>();
            for (String line : lines) {
                cases.add(objectMapper.readValue(line, EvaluationCase.class));
            }
            return cases;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load evaluation dataset", ex);
        }
    }

    private double rate(int passed, int total) {
        if (total == 0) {
            return 1.0;
        }
        return Math.round((double) passed * 10000.0 / total) / 10000.0;
    }

    private String preview(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        return answer.length() <= 120 ? answer : answer.substring(0, 120) + "...";
    }
}
