# Draftly Troubleshooting

## Backend Not Running

Check:

```text
http://localhost:8080/api/health
```

Start backend:

```bash
cd backend
mvn spring-boot:run
```

Use local H2 profile if PostgreSQL is not available:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Database Unavailable

Check the active profile and logs. The default profile expects PostgreSQL. The local profile uses H2.

For PostgreSQL, verify:

- Database server is running.
- Port is correct.
- `DATABASE_URL`, `DB_USERNAME`, and `DB_PASSWORD` are correct.
- No real credentials are committed.

## Extension Context Invalidated

This usually happens after reloading the extension while Gmail is already open.

Fix:

1. Reload Draftly in `chrome://extensions`.
2. Close all Gmail tabs.
3. Open Gmail fresh.

## OAuth Blocked

If Google blocks sign-in, add your Gmail account as a test user in the Google Cloud OAuth consent screen.

Also verify:

- OAuth app is configured.
- Chrome extension OAuth client is used.
- Extension ID matches Google Cloud configuration.

## Sign-In Stuck

Check:

- `extension/manifest.json` OAuth client ID
- extension ID in Chrome
- `identity` permission
- Gmail compose scope
- OAuth consent screen test users

## Draftly Button Not Appearing

Try:

- Refresh Gmail.
- Open a reply editor.
- Reload the extension.
- Confirm host permission includes `https://mail.google.com/*`.
- Check the browser console for content script errors.

## Vague Replies

Check:

- Extracted subject/sender logs
- Email body length and thread history length logs
- Backend category/tone/urgency/action detection
- Whether OpenRouter is configured or fallback mode is being used

Do not log full private email bodies in production.

## Save As Gmail Draft Fails

Check:

- User is signed in with Google.
- Gmail compose scope is present.
- OAuth token has not expired.
- Gmail API is enabled for the Google Cloud project.
- Extension was reloaded and Gmail opened fresh.

If thread linking is unavailable, Draftly should still save a normal Gmail draft and show:

```text
Saved as Gmail Draft. Thread linking unavailable.
```

## Port 8080 Already In Use

Find the process:

```powershell
netstat -ano | Select-String ':8080'
```

Stop it:

```powershell
Stop-Process -Id <PID>
```
