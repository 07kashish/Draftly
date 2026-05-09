# Testing Draftly

## Backend Unit and Integration Tests

```bash
cd backend
mvn clean test
```

If a backend process is running from `target`, stop it before `mvn clean test`.

Run backend:

```bash
cd backend
mvn spring-boot:run
```

Use H2 local profile when PostgreSQL is not running:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## API Tests Through Swagger

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

Test:

- `GET /api/health`
- `POST /api/emails/analyze`
- `PATCH /api/drafts/{draftId}`
- `POST /api/drafts/{draftId}/approve`
- `POST /api/drafts/{draftId}/reject`
- `POST /api/drafts/{draftId}/regenerate`
- `GET /api/users/{email}/tone-profile`
- `DELETE /api/users/{email}/tone-profile`

## Manual Gmail Extension Tests

1. Reload extension in `chrome://extensions`.
2. Close old Gmail tabs.
3. Open Gmail fresh.
4. Open an email thread.
5. Click Reply.
6. Confirm Draftly AI Reply button appears.
7. Click Draftly AI Reply.
8. Confirm generated draft is inserted.
9. Edit the draft.
10. Save as Gmail Draft.

## OAuth Tests

- Confirm Google sign-in opens.
- Confirm account is listed as a test user if OAuth app is in testing mode.
- Confirm the extension has `identity` permission.
- Confirm Gmail compose scope is present.

## Gmail Draft Save Tests

- Draft appears in Gmail Drafts.
- Recipient is original sender.
- Subject starts with `Re:`.
- Body uses latest edited text.
- If thread linking is unavailable, UI shows a clear fallback message.

## Tone Profile Tests

- Generate a draft.
- Edit it with a distinct greeting/sign-off.
- Approve it.
- Call `GET /api/users/{email}/tone-profile`.
- Confirm greeting, sign-off, average length, approved count, tone, and phrases update.

## Error Handling Tests

- Stop backend and verify extension shows backend unavailable.
- Sign out or revoke OAuth and verify sign-in error.
- Reload extension while Gmail is open and verify refresh guidance.
- Temporarily remove Gmail compose scope and verify Gmail draft permission error.
