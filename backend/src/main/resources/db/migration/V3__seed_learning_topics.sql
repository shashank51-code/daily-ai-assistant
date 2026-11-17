-- Learning roadmap: 5 levels, 26 topics with resources, exercises, projects and interview questions.
-- Content is plain English and is split on newlines by the client.

insert into learning_topics
(level_number, level_name, title, slug, explanation, prerequisites, resources, exercises, project_ideas, interview_questions, order_index)
values

-- ===================== LEVEL 1: FUNDAMENTALS =====================
(1, 'Fundamentals', 'Introduction to AI', 'introduction-to-ai',
 'Artificial Intelligence is the field of building software that performs tasks which normally need human intelligence: understanding language, recognising images, recommending content or making predictions. Machine Learning is a subfield where the program learns patterns from data instead of following hand written rules. Generative AI is a further subfield that creates new text, images or code.',
 'Basic computer literacy. No programming experience required yet.',
 'Elements of AI (free course) - https://www.elementsofai.com
Google Machine Learning Crash Course - https://developers.google.com/machine-learning/crash-course
3Blue1Brown neural network videos - https://www.youtube.com/playlist?list=PLZHQObOWTQDNU6R1_67000Dx_ZCJB-3pi',
 'List five AI features you used today and write one sentence on how each might work.
Classify ten app features as rule based, classical ML or generative AI.
Explain in your own words the difference between AI, ML and Generative AI.',
 'Build a one page glossary website for AI terms.
Create a comparison table of AI, ML, Deep Learning and Generative AI with examples.',
 'What is the difference between AI, Machine Learning and Deep Learning?
What is supervised learning?
Can you name a problem where AI is the wrong solution and why?',
 1),

(1, 'Fundamentals', 'Python Basics', 'python-basics',
 'Python is the default language of the AI ecosystem because of its readable syntax and huge library support. For AI work you mainly need variables, lists, dictionaries, functions, virtual environments and how to install packages with pip.',
 'A computer with Python 3.11 or newer installed.',
 'Official Python tutorial - https://docs.python.org/3/tutorial/
Real Python beginners guide - https://realpython.com/python-first-steps/
Automate the Boring Stuff with Python (free book) - https://automatetheboringstuff.com',
 'Write a script that reads a JSON file of articles and prints titles sorted by date.
Create a function that returns the top N keywords from a paragraph.
Set up a virtual environment and install the requests library.',
 'A command line news digest tool that reads an RSS feed with feedparser.
A CSV cleaning utility that removes duplicates and empty rows.',
 'What is the difference between a list and a tuple?
What is a virtual environment and why is it needed?
How do you handle exceptions in Python?',
 2),

(1, 'Fundamentals', 'Mathematics for AI', 'mathematics-for-ai',
 'You do not need advanced maths to start building AI applications, but you do need intuition. Vectors and matrices describe data and model weights, derivatives explain how a model learns from error, and probability describes uncertainty in predictions.',
 'School level algebra. Curiosity about graphs.',
 'Khan Academy Linear Algebra - https://www.khanacademy.org/math/linear-algebra
3Blue1Brown Essence of Linear Algebra - https://www.youtube.com/playlist?list=PLZHQObOWTQDPD3MizzM2xVFitgF8hE_ab
StatQuest statistics playlists - https://www.youtube.com/@statquest',
 'Compute the dot product of two vectors by hand and with NumPy.
Plot a simple loss curve and explain what the slope means.
Normalise a list of numbers to the range 0 to 1.',
 'A NumPy notebook that implements linear regression from scratch.
An interactive page that shows how a line fits points as the slope changes.',
 'What is a gradient and how is it used in training?
What does a dot product measure?
Why do we normalise features before training?',
 3),

(1, 'Fundamentals', 'Machine Learning Fundamentals', 'machine-learning-fundamentals',
 'Machine learning learns a function from sample data. The three main styles are supervised learning (labelled data), unsupervised learning (finding structure) and reinforcement learning (learning from reward). Always evaluate on data the model has not seen, otherwise you are only measuring memorisation.',
 'Python basics and basic statistics.',
 'Google Machine Learning Crash Course - https://developers.google.com/machine-learning/crash-course
scikit-learn user guide - https://scikit-learn.org/stable/user_guide.html
StatQuest machine learning playlist - https://www.youtube.com/@statquest',
 'Train a classifier on the iris dataset and report accuracy and confusion matrix.
Split a dataset into train and test sets and compare the scores.
Try underfitting and overfitting by changing model complexity.',
 'A house price predictor with a clean train/test report.
A spam classifier that explains which words influenced the prediction.',
 'What is overfitting and how do you detect it?
What is the difference between precision and recall?
Why can accuracy be a misleading metric on imbalanced data?',
 4),

-- ===================== LEVEL 2: DEEP LEARNING =====================
(2, 'Deep Learning', 'Neural Networks', 'neural-networks',
 'A neural network is a stack of layers of simple units. Each unit multiplies its inputs by weights, adds a bias and applies a non linear activation. Training repeats two steps: forward pass to compute the output, then backpropagation to push the weights in the direction that reduces the loss.',
 'Machine learning fundamentals and basic linear algebra.',
 'Neural Networks and Deep Learning (free book) - http://neuralnetworksanddeeplearning.com
PyTorch 60 minute blitz - https://pytorch.org/tutorials/beginner/deep_learning_60min_blitz.html
TensorFlow neural network playground - https://playground.tensorflow.org',
 'Implement a single neuron by hand and train it on OR logic.
Build a two layer network on MNIST and plot the loss.
Break the network on purpose by removing the activation function and explain the result.',
 'A digit recogniser served as a small HTTP API.
A visual explainer of one forward and backward pass.',
 'What is backpropagation in one sentence?
Why do we need non linear activation functions?
What happens if the learning rate is too high?',
 1),

(2, 'Deep Learning', 'Convolutional Neural Networks (CNN)', 'cnns',
 'CNNs are designed for grid data such as images. Small filters slide across the image to detect edges, textures and shapes. Because the same filter is reused everywhere, a CNN needs far fewer parameters than a fully connected network and learns patterns that are independent of position.',
 'Neural networks basics.',
 'CS231n CNN notes - https://cs231n.github.io/convolutional-networks/
PyTorch CIFAR10 tutorial - https://pytorch.org/tutorials/beginner/blitz/cifar10_tutorial.html
Keras image classification guide - https://keras.io/guides/transfer_learning/',
 'Compute the output size of a convolution by hand.
Train a small CNN on CIFAR10 for a few epochs and log accuracy.
Try data augmentation and compare results.',
 'A plant leaf disease classifier with a confusion matrix.
An image search demo that uses CNN features instead of raw pixels.',
 'What does a convolution filter learn in the first layer?
What is the purpose of pooling?
Why is transfer learning so common in vision?',
 2),

(2, 'Deep Learning', 'Recurrent Neural Networks (RNN)', 'rnns',
 'RNNs process a sequence one step at a time and carry a hidden state forward, which lets them model order. Plain RNNs struggle with long sequences because gradients vanish or explode. LSTM and GRU cells add gates that decide what to remember and forget.',
 'Neural networks and basic calculus.',
 'Understanding LSTM Networks - https://colah.github.io/posts/2015-08-Understanding-LSTMs/
PyTorch sequence models tutorial - https://pytorch.org/tutorials/beginner/nlp/sequence_models_tutorial.html',
 'Train a character level model on a small text file.
Compare an RNN and an LSTM on the same sequence task.
Inspect the hidden state values across time steps.',
 'A next word predictor for technical headlines.
A simple anomaly detector for sensor time series.',
 'Why do plain RNNs suffer from vanishing gradients?
What does an LSTM gate control?
When would you prefer a transformer over an RNN?',
 3),

(2, 'Deep Learning', 'Transformers', 'transformers',
 'Transformers replaced recurrence with attention. Every token can look at every other token, so training parallelises across the sequence. Self attention scores how relevant each token is to each other token, and multi head attention learns several of these relationships at once. Almost every modern LLM is a transformer.',
 'Neural networks and RNN basics.',
 'Attention Is All You Need (paper) - https://arxiv.org/abs/1706.03762
The Illustrated Transformer - https://jalammar.github.io/illustrated-transformer/
Hugging Face LLM course - https://huggingface.co/learn/llm-course',
 'Implement scaled dot product attention in NumPy.
Visualise attention weights for a short sentence with a pretrained model.
Count the parameters of a small pretrained transformer.',
 'A mini GPT trained on a small text corpus.
An attention heatmap explorer for sentences.',
 'Why is self attention better than recurrence for long sequences?
What is a position encoding needed for?
What is the difference between an encoder and a decoder?',
 4),

(2, 'Deep Learning', 'Deep Learning Frameworks', 'deep-learning-frameworks',
 'PyTorch and TensorFlow are the two dominant frameworks. PyTorch is the most common choice in research and is very readable; TensorFlow with Keras is still common in production and mobile deployment. Hugging Face Transformers wraps pretrained models for both.',
 'Python and neural network basics.',
 'PyTorch tutorials - https://pytorch.org/tutorials/
TensorFlow tutorials - https://www.tensorflow.org/tutorials
Hugging Face Transformers docs - https://huggingface.co/docs/transformers',
 'Install PyTorch in a virtual environment and run the MNIST example.
Save and reload a trained model checkpoint.
Log training metrics with TensorBoard.',
 'A reusable training template with config files and checkpoints.
A model card page that documents a small trained model.',
 'What is the difference between a tensor and a NumPy array?
How do you move a model between CPU and GPU?
What does model.eval() change in PyTorch?',
 5),

-- ===================== LEVEL 3: GENERATIVE AI =====================
(3, 'Generative AI', 'Introduction to LLMs', 'introduction-to-llms',
 'A Large Language Model is a transformer trained to predict the next token on a very large text corpus. After training, instruction tuning and alignment make it follow requests. An LLM does not look up facts in a database; it generates the most likely continuation, which is why it can sound confident and still be wrong.',
 'Transformers basics.',
 'Hugging Face LLM course - https://huggingface.co/learn/llm-course
OpenAI model documentation - https://platform.openai.com/docs/models
Andrej Karpathy intro to LLMs - https://www.youtube.com/watch?v=zjkBMFhNj_g',
 'Send a prompt to a hosted model and vary the temperature.
Compare two models on the same summarisation task.
Write down three answers where the model was confidently wrong.',
 'A news summariser with a fallback when the API fails.
A model comparison page that stores prompt and response history.',
 'What is a token and why does context length matter?
Why do LLMs hallucinate?
What is the difference between a base model and an instruction tuned model?',
 1),

(3, 'Generative AI', 'Prompt Engineering', 'prompt-engineering',
 'Prompt engineering is the practice of writing instructions that reliably produce useful output. Good prompts state the role, the task, the audience, the output format and any constraints, and they include examples when the format matters. It does not replace good product design or verification.',
 'Working with an LLM chat interface.',
 'OpenAI prompt engineering guide - https://platform.openai.com/docs/guides/prompt-engineering
Anthropic prompt engineering overview - https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/overview
Google prompting guide - https://ai.google.dev/gemini-api/docs/prompting-strategies',
 'Rewrite one vague prompt into a structured prompt and compare results.
Create a prompt that forces a JSON response and validate the JSON.
Build a bad prompt on purpose and document the failure.',
 'A prompt library page with copy and version history.
A prompt test harness that runs the same prompt five times.',
 'What belongs in a system prompt versus a user prompt?
How do you make model output machine readable?
How would you evaluate a prompt change objectively?',
 2),

(3, 'Generative AI', 'Embeddings', 'embeddings',
 'An embedding maps text to a list of numbers in which similar meanings are close together. Embeddings power semantic search, clustering, deduplication and retrieval augmented generation. Distance is usually measured with cosine similarity.',
 'Python and LLM basics.',
 'OpenAI embeddings guide - https://platform.openai.com/docs/guides/embeddings
Sentence Transformers documentation - https://www.sbert.net/
Cohere embeddings overview - https://docs.cohere.com/docs/embeddings',
 'Embed ten sentences and print the closest pair.
Compare keyword search with semantic search on the same query.
Plot a 2D projection of embeddings for three topics.',
 'A duplicate question detector for a support inbox.
A semantic search box over your own notes.',
 'Why can cosine similarity ignore vector length?
When would embeddings fail at finding relevant text?
Why must the same embedding model be used for indexing and querying?',
 3),

(3, 'Generative AI', 'Vector Databases', 'vector-databases',
 'A vector database stores embeddings and finds nearest neighbours quickly using specialised indexes. Metadata filters let you combine similarity with rules such as category or date. Common options include pgvector, Qdrant, Weaviate, Chroma and Pinecone.',
 'Embeddings basics.',
 'pgvector README - https://github.com/pgvector/pgvector
Qdrant documentation - https://qdrant.tech/documentation/
Chroma getting started - https://docs.trychroma.com/',
 'Enable pgvector in PostgreSQL and store ten embeddings.
Run a nearest neighbour query and inspect the returned distances.
Add a metadata filter to a similarity query.',
 'A document Q&A backend that stores chunk embeddings in PostgreSQL.
A recommendation service that finds similar articles.',
 'What is an approximate nearest neighbour index?
Why do we split documents into chunks before embedding?
How do metadata filters interact with vector search?',
 4),

(3, 'Generative AI', 'Retrieval Augmented Generation (RAG)', 'rag',
 'RAG gives an LLM relevant context instead of relying on its memory. A typical pipeline: load documents, split them into chunks, embed and store them, embed the user question, retrieve the closest chunks, then ask the model to answer using only those chunks and cite them. RAG reduces hallucination but does not remove it.',
 'Embeddings and vector databases.',
 'What is RAG - https://www.pinecone.io/learn/retrieval-augmented-generation/
LangChain RAG tutorial - https://python.langchain.com/docs/tutorials/rag/
Anthropic contextual retrieval - https://www.anthropic.com/news/contextual-retrieval',
 'Build a RAG pipeline over five PDF pages.
Compare answers with and without retrieved context.
Log the retrieved chunks for every answer and check relevance.',
 'A RAG assistant over official documentation with source links.
A company policy chatbot that refuses questions outside the corpus.',
 'Why does chunk size affect answer quality?
How do you evaluate retrieval quality separately from generation quality?
What should the model do when no relevant chunk is found?',
 5),

(3, 'Generative AI', 'Fine-tuning', 'fine-tuning',
 'Fine-tuning continues training a pretrained model on your own examples so it adapts to a style, format or narrow task. It is expensive compared to prompting and it is a poor way to inject changing facts, which is better handled by retrieval. LoRA and QLoRA make fine-tuning much cheaper by training small adapter matrices.',
 'LLM and prompt engineering experience.',
 'Hugging Face PEFT documentation - https://huggingface.co/docs/peft
OpenAI fine-tuning guide - https://platform.openai.com/docs/guides/fine-tuning
LoRA paper - https://arxiv.org/abs/2106.09685',
 'Estimate whether a task needs prompting or fine-tuning and justify it.
Prepare a small instruction dataset with clean input and output pairs.
Compare a prompt only baseline with a fine-tuned adapter.',
 'A support reply formatter tuned on approved examples.
A domain classifier fine-tuned on labelled headlines.',
 'When should you fine-tune instead of using RAG?
What does LoRA change in a model?
How do you avoid overfitting on a small dataset?',
 6),

-- ===================== LEVEL 4: AI AGENTS =====================
(4, 'AI Agents', 'AI Agents', 'ai-agents',
 'An AI agent is an LLM given a goal, tools and a loop. It plans, calls a tool, observes the result, and repeats until the task is done or a limit is reached. Agents are powerful but non deterministic, so production systems need step limits, timeouts, logging and human approval for risky actions.',
 'LLMs, prompt engineering and basic API development.',
 'Anthropic building effective agents - https://www.anthropic.com/engineering/building-effective-agents
OpenAI agents guide - https://platform.openai.com/docs/guides/agents
LangGraph concepts - https://langchain-ai.github.io/langgraph/concepts/',
 'Write down an agent loop in pseudocode.
Add a step limit to a simple agent and observe behaviour.
Log every tool call and its result for one run.',
 'A research agent that searches and returns a cited summary.
A file organiser agent that asks for confirmation before moving files.',
 'What is the difference between a workflow and an agent?
Why do agents need step limits?
How do you test something that is non deterministic?',
 1),

(4, 'AI Agents', 'Agentic AI', 'agentic-ai',
 'Agentic AI describes systems that pursue goals over multiple steps with some autonomy: they plan, use tools, remember context and adapt. The practical pattern for most teams is a bounded workflow with agentic steps, not a fully autonomous agent, because predictability and cost control matter.',
 'AI agents basics.',
 'Anthropic building effective agents - https://www.anthropic.com/engineering/building-effective-agents
Google agents white paper - https://www.kaggle.com/whitepaper-agents
LangGraph multi agent tutorial - https://langchain-ai.github.io/langgraph/tutorials/',
 'Design an agentic flow for a daily news briefing.
List every place human approval should be required.
Compare a fixed pipeline with an agentic version on cost and latency.',
 'A daily briefing agent that fetches, summarises and emails AI news.
A study planner agent that reschedules tasks when one is missed.',
 'What makes a system agentic rather than just automated?
How do you bound the cost of an agentic workflow?
What are the failure modes of tool calling loops?',
 2),

(4, 'AI Agents', 'Tool Calling and Function Calling', 'tool-calling',
 'Tool calling lets the model request a function by name with structured arguments instead of writing free text. Your code validates the arguments, executes the real function and returns the result to the model. The model never executes code directly, which is the key safety boundary.',
 'Agents basics and REST API experience.',
 'OpenAI function calling guide - https://platform.openai.com/docs/guides/function-calling
Anthropic tool use docs - https://docs.anthropic.com/en/docs/build-with-claude/tool-use
Spring AI tool calling - https://docs.spring.io/spring-ai/reference/api/tools.html',
 'Define a JSON schema for a getWeather tool and validate arguments.
Implement a tool call loop with a maximum of three rounds.
Return a structured error to the model when validation fails.',
 'A Java service exposing three safe tools to an LLM.
A tool registry page that documents every available tool and schema.',
 'Who executes a tool call?
How do you validate tool arguments safely?
What should happen when a tool returns an error?',
 3),

(4, 'AI Agents', 'Multi-Agent Systems', 'multi-agent-systems',
 'Multi agent systems split a task across specialised agents, for example a researcher, a writer and a reviewer. This can improve quality on complex tasks, but it also multiplies cost, latency and failure points. Use it only when a single agent with a clear prompt is clearly insufficient.',
 'Single agent systems and tool calling.',
 'AutoGen documentation - https://microsoft.github.io/autogen/
CrewAI documentation - https://docs.crewai.com/
LangGraph multi agent guide - https://langchain-ai.github.io/langgraph/concepts/multi_agent/',
 'Design a two agent researcher and reviewer flow.
Compare single agent and two agent output on the same task.
Track token cost per agent for one run.',
 'A content pipeline with writer and fact checker agents.
A code review agent that hands fixes to a second agent.',
 'When is multi agent worse than one agent?
How do agents share state safely?
How do you stop agents from looping on each other?',
 4),

(4, 'AI Agents', 'Agent Frameworks', 'agent-frameworks',
 'Frameworks provide building blocks for agents: model clients, tool definitions, state graphs, memory and tracing. Popular choices are LangGraph, CrewAI, AutoGen, OpenAI Agents SDK and Spring AI for Java. Start with the plain API, add a framework when you feel the missing abstraction.',
 'Agent concepts and one programming language.',
 'LangGraph documentation - https://langchain-ai.github.io/langgraph/
Spring AI reference - https://docs.spring.io/spring-ai/reference/
OpenAI Agents SDK - https://openai.github.io/openai-agents-python/',
 'Rebuild one toy agent with two different frameworks.
Add tracing to see each step.
Write down what the framework did for you and what it hid.',
 'A Spring Boot agent service with tool calling and tracing.
A comparison notebook of two agent frameworks on one task.',
 'What problem does an agent framework solve?
How do you debug an agent run in production?
What is the risk of tight coupling to one framework?',
 5),

-- ===================== LEVEL 5: REAL WORLD DEVELOPMENT =====================
(5, 'Real-World Development', 'AI APIs', 'ai-apis',
 'Most applications call an AI model through an HTTP API rather than hosting one. You send messages or inputs with an API key, receive a response and pay per token. Design for timeouts, retries, rate limits and graceful degradation, because the provider can be slow or unavailable.',
 'HTTP, JSON and basic backend development.',
 'OpenAI API reference - https://platform.openai.com/docs/api-reference
Anthropic API reference - https://docs.anthropic.com/en/api/overview
Google Gemini API docs - https://ai.google.dev/gemini-api/docs',
 'Call a chat completions API from Postman and from code.
Handle a 429 response with a retry and backoff.
Measure latency and token usage for ten requests.',
 'A backend proxy that keeps the API key server side.
A cost dashboard for your own API usage.',
 'Why should an API key never live in the mobile app?
How do you handle provider rate limits?
What is your fallback when the AI provider is down?',
 1),

(5, 'Real-World Development', 'Spring Boot Integration with AI Services', 'spring-boot-ai-integration',
 'In a Java backend, AI calls belong in a service layer behind an interface so the rest of the code does not depend on one vendor. Spring RestClient or WebClient handles HTTP, configuration comes from environment variables, and failures should fall back to non AI behaviour instead of breaking the feature.',
 'Spring Boot basics and AI API experience.',
 'Spring RestClient docs - https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
Spring AI reference - https://docs.spring.io/spring-ai/reference/
Baeldung RestClient guide - https://www.baeldung.com/spring-boot-restclient',
 'Wrap an AI call behind an interface with two implementations.
Add a timeout and map provider errors to your own exception.
Cache a generated summary in the database.',
 'A news summarisation service with an offline fallback.
An admin endpoint that regenerates explanations for stored articles.',
 'Why wrap an external API behind an interface?
How do you keep AI secrets out of configuration files?
What should the endpoint return when the provider times out?',
 2),

(5, 'Real-World Development', 'Python AI Services', 'python-ai-services',
 'Python is used for AI heavy work such as embeddings, local models and data processing. A common architecture is a Java backend for business logic plus a small Python service for model work, connected over HTTP. Keep the contract small and versioned.',
 'Python, FastAPI or Flask, and basic AI libraries.',
 'FastAPI tutorial - https://fastapi.tiangolo.com/tutorial/
Sentence Transformers docs - https://www.sbert.net/
Ollama docs - https://docs.ollama.com/',
 'Expose one embedding endpoint with FastAPI and test it.
Add request validation and a health check.
Call the Python service from a Java client.',
 'A Python embedding microservice with pgvector storage.
A local LLM service used for offline summaries.',
 'Why split AI work into a separate service?
How do you version the contract between Java and Python?
How do you deploy two runtimes together?',
 3),

(5, 'Real-World Development', 'Building AI Applications', 'building-ai-applications',
 'A real AI application is mostly normal software: data model, APIs, background jobs, error states and UI. AI adds a non deterministic component, so you need evaluation, user feedback, fallbacks and clear disclosure of what the model produced. Start with a narrow, testable use case.',
 'Backend, frontend and one AI API integration.',
 'Full stack open - https://fullstackopen.com/en/
12 factor app - https://12factor.net/
OpenAI production best practices - https://platform.openai.com/docs/guides/production-best-practices',
 'Write a one page spec with a clear user problem and a measurable success metric.
Build the feature with the AI step disabled and add it last.
Add an evaluation checklist of ten test inputs.',
 'A daily AI briefing app with real RSS data and learning progress.
A document Q&A app with citations and a clear fallback state.',
 'What makes an AI feature testable?
How do you measure whether the feature actually helps users?
What do you show the user when the model is unsure?',
 4),

(5, 'Real-World Development', 'Deployment', 'deployment',
 'Deployment turns your local project into something other people can use. Containerise the backend and database with Docker, store secrets in environment variables, run database migrations at startup and serve the React build as static files or from a host. Set resource limits and health checks.',
 'A working backend and frontend build.',
 'Docker documentation - https://docs.docker.com/get-started/
Spring Boot Docker guide - https://spring.io/guides/topicals/spring-boot-docker/
Render deployment docs - https://render.com/docs/deploy-spring-boot',
 'Write a multi stage Dockerfile for the backend.
Bring up the full stack with docker compose and hit the health endpoint.
Run the app with environment variables instead of hardcoded values.',
 'A deployed demo with PostgreSQL and an automated migration.
A GitHub Actions workflow that builds and tests the project.',
 'Why use a multi stage Docker build?
How do you handle database migrations on deploy?
Where do secrets belong in production?',
 5),

(5, 'Real-World Development', 'Monitoring', 'monitoring',
 'Monitoring tells you whether the product works after launch. For AI applications track availability, latency, error rate and token cost, plus quality signals such as fallback rate and user feedback. Log model calls without storing personal data.',
 'A deployed application.',
 'Spring Boot Actuator - https://docs.spring.io/spring-boot/reference/actuator/index.html
Prometheus documentation - https://prometheus.io/docs/introduction/overview/
OpenTelemetry docs - https://opentelemetry.io/docs/',
 'Expose the actuator health and metrics endpoints.
Add a log line for every AI call with latency and success flag.
Define one alert threshold for error rate or latency.',
 'A small ops dashboard that shows fetch success and fallback rate.
A scheduled job that reports stale news feeds.',
 'Which metrics matter for an AI feature?
How do you detect that an RSS feed stopped working?
What should you never log?',
 6);
