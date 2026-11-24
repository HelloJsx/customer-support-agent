CREATE TABLE customers (
    id UUID NOT NULL,
    customer_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_customer_no UNIQUE (customer_no)
);

CREATE TABLE orders (
    id UUID NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    customer_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    item_summary TEXT,
    tracking_no VARCHAR(64),
    carrier VARCHAR(64),
    paid_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_no UNIQUE (order_no)
);

CREATE TABLE conversations (
    id VARCHAR(64) NOT NULL,
    customer_no VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_conversations PRIMARY KEY (id)
);

CREATE TABLE tickets (
    id UUID NOT NULL,
    ticket_no VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    customer_no VARCHAR(64) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    intent VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    assignee VARCHAR(64),
    resolution TEXT,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tickets PRIMARY KEY (id),
    CONSTRAINT uk_tickets_ticket_no UNIQUE (ticket_no)
);

CREATE TABLE ticket_messages (
    id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    message_role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    citations TEXT,
    agent_name VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_ticket_messages PRIMARY KEY (id)
);

CREATE TABLE agent_traces (
    id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    step_no INTEGER NOT NULL,
    agent VARCHAR(32) NOT NULL,
    action VARCHAR(128) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    decision VARCHAR(64),
    latency_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_agent_traces PRIMARY KEY (id)
);

CREATE TABLE approval_requests (
    id UUID NOT NULL,
    approval_no VARCHAR(64) NOT NULL,
    ticket_id UUID NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    payload TEXT,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    decided_by VARCHAR(64),
    decided_at TIMESTAMP WITH TIME ZONE,
    decision_comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_approval_requests PRIMARY KEY (id),
    CONSTRAINT uk_approval_requests_no UNIQUE (approval_no)
);

CREATE TABLE refund_requests (
    id UUID NOT NULL,
    refund_no VARCHAR(64) NOT NULL,
    ticket_id UUID NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    customer_no VARCHAR(64) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    reason TEXT,
    status VARCHAR(32) NOT NULL,
    approval_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_refund_requests PRIMARY KEY (id),
    CONSTRAINT uk_refund_requests_no UNIQUE (refund_no)
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_no ON orders (customer_no);
CREATE INDEX IF NOT EXISTS idx_conversations_customer_no ON conversations (customer_no);
CREATE INDEX IF NOT EXISTS idx_tickets_conversation_id ON tickets (conversation_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets (status);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_no ON tickets (customer_no);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON ticket_messages (ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_messages_conversation_id ON ticket_messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_traces_ticket_id ON agent_traces (ticket_id);
CREATE INDEX IF NOT EXISTS idx_agent_traces_conversation_id ON agent_traces (conversation_id);
CREATE INDEX IF NOT EXISTS idx_approval_requests_status ON approval_requests (status);
CREATE INDEX IF NOT EXISTS idx_approval_requests_ticket_id ON approval_requests (ticket_id);
CREATE INDEX IF NOT EXISTS idx_refund_requests_approval_id ON refund_requests (approval_id);
CREATE INDEX IF NOT EXISTS idx_refund_requests_order_no ON refund_requests (order_no);
