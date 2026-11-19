-- Phase 8.2: durable, provider-independent daily attempt ledger.
-- Additive only. No existing table or row is modified.
create table ai_usage_daily (
    usage_date    date primary key,
    attempts      integer not null default 0,
    successes     integer not null default 0,
    failures      integer not null default 0,
    input_tokens  bigint not null default 0,
    output_tokens bigint not null default 0,
    created_at    timestamp with time zone not null default now(),
    updated_at    timestamp with time zone not null default now(),
    constraint ck_ai_usage_nonnegative check (
        attempts >= 0 and successes >= 0 and failures >= 0
        and input_tokens >= 0 and output_tokens >= 0
    )
);
