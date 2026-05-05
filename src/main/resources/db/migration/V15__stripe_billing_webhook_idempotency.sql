ALTER TABLE credit_ledger_entries
    ADD COLUMN IF NOT EXISTS stripe_event_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_credit_ledger_stripe_event_id
    ON credit_ledger_entries(stripe_event_id)
    WHERE stripe_event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_billing_subscriptions_stripe_customer
    ON billing_subscriptions(stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_billing_subscriptions_stripe_subscription
    ON billing_subscriptions(stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;
