const BACKEND_HEALTH_URL = "http://localhost:8080/api/health";

const statusElement = document.getElementById("popupStatus");
const messageElement = document.getElementById("popupMessage");
const loginButton = document.getElementById("loginButton");
const profileButton = document.getElementById("profileButton");
const testBackendButton = document.getElementById("testBackendButton");
const allButtons = [loginButton, profileButton, testBackendButton];

console.log("[Draftly] Popup loaded");

loginButton.addEventListener("click", handleLogin);
profileButton.addEventListener("click", handleProfileCheck);
testBackendButton.addEventListener("click", testBackend);

async function handleLogin() {
  setPopupState("Signing in...", "Opening Google OAuth sign-in.", true);

  try {
    const response = await chrome.runtime.sendMessage({ type: "DRAFTLY_LOGIN" });

    if (!response || !response.ok) {
      throw new Error(response?.error || "Google sign-in failed");
    }

    setPopupState("Signed in", "Google OAuth token is available.", false);
    console.log("[Draftly] Login response", response.data);
  } catch (error) {
    console.error("[Draftly] Login failed", error);
    setPopupState("Sign-in failed", error.message || "Could not sign in with Google.", false);
  }
}

async function handleProfileCheck() {
  setPopupState("Checking profile...", "Requesting Gmail profile.", true);

  try {
    const response = await chrome.runtime.sendMessage({ type: "DRAFTLY_GET_PROFILE" });

    if (!response || !response.ok) {
      throw new Error(response?.error || "Could not load Gmail profile");
    }

    setPopupState("Profile loaded", response.data.emailAddress || "Gmail profile loaded.", false);
    console.log("[Draftly] Gmail profile", response.data);
  } catch (error) {
    console.error("[Draftly] Profile check failed", error);
    setPopupState("Profile failed", error.message || "Could not load Gmail profile.", false);
  }
}

async function testBackend() {
  setPopupState("Testing backend...", "Checking Draftly backend health.", true);

  try {
    const response = await fetch(BACKEND_HEALTH_URL, { method: "GET" });
    const text = await response.text();

    if (!response.ok) {
      throw new Error(`Backend health returned HTTP ${response.status}`);
    }

    setPopupState("Backend healthy", text || "Health endpoint responded successfully.", false);
    console.log("[Draftly] Backend health response", { status: response.status, text });
  } catch (error) {
    console.error("[Draftly] Backend health check failed", error);
    setPopupState("Backend failed", error.message || "Could not reach http://localhost:8080/api/health.", false);
  }
}

function setPopupState(status, message, isLoading) {
  statusElement.textContent = status;
  messageElement.textContent = message || "";

  allButtons.forEach((button) => {
    button.disabled = isLoading;
  });
}
