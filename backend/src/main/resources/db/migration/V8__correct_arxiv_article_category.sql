-- arXiv abs URLs with the publisher's Abstract block are research papers. Correct
-- only records carrying both source-level signals; do not classify from title words.
update articles
set category_id = (select id from categories where slug = 'ai-research')
where url like 'https://arxiv.org/abs/%'
  and description ilike '%Abstract:%'
  and (category_id is null or category_id <> (select id from categories where slug = 'ai-research'))
  and exists (select 1 from categories where slug = 'ai-research');
