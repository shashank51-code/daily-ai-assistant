<#
.SYNOPSIS
    SQL statements used by Invoke-NeonTargetValidation.ps1.

.DESCRIPTION
    Kept in its own file so the statements can be inspected and tested without a
    database connection, a Neon credential, or a PostgreSQL client.

    Every statement targets PostgreSQL 16 and is written so the parser can pick
    exactly one operator for each expression. Two classes of implicit cast are
    avoided on purpose:

      * Catalog columns typed "char" (c.relkind, t.typtype, p.prokind, ...) are
        never fed to the || operator directly. text || "char" is ambiguous in
        PostgreSQL: both text || text and anynonarray || text match once the
        "char" operand is implicitly cast, so the parser raises
        'operator is not unique: text || "char"'. These columns are passed
        through concat(), which is variadic over "any" and therefore cannot be
        ambiguous, or are cast with ::text first.
      * boolean and name/varchar/domain columns are cast with ::text before
        concatenation, because boolean has no implicit cast to text at all
        ('operator does not exist: boolean || text') and name only resolves by
        implicit coercion.

    concat() also treats NULL as the empty string, while || propagates NULL.
    Queries whose row presence matters (Flyway history) keep || with explicit
    casts so a NULL column still yields a NULL row, as before.
#>

function Get-NeonSqlEmptinessObjects {
    # Empty-target gate. One row per non-system relation, function and type.
    # Labels are built with concat() so "char" columns (relkind, typtype) never
    # reach the || operator uncast.
    return @'
select concat('relation:', n.nspname, '.', c.relname, ' [', c.relkind, ']')
from pg_class c join pg_namespace n on n.oid = c.relnamespace
where n.nspname not in ('pg_catalog','information_schema','pg_toast')
  and n.nspname not like 'pg_temp%' and n.nspname not like 'pg_toast_temp%'
  and c.relkind in ('r','p','v','m','S','f')
union all
select concat('function:', n.nspname, '.', p.proname)
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname not in ('pg_catalog','information_schema','pg_toast')
  and n.nspname not like 'pg_temp%' and n.nspname not like 'pg_toast_temp%'
union all
select concat('type:', n.nspname, '.', t.typname)
from pg_type t join pg_namespace n on n.oid = t.typnamespace
where n.nspname not in ('pg_catalog','information_schema','pg_toast')
  and n.nspname not like 'pg_temp%' and n.nspname not like 'pg_toast_temp%'
  and t.typtype in ('e','d','r')
order by 1
'@
}

function Get-NeonSqlTableCount {
    # Application tables only. The backup TOC carries 13 tables at V11: the 12
    # application tables plus flyway_schema_history, so Flyway's bookkeeping
    # table is excluded here. No concatenation is involved.
    return @'
select count(*)
from pg_class c join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public' and c.relkind in ('r','p')
  and c.relname <> 'flyway_schema_history'
'@
}

function Get-NeonSqlLatestFlyway {
    return 'select version from flyway_schema_history order by installed_rank desc limit 1'
}

function Get-NeonSqlFlywayFailures {
    return 'select count(*) from flyway_schema_history where success is not true'
}

function Get-NeonSqlRssTableExists {
    return "select to_regclass('public.rss_refresh_runs') is not null"
}

function Get-NeonSqlServerIdentity {
    # Eight pipe-separated fields: address, port, database, user, version,
    # compute-side ssl, compute-side TLS version, compute-side cipher.
    # host() strips the inet netmask ("169.254.254.254/32" can never appear in a
    # DNS answer). pg_stat_ssl is diagnostic only: on Neon it describes the
    # proxy->compute leg, so it cannot attest this client's encryption.
    return @'
select concat(
           coalesce(host(inet_server_addr()), 'unix-socket'), '|',
           coalesce(inet_server_port()::text, '0'), '|',
           current_database()::text, '|',
           current_user::text, '|',
           current_setting('server_version')::text, '|',
           coalesce((select ssl::text from pg_stat_ssl where pid = pg_backend_pid()), 'unknown'), '|',
           coalesce((select version from pg_stat_ssl where pid = pg_backend_pid()), '-'), '|',
           coalesce((select cipher from pg_stat_ssl where pid = pg_backend_pid()), '-'))
'@
}

function Get-NeonSqlRowCounts {
    return @'
select 'users', count(*) from users
union all select 'categories', count(*) from categories
union all select 'articles', count(*) from articles
union all select 'article_explanations', count(*) from article_explanations
union all select 'bookmarks', count(*) from bookmarks
union all select 'learning_topics', count(*) from learning_topics
union all select 'learning_progress', count(*) from learning_progress
union all select 'ai_resources', count(*) from ai_resources
union all select 'ai_usage_daily', count(*) from ai_usage_daily
union all select 'reading_history', count(*) from reading_history
union all select 'daily_tasks', count(*) from daily_tasks
union all select 'daily_task_progress', count(*) from daily_task_progress
order by 1
'@
}

function Get-NeonSqlFlywayHistory {
    # success is boolean: it has no implicit cast to text, so it is cast
    # explicitly. || (not concat) is kept so a NULL version still yields a NULL
    # row and the row-presence semantics of the Verify phase stay unchanged.
    return 'select version::text || ''|'' || success::text || ''|'' || description::text from flyway_schema_history order by installed_rank'
}

function Get-NeonSqlRssColumns {
    # information_schema.columns exposes sql_identifier/character_data domains
    # over name/varchar, so the values are cast to text before concatenation.
    return @'
select column_name::text || ' ' || data_type::text
       || case when is_nullable::text = 'NO' then ' not null' else ''::text end
from information_schema.columns
where table_schema = 'public' and table_name = 'rss_refresh_runs'
order by ordinal_position
'@
}

function Get-NeonSqlRssIndexes {
    return @'
select indexname::text || ' :: ' || indexdef::text
from pg_indexes where schemaname = 'public' and tablename = 'rss_refresh_runs'
order by indexname
'@
}

function Get-NeonSqlRssRefreshCount {
    return 'select count(*) from rss_refresh_runs'
}

function Get-NeonSqlFlywayVersionSequence {
    # Applied version order as a single comma-separated string. Pairing this with
    # the latest-version and failure-count checks proves V1..V11 are all present
    # and successful in order; a missing, duplicated or NULL-version row changes
    # the string. version is varchar, so it is cast before aggregation.
    return @'
select coalesce(string_agg(version::text, ',' order by installed_rank), '')
from flyway_schema_history
where version is not null
'@
}
