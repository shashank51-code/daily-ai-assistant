-- The publisher tags this article as AI, but it is not a startup story.
-- Keep it in the existing general AI news category rather than the TechCrunch feed fallback.
update articles a
set category_id = c.id
from categories c
where a.url = 'https://techcrunch.com/2026/11/16/all-the-questions-nvidia-should-answer-on-tuesday-but-probably-wont/'
  and c.slug = 'latest-ai-news';
