const DRAFTLY_BACKEND_ANALYZE_URL = "http://localhost:8080/api/emails/analyze";
const DRAFTLY_BACKEND_COMPOSE_URL = "http://localhost:8080/api/emails/compose";
const DRAFTLY_BACKEND_DRAFTS_URL = "http://localhost:8080/api/drafts";
const GMAIL_PROFILE_URL = "https://gmail.googleapis.com/gmail/v1/users/me/profile";
const GMAIL_DRAFTS_URL = "https://gmail.googleapis.com/gmail/v1/users/me/drafts";
const OAUTH_CLIENT_ID_PLACEHOLDER = "PASTE_GOOGLE_CHROME_EXTENSION_CLIENT_ID_HERE";

console.log("[Draftly] Background service worker loaded");

async function getGoogleAuthToken(interactive = true) {
  console.log("[Draftly] Requesting Google auth token", { interactive });

  const manifest = chrome.runtime.getManifest();
  const oauthClientId = manifest.oauth2 && manifest.oauth2.client_id;

  if (!oauthClientId || oauthClientId === OAUTH_CLIENT_ID_PLACEHOLDER) {
    throw new Error(
      "Add your Google OAuth Chrome Extension client ID in extension/manifest.json, then reload the unpacked extension."
    );
  }

  const identityApi = globalThis.chrome && globalThis.chrome.identity;

  if (!identityApi || typeof identityApi.getAuthToken !== "function") {
    throw new Error(
      "chrome.identity.getAuthToken is unavailable. Reload the unpacked extension in Chrome and confirm manifest.json includes the identity permission."
    );
  }

  return new Promise((resolve, reject) => {
    identityApi.getAuthToken({ interactive }, (token) => {
      const lastError = chrome.runtime.lastError;

      if (lastError) {
        console.error("[Draftly] Google auth failed", lastError);
        reject(new Error(lastError.message || "Google OAuth sign-in failed"));
        return;
      }

      if (!token) {
        reject(new Error("Google OAuth did not return an auth token"));
        return;
      }

      console.log("[Draftly] Google auth token received");
      resolve(token);
    });
  });
}

async function getGmailProfile() {
  const token = await getGoogleAuthToken(true);

  const response = await fetch(GMAIL_PROFILE_URL, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });

  const data = await readJsonResponse(response);

  if (!response.ok) {
    throw new Error(data.error?.message || `Gmail profile request failed with HTTP ${response.status}`);
  }

  console.log("[Draftly] Gmail profile loaded", data.emailAddress);
  return data;
}

async function analyzeEmailWithBackend(payload) {
  console.debug("[Draftly Payload] Background forwarding analyze request", {
    subject: payload?.subject,
    sender: payload?.sender,
    hasThreadHistory: Boolean(payload?.threadHistory),
    hasGmailMessageId: Boolean(payload?.gmailMessageId),
    hasGmailThreadId: Boolean(payload?.gmailThreadId)
  });

  const response = await fetch(DRAFTLY_BACKEND_ANALYZE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  const data = await readJsonResponse(response);

  if (!response.ok) {
    throw new Error(data.message || data.error || `Draftly backend returned HTTP ${response.status}`);
  }

  console.log("[Draftly] Draftly backend response received", data);
  return data;
}

async function composeEmailWithBackend(payload) {
  console.log("[Draftly Compose] Request payload", payload);

  let response;
  try {
    response = await fetch(DRAFTLY_BACKEND_COMPOSE_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });
  } catch (error) {
    console.error("[Draftly Compose] Backend fetch failed", error);
    const backendError = new Error("Backend unavailable. Please start Draftly backend.");
    backendError.status = 0;
    throw backendError;
  }

  const data = await readJsonResponse(response);
  console.log("[Draftly Compose] Response status", response.status);
  console.log("[Draftly Compose] Response body", data);

  if (!response.ok) {
    const composeError = new Error(getComposeErrorMessage(response.status, data));
    composeError.status = response.status;
    composeError.responseBody = data;
    throw composeError;
  }

  console.log("[Draftly Compose] Email composed successfully");
  return data;
}

function getComposeErrorMessage(status, data = {}) {
  if (status === 400) {
    return "Please enter a valid compose prompt.";
  }
  if (status === 404) {
    return "Compose endpoint not found. Check backend.";
  }
  if (status >= 500) {
    return "Compose failed in backend. Check backend logs.";
  }

  return data.message || data.error || `Compose failed with HTTP ${status}.`;
}

async function updateDraftWithBackend(draftId, content) {
  if (!draftId) {
    throw new Error("Cannot update Draftly draft without a draftId");
  }

  const response = await fetch(`${DRAFTLY_BACKEND_DRAFTS_URL}/${draftId}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ content })
  });

  const data = await readJsonResponse(response);

  if (!response.ok) {
    throw new Error(data.message || data.error || `Draftly draft update returned HTTP ${response.status}`);
  }

  return data;
}

async function approveDraftWithBackend(draftId) {
  if (!draftId) {
    throw new Error("Cannot approve Draftly draft without a draftId");
  }

  const response = await fetch(`${DRAFTLY_BACKEND_DRAFTS_URL}/${draftId}/approve`, {
    method: "POST"
  });

  const data = await readJsonResponse(response);

  if (!response.ok) {
    throw new Error(data.message || data.error || `Draftly draft approval returned HTTP ${response.status}`);
  }

  return data;
}

async function rejectDraftWithBackend(draftId) {
  return postDraftActionWithBackend(draftId, "reject");
}

async function regenerateDraftWithBackend(draftId) {
  return postDraftActionWithBackend(draftId, "regenerate");
}

async function postDraftActionWithBackend(draftId, action) {
  if (!draftId) {
    throw new Error(`Cannot ${action} Draftly draft without a draftId`);
  }

  const response = await fetch(`${DRAFTLY_BACKEND_DRAFTS_URL}/${draftId}/${action}`, {
    method: "POST"
  });

  const data = await readJsonResponse(response);

  if (!response.ok) {
    throw new Error(data.message || data.error || `Draftly draft ${action} returned HTTP ${response.status}`);
  }

  return data;
}

async function createGmailDraft({ rawMessage, threadId }) {
  if (!rawMessage) {
    throw new Error("Cannot create Gmail draft without a raw MIME message");
  }

  const token = await getGoogleAuthToken(true);

  const createDraft = (message) => fetch(GMAIL_DRAFTS_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      message
    })
  });

  const message = { raw: rawMessage };
  if (threadId) {
    message.threadId = threadId;
  }

  let response = await createDraft(message);
  let data = await readJsonResponse(response);

  if (!response.ok && threadId) {
    console.warn("[Draftly Threading] Gmail draft creation with threadId failed; retrying without threadId", {
      threadId,
      status: response.status,
      error: data.error?.message || data.message || data.error
    });

    response = await createDraft({ raw: rawMessage });
    data = await readJsonResponse(response);

    if (!response.ok) {
      throw new Error(data.error?.message || `Gmail draft creation failed with HTTP ${response.status}`);
    }

    console.log("[Draftly Threading] Gmail draft create response:", {
      draftId: data.id,
      messageId: data.message?.id,
      threadId: data.message?.threadId,
      threadLinkingUnavailable: true
    });

    return {
      ...data,
      threadLinkingUnavailable: true,
      requestedThreadId: threadId
    };
  }

  if (!response.ok) {
    throw new Error(data.error?.message || `Gmail draft creation failed with HTTP ${response.status}`);
  }

  console.log("[Draftly] Gmail draft created", {
    draftId: data.id,
    messageId: data.message?.id,
    threadId: data.message?.threadId
  });
  console.log("[Draftly Threading] Gmail draft create response:", {
    draftId: data.id,
    messageId: data.message?.id,
    threadId: data.message?.threadId,
    requestedThreadId: threadId || null
  });
  return data;
}

async function readJsonResponse(response) {
  const text = await response.text();

  if (!text) {
    return {};
  }

  try {
    return JSON.parse(text);
  } catch (error) {
    console.warn("[Draftly] Response was not JSON", text);
    return { message: text };
  }
}

function respondAsync(sendResponse, action) {
  action()
    .then((data) => sendResponse({ ok: true, data }))
    .catch((error) => {
      console.error("[Draftly] Message handler failed", error);
      sendResponse({
        ok: false,
        error: error.message || "Draftly extension request failed",
        status: Number.isInteger(error.status) ? error.status : null,
        responseBody: error.responseBody || null
      });
    });

  return true;
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (!message || !message.type) {
    return false;
  }

  console.log("[Draftly] Message received", {
    type: message.type,
    tabId: sender.tab && sender.tab.id
  });

  if (message.type === "DRAFTLY_LOGIN") {
    return respondAsync(sendResponse, async () => {
      const token = await getGoogleAuthToken(true);
      return { signedIn: Boolean(token) };
    });
  }

  if (message.type === "DRAFTLY_GET_PROFILE") {
    return respondAsync(sendResponse, getGmailProfile);
  }

  if (message.type === "DRAFTLY_ANALYZE_EMAIL") {
    return respondAsync(sendResponse, () => analyzeEmailWithBackend(message.payload));
  }

  if (message.type === "DRAFTLY_COMPOSE_EMAIL") {
    return respondAsync(sendResponse, () => composeEmailWithBackend(message.payload));
  }

  if (message.type === "DRAFTLY_CREATE_GMAIL_DRAFT") {
    return respondAsync(sendResponse, () => createGmailDraft({
      rawMessage: message.rawMessage,
      threadId: message.threadId
    }));
  }

  if (message.type === "DRAFTLY_UPDATE_DRAFT") {
    return respondAsync(sendResponse, () => updateDraftWithBackend(message.draftId, message.content));
  }

  if (message.type === "DRAFTLY_APPROVE_DRAFT") {
    return respondAsync(sendResponse, () => approveDraftWithBackend(message.draftId));
  }

  if (message.type === "DRAFTLY_REJECT_DRAFT") {
    return respondAsync(sendResponse, () => rejectDraftWithBackend(message.draftId));
  }

  if (message.type === "DRAFTLY_REGENERATE_DRAFT") {
    return respondAsync(sendResponse, () => regenerateDraftWithBackend(message.draftId));
  }

  return false;
});
