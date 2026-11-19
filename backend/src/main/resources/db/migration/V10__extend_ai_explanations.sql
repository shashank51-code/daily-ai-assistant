-- Phase 8: source-grounded, provider-independent AI explanation metadata.
-- Additive only. Existing rows and the BASIC fallback remain valid.
alter table article_explanations add column who_benefits text;
alter table article_explanations add column use_cases text;
alter table article_explanations add column technical_explanation text;
alter table article_explanations add column claims_json text;
alter table article_explanations add column source_evidence text;
alter table article_explanations add column provider varchar(50);
alter table article_explanations add column model varchar(100);
alter table article_explanations add column input_tokens integer;
alter table article_explanations add column output_tokens integer;
alter table article_explanations add column latency_ms integer;
alter table article_explanations add column ai_attempted_at timestamp with time zone;
alter table article_explanations add column ai_attempts integer not null default 0;
