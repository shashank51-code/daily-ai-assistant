# Personalization Design

## User controls

Profile now offers eight optional interests:

| Stored value | Display label |
|---|---|
| `AI_ML` | AI and Machine Learning |
| `GENERATIVE_AI` | Generative AI |
| `AI_CODING_AGENTS` | AI Coding Agents |
| `JAVA_BACKEND` | Java and Backend Development |
| `DEVELOPER_TOOLS` | Developer Tools |
| `AI_AUTOMATION` | AI Automation |
| `CLOUD_AI_INFRASTRUCTURE` | Cloud and AI Infrastructure |
| `AI_RESEARCH` | AI Research |

Selections are optional, can be saved from Profile, and can be reset with **Reset to neutral**. Reset sends an empty list; neutral recommendations are used until the user selects interests again.

## Storage and API

- Flyway migration V7 adds a nullable-in-concept, default-empty `preferences` value to the existing `users` row (`varchar(500) NOT NULL DEFAULT ''`). Existing profiles therefore migrate to no selected interests.
- `GET /api/profile` now returns `preferences` as a list.
- `PUT /api/profile/preferences` accepts `{ "preferences": ["JAVA_BACKEND"] }`; send an empty array to reset.
- The backend rejects unknown values and lists longer than eight entries.
- The frontend does not contain a hardcoded user preference. Recommendations are calculated from the persisted profile values and article category.

The schema addition is additive and small. The application still uses a single shared profile and has no authentication, as documented in the earlier audit. This preference field is not a per-account system until authentication/user scoping is implemented.

## How matching works

Categories map to relevant interest keys. For example, `ai-agents` can match coding agents, Java/backend, developer tools and automation. A matching practical category can be Learn Now; a matching research/model category remains Explore Later; no category match becomes Skip. If the list is empty, recommendation is neutral Explore Later. Every branch returns a reason and a relevance explanation.

This first version ranks but does not hide or remove updates. It does not infer technical background from a name, use private profile text, or call an AI provider to make the recommendation. Category matches are broad and should be tuned with user feedback.

## Next design steps

1. Validate interest-to-category mappings with Java/backend, research and automation learners.
2. Add explicit background/goal choices only if topic preferences prove insufficient.
3. Keep recommendations explainable and allow users to ignore or dismiss them.
4. Scope preferences and all user data to an authenticated principal before supporting multiple users or public hosting.
