# Draftly API Test Examples

All examples assume the backend is running at `http://localhost:8080`.

## Health

```bash
curl http://localhost:8080/api/health
```

## Analyze Email - University Decision Request

```bash
curl -X POST http://localhost:8080/api/emails/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Kashish Jain",
    "userEmail": "kashish@example.com",
    "sender": "admissions@example.com",
    "recipients": ["kashish@example.com"],
    "subject": "Update on university reply",
    "body": "Dear Kashish, we are waiting on your reply for the university acceptance. What is your decision?",
    "threadHistory": ""
  }'
```

Expected: `category` is usually `DECISION_REQUEST`, and the draft should avoid inventing an accept/decline decision.

## Analyze Email - Meeting Readiness Request

```bash
curl -X POST http://localhost:8080/api/emails/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Kashish Jain",
    "userEmail": "kashish@example.com",
    "sender": "elixir@example.com",
    "recipients": ["kashish@example.com"],
    "subject": "Meeting follow up",
    "body": "Hi Kashish, let me know when you are ready and when we can have a meeting.",
    "threadHistory": ""
  }'
```

Expected: meeting-focused draft that does not invent a date or time.

## Analyze Email - Complaint

```bash
curl -X POST http://localhost:8080/api/emails/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Kashish Jain",
    "userEmail": "kashish@example.com",
    "sender": "client@example.com",
    "recipients": ["kashish@example.com"],
    "subject": "Issue with latest update",
    "body": "The latest update caused a problem for our team and we need help resolving it.",
    "threadHistory": ""
  }'
```

Expected: polite acknowledgment and a safe follow-up without overpromising.

## Analyze Email With Gmail Metadata

```bash
curl -X POST http://localhost:8080/api/emails/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Kashish Jain",
    "userEmail": "kashish@example.com",
    "gmailMessageId": "optional-message-id",
    "gmailThreadId": "optional-thread-id",
    "sender": "client@example.com",
    "recipients": ["kashish@example.com"],
    "subject": "Project follow up",
    "body": "Please confirm the project timeline.",
    "threadHistory": "Earlier context can go here."
  }'
```

The response keeps these public fields:

```json
{
  "emailId": "...",
  "draftId": "...",
  "category": "FOLLOW_UP",
  "tone": "PROFESSIONAL",
  "urgency": "MEDIUM",
  "strategy": "...",
  "draft": "..."
}
```

## Compose Full Email From Prompt

```bash
curl -X POST http://localhost:8080/api/emails/compose \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Kashish Jain",
    "userEmail": "kashish@example.com",
    "recipient": "admissions@example.com",
    "prompt": "Write an email asking for an extension in the deposit deadline because the deposit amount is high and I need more time to arrange it.",
    "tone": "PROFESSIONAL",
    "context": "I received an offer for MSc Engineering Management.",
    "desiredLength": "MEDIUM"
  }'
```

Example response:

```json
{
  "subject": "Request for Extension of Deposit Deadline",
  "draft": "Dear Admissions Team,\n\nI hope you are doing well...",
  "tone": "PROFESSIONAL",
  "strategy": "Write a polished professional email from the user's prompt without inventing unsupported facts."
}
```

## Get Draft

```bash
curl http://localhost:8080/api/drafts/{draftId}
```

## Update Draft

```bash
curl -X PATCH http://localhost:8080/api/drafts/{draftId} \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hi Client,\n\nThank you for following up. I will review this and get back to you shortly.\n\nBest regards,\nKashish Jain"
  }'
```

## Approve Draft

```bash
curl -X POST http://localhost:8080/api/drafts/{draftId}/approve
```

Approving a draft marks it approved and updates the user's tone profile using the current draft content.

## Reject Draft

```bash
curl -X POST http://localhost:8080/api/drafts/{draftId}/reject
```

## Regenerate Draft

```bash
curl -X POST http://localhost:8080/api/drafts/{draftId}/regenerate
```

## Get Drafts For User

```bash
curl http://localhost:8080/api/users/kashish@example.com/drafts
```

## Get User Tone Profile

```bash
curl http://localhost:8080/api/users/kashish@example.com/tone-profile
```

Example response:

```json
{
  "userEmail": "kashish@example.com",
  "preferredTone": "POLITE_PROFESSIONAL",
  "preferredGreeting": "Hi",
  "preferredSignOff": "Best regards",
  "averageReplyLength": 52,
  "approvedDraftCount": 3,
  "commonPhrases": "Thank you for following up; Please let me know; I appreciate your patience"
}
```

## Reset User Tone Profile

```bash
curl -X DELETE http://localhost:8080/api/users/kashish@example.com/tone-profile
```

Example response:

```json
{
  "message": "Tone profile reset successfully"
}
```

## Full Tone Learning Flow

1. Generate draft with `POST /api/emails/analyze`.
2. Edit draft with `PATCH /api/drafts/{draftId}`.
3. Approve draft with `POST /api/drafts/{draftId}/approve`.
4. View learned profile with `GET /api/users/{email}/tone-profile`.
