-- AI tools explorer. Pricing text is intentionally qualitative: plans change often,
-- so the app shows the verification date and asks users to confirm on the official site.

insert into ai_resources
(name, category, description, features, website, pricing, free_alternatives, trending, pricing_verified_on)
values
('GitHub Copilot', 'AI Coding Assistants',
 'AI pair programmer integrated into popular editors that suggests completions and answers coding questions in context.',
 'Inline code completion
Chat in the editor
Pull request summaries
Multiple IDE integrations',
 'https://github.com/features/copilot',
 'Free tier for individual developers with monthly limits; paid individual and business plans available. Confirm current limits on the official page.',
 'Codeium, Continue with a local model via Ollama',
 true, '2026-11-17'),

('Cursor', 'AI Coding Assistants',
 'AI first code editor built on VS Code that supports chat, inline edits and codebase aware questions.',
 'Codebase chat
Inline edit commands
Agent mode for multi file changes
Model selection',
 'https://cursor.com',
 'Free Hobby plan with limited requests; paid Pro and Business plans. Confirm current limits on the official page.',
 'VS Code with Continue and Ollama',
 true, '2026-11-17'),

('Tabnine', 'AI Coding Assistants',
 'Code completion assistant that emphasises private deployment options for teams.',
 'Code completions
Chat
Self hosted deployment options
IDE integrations',
 'https://www.tabnine.com',
 'Free basic plan for individuals; paid Dev and Enterprise plans. Confirm on the official pricing page.',
 'GitHub Copilot free tier, local models',
 false, '2026-11-17'),

('Midjourney', 'AI Image Generators',
 'Image generation service known for its distinctive artistic style, used through the web app and Discord.',
 'Text to image generation
Style and aspect ratio controls
Image variation and upscaling',
 'https://www.midjourney.com',
 'Paid subscription plans only, with no permanent free tier. Confirm current pricing on the official site.',
 'Stable Diffusion (open source), ComfyUI',
 false, '2026-11-17'),

('DALL-E via ChatGPT', 'AI Image Generators',
 'Image generation available inside ChatGPT for creating and editing images from text prompts.',
 'Text to image
Image editing with prompts
Chat based iteration',
 'https://chatgpt.com',
 'Available within ChatGPT plans, which include a free tier with limits and paid tiers. Confirm current limits on the official site.',
 'Stable Diffusion, Hugging Face Spaces demos',
 true, '2026-11-17'),

('Stable Diffusion', 'AI Image Generators',
 'Open weight image generation model family that can be run locally or on your own servers.',
 'Text to image
Image to image
Fine tuning with LoRA
Runs locally on suitable hardware',
 'https://stability.ai',
 'Model weights are available under open licences with conditions that vary by release; check the licence of the specific version.',
 'ComfyUI, Automatic1111 WebUI',
 true, '2026-11-17'),

('Runway', 'AI Video Generators',
 'Generative video platform with text to video, image to video and editing tools.',
 'Text to video
Image to video
Video editing tools',
 'https://runwayml.com',
 'Free trial credits plus paid plans. Confirm current credits and pricing on the official site.',
 'Open source video models on Hugging Face',
 false, '2026-11-17'),

('Pika', 'AI Video Generators',
 'Video generation tool focused on short clips from text or images.',
 'Text to video
Image to video
Short clip effects',
 'https://pika.art',
 'Free credits with paid plans. Confirm current pricing on the official site.',
 'Open source video models on Hugging Face',
 false, '2026-11-17'),

('Perplexity', 'AI Research Tools',
 'Answer engine that combines web search with AI generated answers and inline citations.',
 'Cited answers
Follow up questions
Focus modes for different sources',
 'https://www.perplexity.ai',
 'Free tier with limited advanced searches; paid Pro plan. Confirm current limits on the official site.',
 'Bing Chat, Google AI search features',
 true, '2026-11-17'),

('NotebookLM', 'AI Research Tools',
 'Google research notebook that answers questions from documents you upload and generates study aids.',
 'Source grounded answers
Audio overviews
Notes and summaries',
 'https://notebooklm.google.com',
 'Free to use with limits on sources and features; Google AI plans add higher limits. Confirm current limits on the official site.',
 'Local RAG with Ollama and pgvector',
 true, '2026-11-17'),

('Elicit', 'AI Research Tools',
 'Research assistant for finding papers and extracting information from them.',
 'Paper search
Structured extraction tables
Summaries',
 'https://elicit.com',
 'Free tier with monthly credits; paid plans for higher usage. Confirm on the official site.',
 'Semantic Scholar, arXiv search',
 false, '2026-11-17'),

('n8n', 'AI Automation Tools',
 'Workflow automation platform with a visual editor that can be self hosted and extended with AI steps.',
 'Visual workflows
Webhook and schedule triggers
Self hosting
AI and LLM nodes',
 'https://n8n.io',
 'Source available with a free self hosted community edition; paid cloud plans. Check the licence terms for your use case.',
 'Node-RED, Windmill',
 true, '2026-11-17'),

('Zapier', 'AI Automation Tools',
 'No code automation platform connecting thousands of apps, with AI steps for summarising and generating content.',
 'App integrations
Multi step automations
AI actions
Scheduled triggers',
 'https://zapier.com',
 'Free plan with limited tasks and single step automations; paid plans. Confirm current limits on the official site.',
 'n8n self hosted, Make free tier',
 false, '2026-11-17'),

('Make', 'AI Automation Tools',
 'Visual automation builder with scenario based workflows and app connectors.',
 'Visual scenario builder
Scheduling
App connectors
Error handling paths',
 'https://www.make.com',
 'Free tier with a monthly operations limit; paid plans. Confirm current limits on the official site.',
 'n8n self hosted',
 false, '2026-11-17'),

('Notion AI', 'AI Productivity Tools',
 'AI features inside Notion for writing help, summarising notes and answering questions across your workspace.',
 'Writing assistance
Summaries
Workspace Q&A',
 'https://www.notion.com/product/ai',
 'Included in some Notion plans as a paid add on or bundled feature. Confirm current plan details on the official site.',
 'ChatGPT free tier, local LLM with Ollama',
 false, '2026-11-17'),

('Ollama', 'Open-Source AI Tools',
 'Runs open weight language models locally with a simple command line and local API.',
 'Local model runner
OpenAI compatible API
Model library
Runs offline',
 'https://ollama.com',
 'Free and open source. Your own hardware and electricity are the only cost.',
 'llama.cpp, LM Studio',
 true, '2026-11-17'),

('LangChain', 'Open-Source AI Tools',
 'Framework for building LLM applications with chains, tools, memory and retrieval integrations.',
 'Model abstractions
Tool calling
Retrieval integrations
Python and JavaScript packages',
 'https://www.langchain.com',
 'Open source library under the MIT licence; optional paid LangSmith tracing platform.',
 'Plain provider SDKs, Spring AI for Java',
 true, '2026-11-17'),

('Hugging Face Transformers', 'Open-Source AI Tools',
 'Library and model hub for using pretrained transformer models across text, vision and audio.',
 'Pretrained model hub
Pipeline API
Fine tuning utilities
Datasets and Spaces',
 'https://huggingface.co',
 'Libraries are open source; the hub has a free tier plus paid Pro and Enterprise plans. Confirm current limits on the official site.',
 'Ollama, scikit-learn for classical ML',
 true, '2026-11-17');
