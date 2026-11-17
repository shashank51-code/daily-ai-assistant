-- Seven day starter plan for freshers. Each day mixes theory, practice, tool exploration and building.

insert into daily_tasks (day_number, title, description, kind, estimated_minutes, order_index) values
(1, 'Understand Generative AI', 'Read the Introduction to LLMs topic and write down what a token is in your own words.', 'LEARN', 30, 1),
(1, 'Learn basic LLM concepts', 'Explain context window, temperature and hallucination in three short notes.', 'LEARN', 20, 2),
(1, 'Explore an AI tool', 'Try NotebookLM or an Ollama model and note one useful and one annoying thing.', 'EXPLORE', 20, 3),
(1, 'Complete one practical exercise', 'Ask a model to summarise an AI news article and check the summary against the source.', 'PRACTICE', 25, 4),

(2, 'Understand Prompt Engineering', 'Read the Prompt Engineering topic and list the five parts of a good prompt.', 'LEARN', 30, 1),
(2, 'Practice writing prompts', 'Rewrite one vague prompt into a structured prompt with a format requirement.', 'PRACTICE', 25, 2),
(2, 'Build a small prompt library', 'Create a file with five reusable prompts for study, summarising and explaining code.', 'BUILD', 30, 3),
(2, 'Reflect on failures', 'Run the same prompt three times and note where the output changed.', 'PRACTICE', 15, 4),

(3, 'Understand AI Agents', 'Read the AI Agents topic and sketch the plan, act, observe loop.', 'LEARN', 30, 1),
(3, 'Design one agentic workflow', 'Write the steps for a daily news briefing agent, including where a human should approve.', 'PRACTICE', 30, 2),
(3, 'Explore tool calling', 'Read the tool calling docs for one provider and note how arguments are validated.', 'LEARN', 25, 3),
(3, 'Try a research tool', 'Use Perplexity to research one AI model release and check the cited sources.', 'EXPLORE', 20, 4),

(4, 'Understand embeddings', 'Read the Embeddings topic and explain cosine similarity to a friend.', 'LEARN', 30, 1),
(4, 'Embed a small text set', 'Embed ten sentences with a free model and find the closest pair.', 'PRACTICE', 40, 2),
(4, 'Compare keyword and semantic search', 'Search the same query both ways and note the difference.', 'PRACTICE', 25, 3),
(4, 'Bookmark learning resources', 'Save three resources from the roadmap that you will use this week.', 'EXPLORE', 10, 4),

(5, 'Understand RAG', 'Read the RAG topic and list the six steps of the pipeline.', 'LEARN', 35, 1),
(5, 'Build a tiny RAG demo', 'Chunk one document, retrieve the best chunk and answer using only that chunk.', 'BUILD', 60, 2),
(5, 'Learn vector databases', 'Read the pgvector README and note how an index speeds up search.', 'LEARN', 25, 3),
(5, 'Review output quality', 'Write three questions your RAG demo answers badly and why.', 'PRACTICE', 20, 4),

(6, 'Learn Spring Boot AI integration', 'Read the Spring Boot integration topic and sketch a service interface for an AI provider.', 'LEARN', 35, 1),
(6, 'Build a summariser endpoint', 'Create a REST endpoint that summarises text and falls back gracefully when the provider fails.', 'BUILD', 60, 2),
(6, 'Handle errors properly', 'Add a timeout, a retry and a clear error response to your endpoint.', 'PRACTICE', 30, 3),
(6, 'Explore a coding assistant', 'Ask a coding assistant to review your endpoint and evaluate its suggestions critically.', 'EXPLORE', 20, 4),

(7, 'Review the week', 'List what you finished, what is pending and what you still do not understand.', 'PRACTICE', 25, 1),
(7, 'Complete a mini project', 'Ship one small feature end to end: data, API, UI and a working error state.', 'BUILD', 90, 2),
(7, 'Prepare interview answers', 'Answer five interview questions from the roadmap topics out loud.', 'PRACTICE', 30, 3),
(7, 'Plan next week', 'Pick the next two roadmap topics and add them to your own study plan.', 'LEARN', 20, 4);
