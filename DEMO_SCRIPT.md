# Draftly 2-Minute Demo Script

Hi, this is Draftly, a Gmail AI Reply Agent.

The problem is simple: replying to emails takes more time than it should. You need to understand what the sender wants, decide the right action, choose the right tone, and avoid accidentally promising a date, payment, decision, or availability.

Draftly solves this inside Gmail. It reads the current email context, sends it to a Spring Boot backend, detects the email category, tone, urgency, and action items, then generates a safe reply strategy. The reply is generated through an LLM when available, with a fallback mode so the project still works during demos.

For the live flow, I open an email thread in Gmail and click Reply. Draftly adds a Draftly AI Reply button inside the Gmail editor. When I click it, the extension extracts the subject, sender, latest email body, and thread context, then calls the backend. The generated reply is inserted directly into Gmail.

Now I can edit the reply like a normal Gmail draft. If I need another version, the backend supports regenerate. If the draft is good, I can approve it. Approved drafts update a lightweight tone profile, such as preferred greeting, sign-off, average reply length, tone, and common phrases. Future replies use that profile, so Draftly gradually sounds closer to the user.

Finally, I click Save as Gmail Draft. Draftly reads the latest edited text, builds a Gmail-compatible MIME reply, and saves it through the Gmail API. It attempts to link the draft to the same thread when Gmail exposes a usable thread identifier, and safely falls back to a normal draft if not.

The final value is a practical Gmail assistant that saves time, keeps the user in control, learns from approved writing style, and avoids unsafe hardcoded commitments.
