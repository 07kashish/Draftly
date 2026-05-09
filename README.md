# Draftly - Gmail AI Reply Agent

Draftly is a Gmail-based AI reply assistant that analyzes incoming emails, detects category, tone, urgency, and action items, generates context-aware replies, learns from approved drafts, and helps users manage replies through a Chrome extension and Spring Boot backend.

## Problem Statement

Writing email replies takes time because users need to understand intent, decide what action is required, choose the right tone, and avoid making accidental commitments. This becomes repetitive in Gmail when handling follow-ups, meeting requests, complaints, and decision requests.

## Solution

Draftly adds an AI reply workflow inside Gmail. The Chrome extension extracts email context, sends it to the backend, receives a suggested reply, inserts it into the Gmail reply editor, and lets the user edit, save, approve, reject, or regenerate drafts. Approved drafts update a lightweight tone profile so future replies better match the user's writing style.

## Key Features

- Gmail Chrome extension button inside compose/reply boxes
- Email category, tone, urgency, and action-item detection
- Context-aware reply strategy generation
- OpenRouter LLM integration with fallback draft generation
- Draft view, update, approve, reject, and regenerate APIs
- User tone learning from approved and edited drafts
- Gmail draft saving with best-effort thread linking
- AI Compose Mode for writing full new emails from prompts
- Swagger/OpenAPI documentation
- Privacy-aware storage of style signals instead of full private email history

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- PostgreSQL or H2 dev profile
- OpenRouter LLM integration with fallback mode
- Chrome Extension Manifest V3
- Gmail API
- Swagger/OpenAPI

## Architecture Overview

Gmail user opens email
-> Draftly Chrome extension extracts email context
-> Extension sends email data to Spring Boot backend
-> Backend detects category, tone, urgency, and action items
-> Backend generates a reply strategy
-> LLM or fallback generator creates a reply
-> Reply is returned to the extension
-> Extension inserts draft into Gmail
-> User edits, regenerates, approves, rejects, or saves as Gmail draft
-> Approved drafts update user tone profile
-> Future drafts use learned tone profile

Main backend modules:

- Controllers expose health, email analysis, draft, and user profile APIs.
- Services detect category/tone/urgency/action items, generate strategy, generate drafts, and learn tone profile signals.
- Repositories persist users, emails, drafts, and tone profiles.
- DTOs preserve stable public API response fields.

Main extension modules:

- `contentScript.js` injects Gmail buttons, extracts Gmail context, inserts replies, and saves drafts.
- `background.js` handles OAuth, Gmail API calls, and backend requests.
- `popup.js` provides simple sign-in status controls.

## Backend Setup

For local testing with H2:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

For PostgreSQL/default profile, start PostgreSQL and make sure the values in `backend/src/main/resources/application.yml` or local environment variables point to your database:

```text
jdbc:postgresql://localhost:5432/draftly
```

Health check:

```text
http://localhost:8080/api/health
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## Chrome Extension Setup

1. Open `chrome://extensions`.
2. Enable Developer Mode.
3. Click Load unpacked.
4. Select the `extension` folder.
5. Reload the extension after code changes.
6. Close old Gmail tabs and open Gmail fresh after extension reloads.

## Google OAuth Setup

The extension uses the Gmail compose scope:

```text
https://www.googleapis.com/auth/gmail.compose
```

Configure a Chrome Extension OAuth client in Google Cloud, add the extension ID, and add your Gmail account as a test user if the consent screen is in testing mode. Do not commit client secrets or OAuth tokens.

## Database Setup

The default profile expects PostgreSQL. The `local` profile uses H2 for quick demos and resets data when the backend stops. PostgreSQL is recommended for persistent final testing.

## API Endpoints

- `GET /api/health`
- `POST /api/emails/analyze`
- `POST /api/emails/compose`
- `GET /api/drafts/{draftId}`
- `PATCH /api/drafts/{draftId}`
- `POST /api/drafts/{draftId}/approve`
- `POST /api/drafts/{draftId}/reject`
- `POST /api/drafts/{draftId}/regenerate`
- `GET /api/users/{email}/drafts`
- `GET /api/users/{email}/tone-profile`
- `DELETE /api/users/{email}/tone-profile`

Analyze responses keep these stable fields:

- `emailId`
- `draftId`
- `category`
- `tone`
- `urgency`
- `strategy`
- `draft`

See [backend/API_TEST_EXAMPLES.md](backend/API_TEST_EXAMPLES.md) for cURL examples.

## User Tone Learning

Draftly learns from drafts the user approves. If a draft is edited with `PATCH /api/drafts/{draftId}` before approval, the edited content is used for learning.

The profile stores style signals such as preferred greeting, sign-off, average reply length, preferred tone, approved draft count, and common reusable phrases. It does not store unnecessary full private email history for tone learning.

## Gmail Draft Saving

The extension reads the latest Gmail editor content, builds a plain-text reply MIME message, and creates a Gmail draft using the Gmail API. It tries to include a thread identifier when Gmail exposes one. If thread linking is unavailable or Gmail rejects the candidate thread ID, Draftly saves a normal draft and shows a clear fallback message.

## AI Compose Mode

Draftly supports two Gmail writing modes:

1. Reply Mode: open an existing Gmail thread, click Reply, and use Draftly AI Reply to generate a response from the email context.
2. Compose Mode: click Compose Email in the Draftly toolbar, enter a natural-language prompt, and generate a full new email with a subject and body.

Compose Mode uses `POST /api/emails/compose` and stays separate from the reply analyzer, so prompt-based emails do not call the reply-mode `/api/emails/analyze` endpoint.

## Demo Flow

1. Start backend.
2. Open Swagger health endpoint.
3. Reload extension.
4. Open Gmail fresh.
5. Open an email thread and click Reply.
6. Click Draftly AI Reply.
7. Edit or regenerate the draft.
8. Save as Gmail Draft.
9. Approve a final draft to update tone profile.
10. View tone profile endpoint.

## Testing

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Use the local profile when PostgreSQL is not running:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Additional testing guidance is in [TESTING.md](TESTING.md).

## Known Limitations

- Gmail DOM extraction can vary depending on Gmail layout.
- True Gmail API `threadId` may not always be available from the DOM.
- Draft quality depends on email context and LLM availability.
- OAuth app in testing mode requires configured test users.
- Local backend must be running for the extension demo.
- H2 is for development; PostgreSQL is recommended for final testing.

## Future Improvements

- Production backend deployment
- Better Gmail thread metadata through Gmail API message access
- More advanced tone learning
- Outlook or other email-provider support
- Calendar availability integration
- Team/admin dashboard
- Privacy controls for stored email data
- Privacy-safe RAG over user-approved replies
- Better analytics for reply categories
