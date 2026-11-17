-- Default local user (no authentication in this project: single-profile mode)
-- and the category taxonomy used by the RSS categorizer.

insert into users (name, email) values ('Fresher', 'fresher@dailyai.local');

insert into categories (name, slug, description) values
    ('Latest AI News',     'latest-ai-news',  'General AI industry updates.'),
    ('Generative AI',      'generative-ai',   'LLMs, image, video and audio generation.'),
    ('Machine Learning',   'machine-learning','Classical ML, training and evaluation.'),
    ('Deep Learning',      'deep-learning',   'Neural networks, CNNs, RNNs and transformers.'),
    ('AI Research',        'ai-research',     'Papers and research breakthroughs.'),
    ('AI Agents',          'ai-agents',       'Agents, agentic workflows and tool calling.'),
    ('AI Tools',           'ai-tools',        'Products, libraries and developer tools.'),
    ('AI Models',          'ai-models',       'New foundation and open-weight model releases.'),
    ('AI Startups',        'ai-startups',     'Funding, products and company news.'),
    ('Tutorials',          'tutorials',       'Guides and learning material.');
