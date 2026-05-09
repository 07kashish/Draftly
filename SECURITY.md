# Security Policy

- Do not commit API keys, OAuth secrets, database passwords, or tokens.
- Use `.env.example` for placeholders.
- Rotate keys immediately if accidentally committed.
- Use restricted Google OAuth scopes.
- Store production secrets in environment variables or a secret manager.
- Do not log OAuth tokens or full email bodies in production logs.
- Store only needed style signals for tone learning, not unnecessary private email history.
