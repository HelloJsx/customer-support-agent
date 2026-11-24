# Agent 决策轨迹示例

一次“我要退款，订单 ORD-2026001”的对话，会在 `agent_traces` 中留下如下记录（节选）：

| step | agent | action | decision | latency |
|------|-------|--------|----------|---------|
| 1 | SUPERVISOR | classify_intent | REFUND | 2ms |
| 2 | REFUND | tool_call | checkRefundEligibility(ORD-2026001) | 0ms |
| 3 | REFUND | tool_call | draftRefund(ORD-2026001) | 0ms |
| 4 | REFUND | draft_answer | approval_required | 18ms |
| 5 | REVIEWER | review_answer | approval_required | 1ms |

对应的 `ChatResponse.trace`：

```json
[
  {"agent":"SUPERVISOR","action":"classify_intent","decision":"REFUND","latencyMs":2},
  {"agent":"REFUND","action":"tool_call","decision":"checkRefundEligibility(ORD-2026001)","latencyMs":0},
  {"agent":"REFUND","action":"tool_call","decision":"draftRefund(ORD-2026001)","latencyMs":0},
  {"agent":"REFUND","action":"draft_answer","decision":"approval_required","latencyMs":18},
  {"agent":"REVIEWER","action":"review_answer","decision":"approval_required","latencyMs":1}
]
```

## 审批记录

```json
{
  "approvalNo": "AP20261124-483920",
  "actionType": "REFUND",
  "status": "PENDING",
  "summary": "退款审批：ORD-2026001 / ¥399.00",
  "payload": {
    "refundNo": "R20261124-120394",
    "orderNo": "ORD-2026001",
    "customerNo": "CUST-001",
    "amount": 399.00,
    "currency": "CNY",
    "reason": "用户申请退款：我要退款，订单 ORD-2026001",
    "eligibility": "已签收且在 7 天无理由退货期内"
  }
}
```

管理员审批通过后：

- `approval_requests.status` → `APPROVED`
- `refund_requests.status` → `APPROVED`
- `orders.status` → `REFUNDING`
- `tickets.status` → `RESOLVED`
- 新增一条 `SYSTEM` 消息：“人工审批通过：退款审批：ORD-2026001 / ¥399.00，操作已执行。”

## 被 Reviewer 拦截的反例

如果模型草稿中出现“已退款”且当前没有审批单，Reviewer 会把答案替换为：

> 退款申请尚未执行。退款属于敏感操作，必须经过人工审批后才能处理。

这保证了 Prompt 注入或模型幻觉不会直接造成资金操作或错误承诺。
