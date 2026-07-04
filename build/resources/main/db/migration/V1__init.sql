CREATE TABLE obligations (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    category VARCHAR(32) NOT NULL,
    recurrence VARCHAR(32),
    next_payment_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    obligation_id UUID NOT NULL REFERENCES obligations(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_obligations_status_next_payment_date ON obligations(status, next_payment_date);
CREATE INDEX idx_obligations_title_lower_status ON obligations(LOWER(title), status);
CREATE INDEX idx_obligations_category_status_next_payment_date ON obligations(category, status, next_payment_date);
CREATE INDEX idx_payments_obligation_id ON payments(obligation_id);
