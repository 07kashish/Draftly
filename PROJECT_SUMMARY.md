# Draftly Project Summary

## Project Name

Draftly - Gmail AI Reply Agent

## Problem Statement

Email replies are repetitive but still require careful judgment. Users need to identify intent, urgency, tone, action items, and safe next steps before responding. Manual replies take time, and generic AI replies can miss context or invent commitments.

## Objective

Build a Gmail-based AI reply assistant that generates safe, context-aware drafts, lets users manage drafts, learns lightweight tone preferences from approved drafts, and saves replies as Gmail drafts.

## Features Implemented

- Gmail Chrome extension integration
- Google OAuth for Gmail compose access
- Email context extraction from Gmail
- Category, tone, urgency, and action-item detection
- Reply strategy generation
- OpenRouter LLM integration with fallback generator
- Draft create, view, update, approve, reject, and regenerate workflows
- User tone profile learning
- Gmail draft saving with best-effort thread linking
- Swagger/OpenAPI backend documentation
- Backend tests and manual demo checklists

## System Architecture

Gmail -> Chrome extension -> Spring Boot backend -> detection services -> strategy service -> LLM/fallback draft generator -> backend response -> Gmail editor.

Approved drafts update the tone profile. Future analysis requests include learned writing preferences in draft generation.

## Backend Modules

- Controllers: health, email analysis, draft management, user/tone profile APIs
- Services: detection, action-item extraction, strategy creation, draft generation, tone learning
- Repositories: users, emails, drafts, tone profiles
- DTOs: request/response contracts for extension and API clients
- Config: CORS, OpenRouter, OpenAPI

## Chrome Extension Modules

- `contentScript.js`: Gmail DOM integration, context extraction, draft insertion, Gmail draft save flow
- `background.js`: Google OAuth, Gmail API calls, backend API calls
- `popup.js`: sign-in/status UI
- `manifest.json`: permissions, host access, OAuth scope, content script registration

## Database Entities

- `User`
- `Email`
- `Draft`
- `UserToneProfile`

## AI Integration

Draftly calls OpenRouter for reply generation when configured. If the LLM is unavailable or no API key is provided, the backend uses deterministic fallback drafts so demos and tests remain reliable.

## Gmail API Integration

The extension uses `gmail.compose` to create Gmail drafts. It builds Base64URL-encoded plain-text MIME messages and attempts best-effort thread linking using available Gmail DOM/URL metadata.

## Testing Approach

- Backend integration tests through MockMvc
- Detection service tests
- Draft generation fallback tests
- Swagger/manual API testing
- Manual Gmail extension checklist
- OAuth and Gmail draft save manual tests

## Limitations

- Gmail DOM metadata can vary.
- True Gmail API thread ID is not always available from the DOM.
- Draft quality depends on context and LLM availability.
- OAuth testing mode requires configured test users.
- Local backend must be running during extension demos.

## Future Scope

- Production backend deployment
- More reliable Gmail metadata through Gmail API message access
- Advanced tone learning with privacy controls
- Outlook support
- Calendar availability integration
- Team/admin dashboard
- Privacy-safe retrieval over approved replies
- Analytics for reply categories and workflow outcomes

## Conclusion

Draftly demonstrates a practical AI-assisted email workflow with a working Gmail extension, Spring Boot backend, AI/fallback generation, draft management, tone learning, and Gmail draft saving. It keeps the user in control while reducing repetitive reply-writing effort.
