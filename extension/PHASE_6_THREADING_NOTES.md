# Phase 6 Gmail Threading Notes

Draftly saves Gmail drafts as replies by building a reply MIME message and, when available, passing a Gmail thread identifier to `users.me.drafts.create`.

## Metadata Draftly Tries To Extract

- Current Gmail URL
- Last URL segment as a possible thread/message identifier
- Subject
- Original sender
- Current user email
- Latest visible email body
- Thread history text
- DOM message/thread attributes when Gmail exposes them
- `In-Reply-To` and `References` only when a usable message header is available

## Threading Behavior

Gmail's DOM does not always expose the true Gmail API `threadId`. Draftly treats URL and DOM values as best-effort candidates. If a candidate thread ID is available, Draftly sends:

```json
{
  "message": {
    "raw": "base64url-mime",
    "threadId": "candidate-thread-id"
  }
}
```

If Gmail rejects the threaded create request, Draftly retries without `threadId` and shows:

```text
Saved as Gmail Draft. Thread linking unavailable.
```

This avoids pretending a draft was linked when Gmail did not accept the thread identifier.

## MIME Reply Format

Draftly builds a plain text MIME reply with CRLF line endings:

- `To`
- `From`
- reply-safe `Subject` with `Re:` prefix
- optional `In-Reply-To`
- optional `References`
- `Content-Type: text/plain; charset=UTF-8`
- `MIME-Version: 1.0`

The MIME message is UTF-8 encoded and Base64URL encoded for the Gmail API.

## Manual Test

1. Start the backend.
2. Reload the Draftly extension.
3. Close old Gmail tabs.
4. Open Gmail fresh.
5. Open an existing email thread.
6. Click Reply.
7. Click Draftly AI Reply.
8. Edit the generated reply.
9. Click Save as Gmail Draft.
10. Check Gmail Drafts.
11. Verify recipient, `Re:` subject, latest edited body, and whether the draft appears in the original conversation.

## Known Limitations

- `gmail.compose` can create drafts but does not provide broad message-reading access.
- Gmail DOM attributes can change and may not include the true API `threadId`.
- Draftly logs clear warnings and falls back to normal draft saving when thread linking cannot be confirmed.
- Draftly does not log OAuth tokens.
