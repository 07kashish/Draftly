# Draftly Chrome Extension

This Manifest V3 extension adds Draftly controls inside Gmail so users can generate AI replies, edit them, and save replies as Gmail drafts.

## Load Unpacked Extension

1. Open `chrome://extensions`.
2. Enable Developer Mode.
3. Click Load unpacked.
4. Select the `extension` folder.
5. Pin Draftly if you want quick access to the popup.

## Reload Extension

After editing extension files:

1. Open `chrome://extensions`.
2. Click Reload on Draftly.
3. Close old Gmail tabs.
4. Open Gmail fresh.

Gmail must be refreshed because content scripts are injected into the page. Old Gmail tabs can keep an old extension context and show `Extension context invalidated`.

## Google Sign-In

Click the Draftly extension icon and sign in with Google. The extension uses:

```text
https://www.googleapis.com/auth/gmail.compose
```

This allows creating Gmail drafts. Do not add broader scopes unless the project needs them.

## Using Draftly

### Reply Mode

1. Start the backend at `http://localhost:8080`.
2. Open Gmail.
3. Open an email thread.
4. Click Reply.
5. Click Draftly AI Reply.
6. Edit the inserted draft if needed.
7. Save as Gmail Draft.

### Compose Mode

1. Open a Gmail compose or reply editor.
2. Click Compose Email in the Draftly toolbar.
3. Enter a prompt such as `Write an email asking for a deposit deadline extension.`
4. Choose a tone and add optional context.
5. Click Generate Email.
6. Draftly inserts the generated body into Gmail and fills the subject field when Gmail exposes one.

## Draft Actions

- Regenerate, approve, and reject are backend-supported workflows.
- Approving a draft updates the user's tone profile.
- Saving as Gmail Draft stores the latest Gmail editor content in Gmail.
- Save and approve are separate actions; saving does not automatically approve.

## Gmail Draft Saving

Draftly builds a reply MIME message and calls Gmail `drafts.create`. If Gmail exposes a usable thread identifier, Draftly attempts to save the draft in the same conversation. If thread linking is unavailable, Draftly saves a normal Gmail draft and shows a fallback message.

## Troubleshooting

- Backend unavailable: start the backend and check `http://localhost:8080/api/health`.
- Database unavailable: use the `local` profile or start PostgreSQL.
- Google sign-in failed: verify OAuth client ID, extension ID, consent screen test user, and `identity` permission.
- Extension context invalidated: reload extension, close Gmail, open Gmail fresh.
- Gmail draft save failed: verify Gmail compose scope and sign in again.
- Thread linking unavailable: Gmail did not expose or accept a thread ID; the draft is still saved.
- Draftly button not appearing: refresh Gmail and check extension permissions for `https://mail.google.com/*`.
