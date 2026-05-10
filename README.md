# Draftly - Gmail AI Reply Agent

Draftly is a Gmail-based AI writing assistant. It adds a small toolbar inside Gmail so you can generate replies to existing email threads, compose new emails from prompts, save drafts into Gmail, and approve finished drafts so Draftly learns your writing style over time.

The project has two parts:

- `backend/`: Spring Boot API for email analysis, draft generation, tone learning, and draft workflows.
- `extension/`: Chrome Manifest V3 extension that runs inside Gmail.

## What Draftly Does

Draftly helps with two common Gmail workflows.

**Reply Mode**

Open an email thread, click Reply, then click `Draftly AI Reply`. Draftly reads the visible Gmail context, sends it to the backend, generates a reply, and inserts it into the Gmail editor.

**Compose Mode**

Open a Gmail compose box, click `Compose Email`, type a prompt like `Write an email asking for a deposit deadline extension`, and Draftly generates a full email with a subject and body.

Draftly can also:

- Detect email category, tone, urgency, and action items.
- Generate a reply strategy before writing the draft.
- Use OpenRouter when an API key is configured.
- Fall back to rule-based drafts when the LLM is unavailable.
- Regenerate, approve, reject, and update drafts.
- Learn tone preferences from approved/edited drafts.
- Save the latest editor content as a Gmail draft.
- Use Gmail display names for greetings when Gmail exposes them.

## Tech Stack

- Java 17
- Spring Boot 3.3
- Spring Data JPA / Hibernate
- PostgreSQL for persistent local data
- H2 for quick local demos/tests
- OpenRouter LLM API with fallback mode
- Chrome Extension Manifest V3
- Gmail API `gmail.compose` scope
- Swagger/OpenAPI

## How It Works

```text
Gmail thread or compose box
-> Draftly Chrome extension extracts context
-> Extension calls Spring Boot backend
-> Backend analyzes intent, tone, urgency, and action items
-> Backend generates a strategy
-> OpenRouter or fallback generator creates a draft
-> Extension inserts the draft into Gmail
-> User edits, regenerates, approves, rejects, or saves as Gmail draft
-> Approved drafts update the user's tone profile
-> Future drafts use the learned profile
```

## Prerequisites

Install:

- Java 17+
- Maven
- Google Chrome
- PostgreSQL, unless you use the H2 local profile
- An OpenRouter API key, optional but recommended
- A Google Cloud OAuth client for the Chrome extension

## Docker Setup

Draftly is Dockerized using Docker Compose.

The Docker setup runs:

- Spring Boot backend
- PostgreSQL database

The Chrome extension must be loaded manually in Chrome because browser extensions run inside the browser.

### Prerequisites

- Docker Desktop
- OpenRouter API key

### Environment Setup

Create a `.env` file in the project root:

```env
OPENROUTER_API_KEY=your_openrouter_api_key_here
OPENROUTER_MODEL=openrouter/free

## Environment Setup

Create a root `.env` file from the example:

```powershell
Copy-Item .env.example .env
```

Fill in values:

```env
OPENROUTER_API_KEY=your_openrouter_api_key_here
OPENROUTER_MODEL=openrouter/free
DATABASE_URL=jdbc:postgresql://localhost:5432/draftly
DB_USERNAME=draftly
DB_PASSWORD=draftly
GOOGLE_CLIENT_ID=your_google_client_id_here
```

The backend imports both `../.env` and `backend/.env`, so running from `backend/` can still read the root `.env`.

Never commit `.env`. It is ignored by `.gitignore`.

## Run Backend

### Option 1: PostgreSQL

Start PostgreSQL and create a database/user matching your `.env`.

Quick Docker example:

```powershell
docker run --name draftly-postgres -e POSTGRES_DB=draftly -e POSTGRES_USER=draftly -e POSTGRES_PASSWORD=draftly -p 5432:5432 -d postgres:16
```

Run backend:

```powershell
cd backend
mvn spring-boot:run
```

### Option 2: H2 Local Demo

Use this when PostgreSQL is not running:

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

H2 data is temporary and resets when the backend stops.

## Check Backend

Health check:

```text
http://localhost:8080/api/health
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Run tests:

```powershell
cd backend
mvn clean test
```

## Load Chrome Extension

1. Open Chrome.
2. Go to `chrome://extensions`.
3. Enable `Developer mode`.
4. Click `Load unpacked`.
5. Select the `extension` folder.
6. Pin the Draftly extension if you want quick access.

After changing extension files:

1. Click `Reload` on the Draftly extension.
2. Close all old Gmail tabs.
3. Open Gmail fresh.

Old Gmail tabs can keep an old extension context and cause `Extension context invalidated`.

## Google OAuth Setup

Draftly uses this Gmail scope:

```text
https://www.googleapis.com/auth/gmail.compose
```

In Google Cloud:

1. Create/configure an OAuth consent screen.
2. Add your Gmail account as a test user if the app is in testing mode.
3. Create a Chrome Extension OAuth client.
4. Add your Chrome extension ID.
5. Put the OAuth client ID in `extension/manifest.json`.

Do not commit OAuth client secrets or tokens. This extension should only need the OAuth client ID, not a client secret.

## Use Draftly In Gmail

### Reply Mode

1. Start the backend.
2. Reload the extension.
3. Close old Gmail tabs and open Gmail fresh.
4. Open an existing email thread.
5. Click Gmail `Reply`.
6. Click `Draftly AI Reply`.
7. Review the inserted draft.
8. Edit the draft if needed.
9. Use one of the Draftly actions:
   - `Regenerate`: ask backend for another version.
   - `Approve`: mark the final draft as approved and update tone learning.
   - `Reject`: mark the draft as rejected.
   - `Save as Gmail Draft`: save the latest editor text into Gmail drafts.

### Compose Mode

1. Open Gmail `Compose`.
2. Click `Compose Email` in the Draftly toolbar.
3. Type a prompt, for example:

```text
Write an email to my university asking for an extension in the deposit deadline because the amount is high and I need more time to arrange it.
```

4. Choose tone.
5. Add optional context if helpful.
6. Click `Generate Email`.
7. Draftly inserts the generated email body and fills the subject when Gmail exposes the subject field.

Compose Mode calls:

```text
POST /api/emails/compose
```

Reply Mode calls:

```text
POST /api/emails/analyze
```

The two modes are separate.

## Tone Learning

Draftly learns only from approved drafts.

When you edit a Gmail draft and approve it:

1. The extension sends the latest edited text to the backend.
2. The backend marks the draft as approved.
3. Draftly updates the user tone profile.

The profile stores style signals:

- Preferred tone
- Preferred greeting
- Preferred sign-off
- Average reply length
- Approved draft count
- Common useful phrases

Draftly does not need to store full private email history for tone learning.

View profile:

```text
GET /api/users/{email}/tone-profile
```

Reset profile:

```text
DELETE /api/users/{email}/tone-profile
```

## Gmail Draft Saving

When you click `Save as Gmail Draft`, Draftly:

1. Reads the current Gmail editor content.
2. Uses the latest edited text, not stale generated text.
3. Builds a plain-text MIME email.
4. Calls Gmail `drafts.create`.
5. Attempts best-effort thread linking when Gmail exposes a usable thread ID.

If thread linking is unavailable, Draftly still saves a normal Gmail draft and shows a fallback message.

## API Endpoints

Core endpoints:

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

Reply analysis responses keep these stable fields:

- `emailId`
- `draftId`
- `category`
- `tone`
- `urgency`
- `strategy`
- `draft`

Full API examples are in [backend/API_TEST_EXAMPLES.md](backend/API_TEST_EXAMPLES.md).

## Demo Flow

1. Start backend:

```powershell
cd backend
mvn spring-boot:run
```

2. Open health endpoint:

```text
http://localhost:8080/api/health
```

3. Open Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

4. Reload Draftly extension.
5. Close old Gmail tabs.
6. Open Gmail fresh.
7. Open an email thread.
8. Click `Reply`.
9. Click `Draftly AI Reply`.
10. Edit the reply.
11. Click `Save as Gmail Draft`.
12. Click `Approve`.
13. Check tone profile endpoint.
14. Open Gmail Compose and test `Compose Email`.

## Troubleshooting

**Backend unavailable**

Start backend and check:

```text
http://localhost:8080/api/health
```

**Port 8080 already in use**

Another backend instance is already running. Stop it, or run this in PowerShell to find it:

```powershell
netstat -ano | findstr :8080
```

**Database unavailable**

Start PostgreSQL, check `.env`, or use:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**OpenRouter key missing**

If logs show `OPENROUTER_API_KEY is missing or blank`, make sure root `.env` has a real key and restart the backend.

**Extension context invalidated**

Reload extension, close Gmail, then open Gmail fresh.

**Google sign-in failed**

Check OAuth client ID, extension ID, consent screen test users, and `identity` permission.

**Draftly button not appearing**

Refresh Gmail, make sure the extension is loaded, and confirm host permissions include `https://mail.google.com/*`.

**Gmail draft save failed**

Confirm Google sign-in works and the OAuth scope includes `gmail.compose`.

**Thread linking unavailable**

Gmail does not always expose a true Gmail API thread ID in the DOM. Draftly saves the draft anyway, but may not link it to the original thread.

## Security Notes

- Do not commit `.env`.
- Do not commit real API keys, OAuth tokens, client secrets, database passwords, or credential JSON files.
- Do not log OAuth tokens.
- Avoid logging full private email bodies in production.
- Store only needed style signals for tone learning.

## Known Limitations

- Gmail DOM structure can change.
- True Gmail thread IDs are not always available from the page.
- Draft quality depends on prompt/email context and LLM availability.
- OAuth apps in testing mode require test users.
- Backend must run locally for the extension demo.
- H2 is useful for demos but PostgreSQL is better for persistent testing.

## Future Improvements

- Production deployment
- Gmail metadata through Gmail API message reads
- Better privacy controls for stored email data
- More advanced tone learning
- Outlook support
- Calendar availability integration
- Team/admin dashboard
- Analytics for email categories and reply workflows