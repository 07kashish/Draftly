const DRAFTLY_REPLY_BUTTON_CLASS = "draftly-ai-reply-button";
const DRAFTLY_SAVE_BUTTON_CLASS = "draftly-save-draft-button";
const DRAFTLY_STATUS_CLASS = "draftly-ai-status";
const DRAFTLY_TOOLBAR_CLASS = "draftly-toolbar";
const DRAFTLY_COMPOSE_BUTTON_CLASS = "draftly-compose-button";
const DRAFTLY_REGENERATE_BUTTON_CLASS = "draftly-regenerate-button";
const DRAFTLY_APPROVE_BUTTON_CLASS = "draftly-approve-button";
const DRAFTLY_REJECT_BUTTON_CLASS = "draftly-reject-button";
const DRAFTLY_COMPOSE_PANEL_CLASS = "draftly-compose-panel";

let latestDraftState = null;

console.log("[Draftly] Gmail content script loaded");

function findComposeEditors(root = document) {
  const selectors = [
    'div[contenteditable="true"][role="textbox"]',
    'div[contenteditable="true"][aria-label*="Message Body"]',
    'div[contenteditable="true"][aria-label*="message body"]',
    'div[contenteditable="true"][g_editable="true"]'
  ];

  return Array.from(root.querySelectorAll(selectors.join(","))).filter((editor) => {
    const searchBox = editor.closest("[aria-label='Search mail'], [role='search']");
    return !searchBox && editor.offsetParent !== null;
  });
}

function getComposeContainer(editor) {
  return (
    editor.closest('[role="dialog"]') ||
    editor.closest(".M9") ||
    editor.closest("form") ||
    editor.parentElement
  );
}

function addDraftlyButtons() {
  const editors = findComposeEditors();

  editors.forEach((editor) => {
    const container = getComposeContainer(editor);
    if (!container || container.querySelector('[data-draftly-toolbar="true"]')) {
      return;
    }

    const toolbar = document.createElement("div");
    toolbar.className = DRAFTLY_TOOLBAR_CLASS;
    toolbar.setAttribute("data-draftly-toolbar", "true");

    const replyButton = document.createElement("button");
    replyButton.type = "button";
    replyButton.className = `${DRAFTLY_REPLY_BUTTON_CLASS} draftly-primary-button`;
    replyButton.textContent = "Draftly AI Reply";
    replyButton.addEventListener("click", () => handleDraftlyReplyClick(editor, toolbar));

    const composeButton = document.createElement("button");
    composeButton.type = "button";
    composeButton.className = `${DRAFTLY_COMPOSE_BUTTON_CLASS} draftly-secondary-button`;
    composeButton.textContent = "Compose Email";
    composeButton.addEventListener("click", () => toggleComposePanel(editor, toolbar));

    const regenerateButton = document.createElement("button");
    regenerateButton.type = "button";
    regenerateButton.className = `${DRAFTLY_REGENERATE_BUTTON_CLASS} draftly-secondary-button`;
    regenerateButton.textContent = "Regenerate";
    regenerateButton.disabled = true;
    regenerateButton.hidden = true;
    regenerateButton.addEventListener("click", () => handleRegenerateDraftClick(editor, toolbar));

    const approveButton = document.createElement("button");
    approveButton.type = "button";
    approveButton.className = `${DRAFTLY_APPROVE_BUTTON_CLASS} draftly-secondary-button`;
    approveButton.textContent = "Approve";
    approveButton.disabled = true;
    approveButton.hidden = true;
    approveButton.addEventListener("click", () => handleApproveDraftClick(editor, toolbar));

    const rejectButton = document.createElement("button");
    rejectButton.type = "button";
    rejectButton.className = `${DRAFTLY_REJECT_BUTTON_CLASS} draftly-danger-button`;
    rejectButton.textContent = "Reject";
    rejectButton.disabled = true;
    rejectButton.hidden = true;
    rejectButton.addEventListener("click", () => handleRejectDraftClick(toolbar));

    const saveButton = document.createElement("button");
    saveButton.type = "button";
    saveButton.className = `${DRAFTLY_SAVE_BUTTON_CLASS} draftly-secondary-button`;
    saveButton.textContent = "Save as Gmail Draft";
    saveButton.disabled = true;
    saveButton.hidden = true;
    saveButton.addEventListener("click", () => handleSaveDraftClick(editor, toolbar));

    const status = document.createElement("span");
    status.className = DRAFTLY_STATUS_CLASS;
    status.setAttribute("aria-live", "polite");

    toolbar.appendChild(replyButton);
    toolbar.appendChild(composeButton);
    toolbar.appendChild(regenerateButton);
    toolbar.appendChild(approveButton);
    toolbar.appendChild(rejectButton);
    toolbar.appendChild(saveButton);
    toolbar.appendChild(status);

    insertDraftlyToolbar(container, editor, toolbar);

    console.log("[Draftly] Buttons added to Gmail compose box", editor);
  });
}

function insertDraftlyToolbar(container, editor, toolbar) {
  const gmailToolbar = findGmailBottomToolbar(container, editor);
  if (gmailToolbar && gmailToolbar.parentElement) {
    gmailToolbar.parentElement.insertBefore(toolbar, gmailToolbar);
    ensureComposePanel(container, editor, toolbar);
    return;
  }

  editor.insertAdjacentElement("afterend", toolbar);
  ensureComposePanel(container, editor, toolbar);
}

function ensureComposePanel(container, editor, toolbar) {
  if (container.querySelector('[data-draftly-compose-panel="true"]')) {
    return;
  }

  toolbar.insertAdjacentElement("beforebegin", createComposePanel(editor, toolbar));
}

function findGmailBottomToolbar(container, editor) {
  const candidates = Array.from(container.querySelectorAll(".btC, .aDh, [role='toolbar']"))
    .filter((node) => !node.closest('[data-draftly-toolbar="true"]') && node.offsetParent !== null);

  const editorRect = editor.getBoundingClientRect();
  return candidates.find((node) => node.getBoundingClientRect().top >= editorRect.bottom - 4)
    || candidates[candidates.length - 1]
    || null;
}

function createComposePanel(editor, toolbar) {
  const panel = document.createElement("div");
  panel.className = DRAFTLY_COMPOSE_PANEL_CLASS;
  panel.setAttribute("data-draftly-compose-panel", "true");
  panel.hidden = true;

  const title = document.createElement("div");
  title.className = "draftly-compose-title";
  title.textContent = "Draftly Compose";

  const promptLabel = document.createElement("label");
  promptLabel.textContent = "What email do you want to write?";
  const promptInput = document.createElement("textarea");
  promptInput.className = "draftly-compose-prompt";
  promptInput.rows = 3;
  promptInput.placeholder = "Write an email asking for a deposit deadline extension.";
  promptLabel.appendChild(promptInput);

  const toneLabel = document.createElement("label");
  toneLabel.textContent = "Tone";
  const toneSelect = document.createElement("select");
  toneSelect.className = "draftly-compose-tone";
  ["PROFESSIONAL", "POLITE", "FORMAL", "CONCISE"].forEach((tone) => {
    const option = document.createElement("option");
    option.value = tone;
    option.textContent = tone.charAt(0) + tone.slice(1).toLowerCase();
    toneSelect.appendChild(option);
  });
  toneLabel.appendChild(toneSelect);

  const contextLabel = document.createElement("label");
  contextLabel.textContent = "Optional context";
  const contextInput = document.createElement("textarea");
  contextInput.className = "draftly-compose-context";
  contextInput.rows = 2;
  contextInput.placeholder = "Add details the email should use.";
  contextLabel.appendChild(contextInput);

  const actions = document.createElement("div");
  actions.className = "draftly-compose-actions";

  const generateButton = document.createElement("button");
  generateButton.type = "button";
  generateButton.className = "draftly-compose-generate draftly-primary-button";
  generateButton.textContent = "Generate Email";
  generateButton.addEventListener("click", () => handleComposeEmailGenerate(editor, toolbar, panel));

  const cancelButton = document.createElement("button");
  cancelButton.type = "button";
  cancelButton.className = "draftly-compose-cancel draftly-secondary-button";
  cancelButton.textContent = "Cancel";
  cancelButton.addEventListener("click", () => {
    panel.hidden = true;
  });

  actions.appendChild(generateButton);
  actions.appendChild(cancelButton);
  panel.appendChild(title);
  panel.appendChild(promptLabel);
  panel.appendChild(toneLabel);
  panel.appendChild(contextLabel);
  panel.appendChild(actions);

  return panel;
}

function toggleComposePanel(editor, toolbar) {
  const container = getComposeContainer(editor);
  const panel = container?.querySelector('[data-draftly-compose-panel="true"]');
  if (!panel) {
    return;
  }

  panel.hidden = !panel.hidden;
  if (!panel.hidden) {
    panel.querySelector(".draftly-compose-prompt")?.focus();
  }
}

async function handleComposeEmailGenerate(editor, toolbar, panel) {
  const replyButton = toolbar.querySelector(`.${DRAFTLY_REPLY_BUTTON_CLASS}`);
  const saveButton = toolbar.querySelector(`.${DRAFTLY_SAVE_BUTTON_CLASS}`);
  const status = toolbar.querySelector(`.${DRAFTLY_STATUS_CLASS}`);
  const promptInput = panel.querySelector(".draftly-compose-prompt");
  const toneSelect = panel.querySelector(".draftly-compose-tone");
  const contextInput = panel.querySelector(".draftly-compose-context");

  try {
    const prompt = promptInput.value.trim();
    if (!prompt) {
      throw new Error("Enter a prompt before generating an email.");
    }

    setToolbarLoading(replyButton, saveButton, status, true, "Composing email...");
    const profileResponse = await chrome.runtime.sendMessage({ type: "DRAFTLY_GET_PROFILE" });
    if (!profileResponse || !profileResponse.ok) {
      throw new Error(profileResponse?.error || "Could not load Gmail profile");
    }

    const gmailProfile = profileResponse.data || {};
    const payload = {
      userName: "Kashish Jain",
      userEmail: gmailProfile.emailAddress,
      recipient: extractComposeRecipient(),
      prompt,
      tone: toneSelect.value,
      context: contextInput.value.trim(),
      desiredLength: "MEDIUM"
    };

    console.log("[Draftly Compose] Request payload", payload);
    const composeResponse = await chrome.runtime.sendMessage({
      type: "DRAFTLY_COMPOSE_EMAIL",
      payload
    });
    console.log("[Draftly Compose] Response status", composeResponse?.status ?? (composeResponse?.ok ? 200 : "no-response"));
    console.log("[Draftly Compose] Response body", composeResponse?.data || composeResponse?.responseBody || composeResponse);

    if (!composeResponse) {
      throw new Error("No response from Draftly extension background. Reload the extension and reopen Gmail.");
    }

    if (!composeResponse.ok) {
      const error = new Error(composeResponse.error || getComposeStatusMessage(composeResponse.status));
      error.status = composeResponse.status;
      error.responseBody = composeResponse.responseBody;
      throw error;
    }

    const subject = composeResponse.data?.subject || "";
    const draft = composeResponse.data?.draft || "";
    if (!draft) {
      throw new Error("Backend did not return composed email content.");
    }

    insertSubjectIfAvailable(subject);
    replaceDraftInEditor(findNearestEditableEditor(editor), draft);

    latestDraftState = {
      mode: "compose",
      draft,
      subject,
      profile: gmailProfile,
      backendResponse: composeResponse.data,
      gmailDraftId: null,
      gmailSavedThreadId: null,
      payload
    };

    saveButton.hidden = false;
    saveButton.disabled = false;
    setDraftActionButtons(toolbar, true, true);
    console.log("[Draftly Compose] Email composed successfully", {
      subject,
      draftLength: draft.length
    });
    setToolbarLoading(replyButton, saveButton, status, false, "Email composed.");
  } catch (error) {
    console.error("[Draftly Compose] Could not compose email", error);
    setToolbarLoading(replyButton, saveButton, status, false, normalizeComposeErrorMessage(error));
  }
}

async function handleDraftlyReplyClick(editor, toolbar) {
  const replyButton = toolbar.querySelector(`.${DRAFTLY_REPLY_BUTTON_CLASS}`);
  const saveButton = toolbar.querySelector(`.${DRAFTLY_SAVE_BUTTON_CLASS}`);
  const status = toolbar.querySelector(`.${DRAFTLY_STATUS_CLASS}`);

  try {
    setToolbarLoading(replyButton, saveButton, status, true, "Getting Gmail profile...");

    const profileResponse = await chrome.runtime.sendMessage({ type: "DRAFTLY_GET_PROFILE" });
    if (!profileResponse || !profileResponse.ok) {
      throw new Error(profileResponse?.error || "Could not load Gmail profile");
    }

    const gmailProfile = profileResponse.data || {};
    const composeEditor = findNearestEditableEditor(editor);
    const extractedSubject = extractSubject();
    const extractedThreadText = extractThreadText();
    const extractedBody = extractLatestEmailBody();
    const senderDetails = extractSenderDetails(extractedBody || extractedThreadText || "");
    const extractedSender = senderDetails.sender;
    const gmailContext = extractGmailContext({
      currentUserEmail: gmailProfile.emailAddress,
      subject: extractedSubject,
      sender: extractedSender
    });

    if (!extractedBody) {
      console.warn("[Draftly] Could not confidently extract latest email body.");
    }

    const payload = {
      userName: "Kashish Jain",
      userEmail: gmailProfile.emailAddress,
      sender: extractedSender || "client@example.com",
      senderName: senderDetails.displayName || null,
      senderEmail: senderDetails.email || null,
      recipients: [gmailProfile.emailAddress],
      subject: extractedSubject || "Gmail Reply",
      body: extractedBody || extractedThreadText || "Please help me draft a reply.",
      threadHistory: extractedThreadText || "",
      gmailMessageId: gmailContext.gmailMessageId || gmailContext.possibleMessageId || null,
      gmailThreadId: gmailContext.gmailThreadId || null
    };

    console.debug("[Draftly Payload] Extracted sender", extractedSender);
    console.log("[Draftly Sender] displayName:", senderDetails.displayName || "");
    console.log("[Draftly Sender] email:", senderDetails.email || "");
    console.log("[Draftly Sender] finalGreetingName:", senderDetails.displayName || cleanEmailLocalPartForGreeting(senderDetails.email) || "");
    console.debug("[Draftly Payload] Extracted subject", extractedSubject);
    console.debug("[Draftly Payload] Email body length", (extractedBody || "").length);
    console.debug("[Draftly Payload] Thread history length", (extractedThreadText || "").length);
    logGmailThreadingContext(gmailContext);
    setToolbarLoading(replyButton, saveButton, status, true, "Generating draft...");

    const analyzeResponse = await chrome.runtime.sendMessage({
      type: "DRAFTLY_ANALYZE_EMAIL",
      payload
    });

    if (!analyzeResponse || !analyzeResponse.ok) {
      throw new Error(analyzeResponse?.error || "Draftly backend did not return a draft");
    }

    const draft = analyzeResponse.data && analyzeResponse.data.draft;
    if (!draft) {
      throw new Error("Backend response did not include a draft field");
    }

    insertDraftIntoEditor(composeEditor, draft);

    latestDraftState = {
      emailId: analyzeResponse.data.emailId,
      draftId: analyzeResponse.data.draftId,
      draft,
      category: analyzeResponse.data.category,
      tone: analyzeResponse.data.tone,
      urgency: analyzeResponse.data.urgency,
      strategy: analyzeResponse.data.strategy,
      gmailContext,
      payload,
      profile: gmailProfile,
      backendResponse: analyzeResponse.data,
      gmailDraftId: null,
      gmailSavedThreadId: null
    };

    saveButton.hidden = false;
    saveButton.disabled = false;
    setDraftActionButtons(toolbar, false, false);
    setToolbarLoading(replyButton, saveButton, status, false, "Draft inserted.");
    console.log("[Draftly] Draft inserted and stored in memory", latestDraftState);
  } catch (error) {
    console.error("[Draftly] Could not generate draft", error);
    setToolbarLoading(replyButton, saveButton, status, false, error.message || "Draftly failed.");
  }
}

async function handleSaveDraftClick(editor, toolbar) {
  const replyButton = toolbar.querySelector(`.${DRAFTLY_REPLY_BUTTON_CLASS}`);
  const saveButton = toolbar.querySelector(`.${DRAFTLY_SAVE_BUTTON_CLASS}`);
  const status = toolbar.querySelector(`.${DRAFTLY_STATUS_CLASS}`);

  try {
    if (!latestDraftState || !latestDraftState.draft) {
      throw new Error("Generate a Draftly reply before saving a Gmail draft.");
    }

    const composeEditor = findNearestEditableEditor(editor);
    const currentDraftContent = getEditorPlainText(composeEditor) || latestDraftState.draft;

    setToolbarLoading(replyButton, saveButton, status, true, "Updating Draftly draft...");

    const draftId = latestDraftState.draftId || (latestDraftState.backendResponse && latestDraftState.backendResponse.draftId);
    if (draftId && currentDraftContent !== latestDraftState.draft) {
      const updateResponse = await chrome.runtime.sendMessage({
        type: "DRAFTLY_UPDATE_DRAFT",
        draftId,
        content: currentDraftContent
      });

      if (!updateResponse || !updateResponse.ok) {
        throw new Error(updateResponse?.error || "Backend unavailable. Please start Draftly backend.");
      }

      latestDraftState.draft = currentDraftContent;
      latestDraftState.backendResponse = updateResponse.data;
    }

    const gmailContext = latestDraftState.gmailContext || {};
    const threadIdForDraft = gmailContext.gmailThreadId || gmailContext.possibleThreadId || "";

    if (!threadIdForDraft) {
      console.warn("[Draftly] Gmail thread ID could not be extracted from DOM.");
    }

    console.log("[Draftly Threading] Gmail URL:", gmailContext.gmailUrl || window.location.href);
    console.log("[Draftly Threading] Possible thread ID:", gmailContext.possibleThreadId || "");
    console.log("[Draftly Threading] Subject:", gmailContext.subject || latestDraftState.subject || latestDraftState.payload.subject);
    console.log("[Draftly Threading] To:", gmailContext.sender || latestDraftState.payload.sender || latestDraftState.payload.recipient);
    console.log("[Draftly Threading] In-Reply-To:", gmailContext.inReplyTo || "");

    setToolbarLoading(replyButton, saveButton, status, true, "Saving Gmail draft...");

    const rawMessage = buildReplyMime({
      to: gmailContext.sender || latestDraftState.payload.sender || latestDraftState.payload.recipient,
      from: latestDraftState.profile.emailAddress,
      subject: gmailContext.subject || latestDraftState.subject || latestDraftState.payload.subject,
      body: currentDraftContent,
      inReplyTo: gmailContext.inReplyTo,
      references: gmailContext.references
    });

    const response = await chrome.runtime.sendMessage({
      type: "DRAFTLY_CREATE_GMAIL_DRAFT",
      rawMessage,
      threadId: threadIdForDraft || null
    });

    if (!response || !response.ok) {
      throw new Error(response?.error || "Could not save Gmail draft. Please check Gmail permissions.");
    }

    latestDraftState.gmailDraftId = response.data?.id || null;
    latestDraftState.gmailSavedThreadId = response.data?.message?.threadId || null;

    const threadLinked = Boolean(threadIdForDraft) && !response.data?.threadLinkingUnavailable;
    const successMessage = threadLinked
      ? "Saved as Gmail Draft."
      : "Saved as Gmail Draft. Thread linking unavailable.";

    setToolbarLoading(replyButton, saveButton, status, false, successMessage);
    console.log("[Draftly] Gmail draft created", {
      draftId: latestDraftState.gmailDraftId,
      messageId: response.data?.message?.id,
      threadId: latestDraftState.gmailSavedThreadId
    });
  } catch (error) {
    console.error("[Draftly] Could not save Gmail draft", error);
    setToolbarLoading(replyButton, saveButton, status, false, normalizeSaveErrorMessage(error));
  }
}

async function handleRegenerateDraftClick(editor, toolbar) {
  const replyButton = toolbar.querySelector(`.${DRAFTLY_REPLY_BUTTON_CLASS}`);
  const saveButton = toolbar.querySelector(`.${DRAFTLY_SAVE_BUTTON_CLASS}`);
  const status = toolbar.querySelector(`.${DRAFTLY_STATUS_CLASS}`);

  try {
    const draftId = getCurrentDraftId();
    if (!draftId) {
      throw new Error("Generate a Draftly reply before regenerating.");
    }

    setToolbarLoading(replyButton, saveButton, status, true, "Regenerating draft...");
    const response = await chrome.runtime.sendMessage({
      type: "DRAFTLY_REGENERATE_DRAFT",
      draftId
    });

    if (!response || !response.ok) {
      throw new Error(response?.error || "Could not regenerate draft.");
    }

    const regeneratedDraft = response.data?.content;
    if (!regeneratedDraft) {
      throw new Error("Backend did not return regenerated draft content.");
    }

    replaceDraftInEditor(findNearestEditableEditor(editor), regeneratedDraft);
    latestDraftState.draft = regeneratedDraft;
    latestDraftState.backendResponse = response.data;
    setToolbarLoading(replyButton, saveButton, status, false, "Draft regenerated.");
  } catch (error) {
    console.error("[Draftly] Could not regenerate draft", error);
    setToolbarLoading(replyButton, saveButton, status, false, error.message || "Regenerate failed.");
  }
}

async function handleApproveDraftClick(editor, toolbar) {
  const replyButton = toolbar.querySelector(`.${DRAFTLY_REPLY_BUTTON_CLASS}`);
  const saveButton = toolbar.querySelector(`.${DRAFTLY_SAVE_BUTTON_CLASS}`);
  const status = toolbar.querySelector(`.${DRAFTLY_STATUS_CLASS}`);

  try {
    const draftId = getCurrentDraftId();
    if (!draftId) {
      throw new Error("Generate a Draftly reply before approving.");
    }

    setToolbarLoading(replyButton, saveButton, status, true, "Approving draft...");
    await patchDraftFromEditorIfNeeded(editor);

    const response = await chrome.runtime.sendMessage({
      type: "DRAFTLY_APPROVE_DRAFT",
      draftId
    });

    if (!response || !response.ok) {
      throw new Error(response?.error || "Could not approve draft.");
    }

    latestDraftState.backendResponse = response.data;
    setToolbarLoading(replyButton, saveButton, status, false, "Draft approved.");
  } catch (error) {
    console.error("[Draftly] Could not approve draft", error);
    setToolbarLoading(replyButton, saveButton, status, false, error.message || "Approve failed.");
  }
}

async function handleRejectDraftClick(toolbar) {
  const replyButton = toolbar.querySelector(`.${DRAFTLY_REPLY_BUTTON_CLASS}`);
  const saveButton = toolbar.querySelector(`.${DRAFTLY_SAVE_BUTTON_CLASS}`);
  const status = toolbar.querySelector(`.${DRAFTLY_STATUS_CLASS}`);

  try {
    const draftId = getCurrentDraftId();
    if (!draftId) {
      throw new Error("Generate a Draftly reply before rejecting.");
    }

    setToolbarLoading(replyButton, saveButton, status, true, "Rejecting draft...");
    const response = await chrome.runtime.sendMessage({
      type: "DRAFTLY_REJECT_DRAFT",
      draftId
    });

    if (!response || !response.ok) {
      throw new Error(response?.error || "Could not reject draft.");
    }

    latestDraftState.backendResponse = response.data;
    setToolbarLoading(replyButton, saveButton, status, false, "Draft rejected.");
  } catch (error) {
    console.error("[Draftly] Could not reject draft", error);
    setToolbarLoading(replyButton, saveButton, status, false, error.message || "Reject failed.");
  }
}

async function patchDraftFromEditorIfNeeded(editor) {
  const draftId = getCurrentDraftId();
  const composeEditor = findNearestEditableEditor(editor);
  const currentDraftContent = getEditorPlainText(composeEditor) || latestDraftState?.draft || "";

  if (!draftId || !latestDraftState || currentDraftContent === latestDraftState.draft) {
    return;
  }

  const updateResponse = await chrome.runtime.sendMessage({
    type: "DRAFTLY_UPDATE_DRAFT",
    draftId,
    content: currentDraftContent
  });

  if (!updateResponse || !updateResponse.ok) {
    throw new Error(updateResponse?.error || "Could not update Draftly draft.");
  }

  latestDraftState.draft = currentDraftContent;
  latestDraftState.backendResponse = updateResponse.data;
}

function getCurrentDraftId() {
  return latestDraftState?.draftId || latestDraftState?.backendResponse?.draftId || latestDraftState?.backendResponse?.id || "";
}

function setToolbarLoading(replyButton, saveButton, status, isLoading, message) {
  const toolbar = replyButton.closest('[data-draftly-toolbar="true"]');
  if (toolbar) {
    toolbar.querySelectorAll("button").forEach((button) => {
      if (!button.hidden) {
        button.disabled = isLoading;
      }
    });
  }

  replyButton.disabled = isLoading;
  replyButton.textContent = isLoading ? "Draftly is thinking..." : "Draftly AI Reply";

  if (saveButton && !saveButton.hidden) {
    saveButton.disabled = isLoading || !latestDraftState;
  }

  status.textContent = message || "";
  status.classList.toggle("is-loading", isLoading);
}

function setDraftActionButtons(toolbar, disabled, hidden) {
  [
    DRAFTLY_REGENERATE_BUTTON_CLASS,
    DRAFTLY_APPROVE_BUTTON_CLASS,
    DRAFTLY_REJECT_BUTTON_CLASS
  ].forEach((className) => {
    const button = toolbar.querySelector(`.${className}`);
    if (button) {
      button.disabled = disabled;
      button.hidden = hidden;
    }
  });
}

function findNearestEditableEditor(editor) {
  const container = getComposeContainer(editor);
  if (!container) {
    return editor;
  }

  const visibleEditors = findComposeEditors(container);
  return visibleEditors[visibleEditors.length - 1] || editor;
}

function extractSubject() {
  const subjectInput = document.querySelector('input[name="subjectbox"]');
  if (subjectInput && subjectInput.value.trim()) {
    return subjectInput.value.trim();
  }

  const subjectNodes = [
    document.querySelector("h2.hP"),
    document.querySelector("[data-thread-perm-id] h2"),
    document.querySelector("[role='main'] h2")
  ];

  return firstUsefulText(subjectNodes);
}

function extractSenderDetails(emailBodyText = "") {
  const senderNodes = [
    document.querySelector(".gD[email]"),
    document.querySelector("span[email]"),
    document.querySelector(".go")
  ];

  for (const node of senderNodes) {
    if (!node) {
      continue;
    }

    const email = cleanEmailAddress(node.getAttribute("email") || extractEmailAddress(node.getAttribute("title")) || "");
    const visibleName = cleanSenderDisplayName(node.innerText || node.textContent, email);
    const titleName = cleanSenderDisplayName(node.getAttribute("title"), email);
    const displayName = visibleName || titleName;

    if (displayName || email) {
      return buildSenderDetails(displayName, email, emailBodyText);
    }
  }

  return buildSenderDetails("", "", emailBodyText);
}

function buildSenderDetails(displayName, email, emailBodyText) {
  const signatureName = displayName ? "" : extractSignatureName(emailBodyText);
  const finalDisplayName = displayName || signatureName || "";
  const finalEmail = cleanEmailAddress(email);
  const sender = finalDisplayName && finalEmail
    ? `${finalDisplayName} <${finalEmail}>`
    : finalDisplayName || finalEmail || "";

  return {
    displayName: finalDisplayName,
    email: finalEmail,
    sender
  };
}

function cleanSenderDisplayName(value, email = "") {
  let text = cleanText(value || "");
  if (!text) {
    return "";
  }

  if (email) {
    text = text.replace(email, "").replace(/[<>()]/g, " ");
  }

  const emailInText = extractEmailAddress(text);
  if (emailInText) {
    text = text.replace(emailInText, "").replace(/[<>()]/g, " ");
  }

  text = text
    .replace(/\bto me\b/gi, " ")
    .replace(/\bme\b/gi, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (!text || text.includes("@")) {
    return "";
  }

  return text;
}

function extractEmailAddress(value) {
  const match = String(value || "").match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i);
  return match ? match[0] : "";
}

function cleanEmailAddress(value) {
  return extractEmailAddress(value);
}

function cleanEmailLocalPartForGreeting(email) {
  if (!email || !email.includes("@")) {
    return "";
  }

  const localPart = email.split("@")[0]
    .replace(/\d+/g, " ")
    .replace(/[._+-]+/g, " ")
    .replace(/[^A-Za-z\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (!localPart) {
    return "";
  }

  const first = splitMergedLastName(localPart.split(/\s+/)[0]);
  return first.charAt(0).toUpperCase() + first.slice(1).toLowerCase();
}

function splitMergedLastName(name) {
  const lower = String(name || "").toLowerCase();
  const likelyMergedLastNames = ["jain", "shah", "patel", "singh", "kumar", "gupta", "sharma"];
  for (const lastName of likelyMergedLastNames) {
    if (lower.endsWith(lastName) && lower.length > lastName.length + 2) {
      return lower.slice(0, -lastName.length);
    }
  }
  return lower;
}

function extractSignatureName(emailBodyText) {
  const lines = String(emailBodyText || "")
    .split(/\r?\n/)
    .map((line) => cleanText(line))
    .filter(Boolean);

  const signOffPattern = /^(best regards|regards|thank you|thanks|sincerely|warm regards),?$/i;
  for (let index = lines.length - 2; index >= 0; index -= 1) {
    if (signOffPattern.test(lines[index])) {
      const candidate = lines[index + 1] || "";
      if (/^[A-Za-z][A-Za-z\s.'-]{1,60}$/.test(candidate) && !candidate.includes("@")) {
        return candidate;
      }
    }
  }

  return "";
}

function extractComposeRecipient() {
  const recipientSelectors = [
    'textarea[name="to"]',
    'input[name="to"]',
    'span[email]',
    '.vR span[email]'
  ];

  for (const selector of recipientSelectors) {
    const node = document.querySelector(selector);
    if (!node) {
      continue;
    }

    const email = node.getAttribute("email") || node.value || node.textContent;
    if (email && email.trim()) {
      return email.trim();
    }
  }

  return "";
}

function insertSubjectIfAvailable(subject) {
  const subjectInput = document.querySelector('input[name="subjectbox"]');
  if (!subjectInput) {
    console.log("[Draftly Compose] Gmail subject field was not available; inserted body only.");
    return;
  }

  subjectInput.value = subject || "";
  subjectInput.dispatchEvent(new Event("input", { bubbles: true }));
  subjectInput.dispatchEvent(new Event("change", { bubbles: true }));
}

function extractGmailContext({ currentUserEmail, subject, sender }) {
  const gmailUrl = window.location.href;
  const possibleThreadId = extractPossibleThreadIdFromUrl(gmailUrl);
  const latestMessageNode = findLatestMessageNode();
  const possibleMessageId = getFirstAttribute(latestMessageNode, [
    "data-message-id",
    "data-legacy-message-id",
    "data-local-message-id"
  ]);
  const gmailThreadId = extractThreadIdFromDom(latestMessageNode);
  const gmailMessageId = extractMessageIdFromDom(latestMessageNode);
  const inReplyTo = extractOriginalMessageHeader(latestMessageNode, ["data-rfc822-message-id", "data-message-id"]);
  const references = extractReferences(latestMessageNode, inReplyTo);

  const context = {
    gmailUrl,
    possibleThreadId,
    possibleMessageId,
    gmailThreadId,
    gmailMessageId,
    subject: subject || "Gmail Reply",
    sender: sender || "",
    recipients: currentUserEmail ? [currentUserEmail] : [],
    inReplyTo,
    references
  };

  if (!gmailThreadId && !possibleThreadId) {
    console.warn("[Draftly] Gmail thread ID could not be extracted from DOM.");
  }

  return context;
}

function extractPossibleThreadIdFromUrl(gmailUrl) {
  try {
    const url = new URL(gmailUrl);
    const hash = url.hash || "";
    const segments = hash
      .replace(/^#/, "")
      .split("/")
      .map((segment) => decodeURIComponent(segment).trim())
      .filter(Boolean);
    const lastSegment = segments[segments.length - 1] || "";

    if (!lastSegment || ["inbox", "drafts", "sent", "starred", "snoozed"].includes(lastSegment.toLowerCase())) {
      return "";
    }

    return lastSegment.split("?")[0];
  } catch (error) {
    console.warn("[Draftly Threading] Could not parse Gmail URL", error);
    return "";
  }
}

function findLatestMessageNode() {
  const messageNodes = Array.from(document.querySelectorAll("[data-message-id], [data-legacy-message-id]"))
    .filter((node) => node.offsetParent !== null);

  return messageNodes[messageNodes.length - 1] || null;
}

function extractThreadIdFromDom(latestMessageNode) {
  const candidateNode = latestMessageNode || document.querySelector("[data-thread-id], [data-legacy-thread-id]");
  return getFirstAttribute(candidateNode, [
    "data-thread-id",
    "data-legacy-thread-id",
    "data-thread-perm-id"
  ]);
}

function extractMessageIdFromDom(latestMessageNode) {
  return getFirstAttribute(latestMessageNode, [
    "data-message-id",
    "data-legacy-message-id"
  ]);
}

function extractOriginalMessageHeader(latestMessageNode, attributeNames) {
  const value = getFirstAttribute(latestMessageNode, attributeNames);
  if (!value) {
    return "";
  }

  return value.startsWith("<") && value.endsWith(">") ? value : "";
}

function extractReferences(latestMessageNode, inReplyTo) {
  const references = getFirstAttribute(latestMessageNode, ["data-references", "references"]);
  return references || inReplyTo || "";
}

function getFirstAttribute(node, names) {
  if (!node) {
    return "";
  }

  for (const name of names) {
    const value = node.getAttribute(name);
    if (value && value.trim()) {
      return value.trim();
    }
  }

  return "";
}

function logGmailThreadingContext(gmailContext) {
  console.log("[Draftly Threading] Gmail URL:", gmailContext.gmailUrl);
  console.log("[Draftly Threading] Possible thread ID:", gmailContext.possibleThreadId || "");
  console.log("[Draftly Threading] Subject:", gmailContext.subject || "");
  console.log("[Draftly Threading] To:", gmailContext.sender || "");
  console.log("[Draftly Threading] In-Reply-To:", gmailContext.inReplyTo || "");
}

function extractLatestEmailBody() {
  const bodyNodes = Array.from(document.querySelectorAll("[data-message-id] .a3s, .adn.ads .a3s, .a3s.aiL, .a3s"))
    .filter(isVisibleEmailBodyNode);

  if (!bodyNodes.length) {
    return "";
  }

  const latestBody = bodyNodes[bodyNodes.length - 1];
  return cleanEmailBodyText(latestBody.innerText || latestBody.textContent);
}

function extractThreadText() {
  const threadNodes = Array.from(
    document.querySelectorAll("[data-message-id] .a3s, .adn.ads .a3s, .a3s.aiL, .a3s")
  ).filter(isVisibleEmailBodyNode);

  const text = threadNodes
    .map((node) => cleanEmailBodyText(node.innerText || node.textContent))
    .filter(Boolean)
    .join("\n\n---\n\n");

  return text.slice(0, 12000);
}

function isVisibleEmailBodyNode(node) {
  if (!node || node.offsetParent === null) {
    return false;
  }

  if (
    node.closest(`.${DRAFTLY_TOOLBAR_CLASS}`) ||
    node.closest('[contenteditable="true"]') ||
    node.closest('[role="dialog"]') ||
    node.closest("form") ||
    node.closest(".gmail_quote") ||
    node.closest(".gmail_signature")
  ) {
    return false;
  }

  const text = cleanEmailBodyText(node.innerText || node.textContent);
  if (!text || text.length < 3) {
    return false;
  }

  const lower = text.toLowerCase();
  return !lower.startsWith("hi client,\n\nthank you for your email");
}

function firstUsefulText(nodes) {
  for (const node of nodes) {
    const text = node ? cleanText(node.innerText || node.textContent) : "";
    if (text) {
      return text;
    }
  }

  return "";
}

function cleanText(value) {
  return (value || "").replace(/\s+/g, " ").trim();
}

function cleanEmailBodyText(value) {
  return (value || "")
    .replace(/\r/g, "\n")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .replace(/[ \t]{2,}/g, " ")
    .trim();
}

function getEditorPlainText(editor) {
  if (!editor) {
    return "";
  }

  return cleanEmailBodyText(editor.innerText || editor.textContent || "");
}

function insertDraftIntoEditor(editor, draft) {
  if (!editor) {
    throw new Error("Could not find Gmail reply editor");
  }

  editor.focus();

  const inserted = document.execCommand && document.execCommand("insertText", false, draft);

  if (!inserted) {
    editor.innerText = draft;
    editor.dispatchEvent(new InputEvent("input", { bubbles: true, inputType: "insertText", data: draft }));
  }
}

function replaceDraftInEditor(editor, draft) {
  if (!editor) {
    throw new Error("Could not find Gmail reply editor");
  }

  editor.focus();
  editor.innerText = draft;
  editor.dispatchEvent(new InputEvent("input", { bubbles: true, inputType: "insertText", data: draft }));
}

function buildReplyMime({ to, from, subject, body, inReplyTo, references }) {
  const headers = [];
  appendHeader(headers, "To", to || "client@example.com");
  appendHeader(headers, "From", from || "");
  appendHeader(headers, "Subject", makeReplySubject(subject || "Gmail Reply"), true);
  appendHeader(headers, "In-Reply-To", inReplyTo || "");
  appendHeader(headers, "References", references || "");
  headers.push("Content-Type: text/plain; charset=UTF-8");
  headers.push("MIME-Version: 1.0");
  headers.push("Content-Transfer-Encoding: 8bit");

  const normalizedBody = (body || "").replace(/\r?\n/g, "\r\n");
  const mime = `${headers.join("\r\n")}\r\n\r\n${normalizedBody}`;
  return base64UrlEncodeUnicode(mime);
}

function appendHeader(headers, name, value, encodeValue = false) {
  if (!value) {
    return;
  }

  const sanitized = sanitizeHeaderValue(value);
  if (!sanitized) {
    return;
  }

  headers.push(`${name}: ${encodeValue ? encodeMimeHeaderValue(sanitized) : sanitized}`);
}

function sanitizeHeaderValue(value) {
  return String(value || "").replace(/[\r\n]+/g, " ").replace(/\s+/g, " ").trim();
}

function makeReplySubject(subject) {
  const cleanSubject = sanitizeHeaderValue(subject || "Gmail Reply");
  return /^re:/i.test(cleanSubject) ? cleanSubject : `Re: ${cleanSubject}`;
}

function encodeMimeHeaderValue(value) {
  if (/^[\x00-\x7F]*$/.test(value)) {
    return value;
  }

  return `=?UTF-8?B?${base64EncodeUnicode(value)}?=`;
}

function base64EncodeUnicode(value) {
  const utf8Binary = encodeURIComponent(value).replace(/%([0-9A-F]{2})/g, (_match, hex) =>
    String.fromCharCode(parseInt(hex, 16))
  );

  return btoa(utf8Binary);
}

function base64UrlEncodeUnicode(value) {
  return base64EncodeUnicode(value).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function normalizeSaveErrorMessage(error) {
  const message = error?.message || "";
  const lower = message.toLowerCase();

  if (lower.includes("oauth") || lower.includes("auth") || lower.includes("sign")) {
    return "Please sign in with Google.";
  }
  if (lower.includes("failed to fetch") || lower.includes("draftly backend") || lower.includes("backend")) {
    return "Backend unavailable. Please start Draftly backend.";
  }
  if (lower.includes("extension context invalidated")) {
    return "Please refresh Gmail after reloading Draftly.";
  }
  if (lower.includes("gmail") || lower.includes("permission")) {
    return "Could not save Gmail draft. Please check Gmail permissions.";
  }

  return message || "Could not save Gmail draft. Please check Gmail permissions.";
}

function normalizeComposeErrorMessage(error) {
  const statusMessage = getComposeStatusMessage(error?.status);
  if (statusMessage) {
    return statusMessage;
  }

  const message = error?.message || "";
  const lower = message.toLowerCase();

  if (lower.includes("extension context invalidated")) {
    return "Please refresh Gmail after reloading Draftly.";
  }
  if (lower.includes("failed to fetch") || lower.includes("backend unavailable") || lower.includes("backend")) {
    return "Backend unavailable. Please start Draftly backend.";
  }
  if (lower.includes("compose endpoint not found")) {
    return "Compose endpoint not found. Check backend.";
  }
  if (lower.includes("valid compose prompt") || lower.includes("prompt")) {
    return "Please enter a valid compose prompt.";
  }

  return message || "Could not compose email.";
}

function getComposeStatusMessage(status) {
  if (status === 400) {
    return "Please enter a valid compose prompt.";
  }
  if (status === 404) {
    return "Compose endpoint not found. Check backend.";
  }
  if (status >= 500) {
    return "Compose failed in backend. Check backend logs.";
  }
  if (status === 0) {
    return "Backend unavailable. Please start Draftly backend.";
  }

  return "";
}

const observer = new MutationObserver(() => {
  addDraftlyButtons();
});

observer.observe(document.body, {
  childList: true,
  subtree: true
});

addDraftlyButtons();
