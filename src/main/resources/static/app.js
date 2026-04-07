const state = {
  accessToken: localStorage.getItem("appDefendToken") || "",
  refreshToken: localStorage.getItem("appDefendRefresh") || ""
};

const el = (id) => document.getElementById(id);

function setConsole(title, payload) {
  const text = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
  el("console-output").textContent = `${title}\n\n${text}`;
}

function setTooltip(message, type = "info") {
  const tooltip = el("integration-tooltip");
  tooltip.textContent = message;
  tooltip.className = `tooltip ${type}`;
}

function clearTooltip() {
  el("integration-tooltip").className = "tooltip hidden";
  el("integration-tooltip").textContent = "";
}

function updateIntegrationUi() {
  const isGitLab = el("integration-provider").value === "GITLAB";
  const isSaas = el("integration-deployment-mode").value === "SAAS";
  el("gitlab-fields").style.display = isGitLab ? "grid" : "none";
  el("namespace-field").style.display = isGitLab && isSaas ? "flex" : "none";
  el("namespace-preview").style.display = isGitLab && isSaas ? "block" : "none";
  updateNamespacePreview();
}

function toNamespaceUrl(baseUrl, namespacePath) {
  const sanitizedBase = (baseUrl || "").trim().replace(/\/+$/, "").replace(/\/api\/v4$/i, "");
  const sanitizedNamespace = (namespacePath || "").trim().replace(/^\/+/, "");
  if (!sanitizedBase) {
    return "Namespace URL: enter the GitLab base URL";
  }
  if (!sanitizedNamespace) {
    return `Namespace URL: ${sanitizedBase}/<namespacePath>`;
  }
  return `Namespace URL: ${sanitizedBase}/${sanitizedNamespace}`;
}

function updateNamespacePreview() {
  el("namespace-preview").textContent = toNamespaceUrl(
    el("integration-url").value,
    el("integration-namespace").value
  );
}

function summarizeResponse(data) {
  if (typeof data === "string") {
    return data;
  }
  if (Array.isArray(data)) {
    return `Request succeeded. ${data.length} item(s) returned.`;
  }
  if (data?.message) {
    return data.message;
  }
  if (data?.name && data?.providerType) {
    return `${data.providerType} integration "${data.name}" saved successfully.`;
  }
  if (data?.supportedProviders) {
    return "Supported providers loaded successfully.";
  }
  return "Request completed successfully.";
}

function summarizeError(error) {
  const payload = error?.data || error;
  if (typeof payload === "string") {
    return payload;
  }
  if (payload?.message) {
    return payload.message;
  }
  if (payload?.error) {
    return payload.error;
  }
  return "Request failed. Please review the console output.";
}

function updateAuthUi() {
  el("token-box").value = state.accessToken || "";
  el("auth-status").textContent = state.accessToken ? "Authenticated" : "Not Authenticated";
}

function persistTokens(accessToken, refreshToken = state.refreshToken) {
  state.accessToken = accessToken || "";
  state.refreshToken = refreshToken || "";
  localStorage.setItem("appDefendToken", state.accessToken);
  localStorage.setItem("appDefendRefresh", state.refreshToken);
  updateAuthUi();
}

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };

  if (state.accessToken && !options.skipAuth) {
    headers.Authorization = `Bearer ${state.accessToken}`;
  }

  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  } catch (error) {
    data = text;
  }

  if (!response.ok) {
    throw { status: response.status, data };
  }

  return data;
}

function parseRoleIds() {
  return el("user-role-ids").value
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean)
    .map(Number);
}

document.getElementById("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const result = await api("/api/v1/auth/login", {
      method: "POST",
      skipAuth: true,
      body: JSON.stringify({
        email: el("login-email").value,
        password: el("login-password").value
      })
    });
    persistTokens(result.accessToken, result.refreshToken);
    setConsole("Login Success", result);
  } catch (error) {
    setConsole("Login Failed", error.data || error);
  }
});

el("btn-clear-token").addEventListener("click", () => {
  persistTokens("", "");
  setConsole("Session Cleared", "Stored JWT tokens were removed.");
});

el("btn-clear-console").addEventListener("click", () => {
  setConsole("Console", "Ready.");
});

el("btn-health").addEventListener("click", async () => {
  try {
    const result = await api("/api/v1/health", { skipAuth: true });
    setConsole("Health", result);
  } catch (error) {
    setConsole("Health Failed", error.data || error);
  }
});

el("btn-license-status").addEventListener("click", async () => {
  try {
    const result = await api("/api/v1/licenses/status");
    el("license-status-chip").textContent = result.status || "License Loaded";
    setConsole("License Status", result);
  } catch (error) {
    setConsole("License Status Failed", error.data || error);
  }
});

el("btn-users-list").addEventListener("click", async () => {
  try {
    setConsole("Users", await api("/api/v1/users"));
  } catch (error) {
    setConsole("Users Failed", error.data || error);
  }
});

el("user-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const result = await api("/api/v1/users", {
      method: "POST",
      body: JSON.stringify({
        fullName: el("user-full-name").value,
        email: el("user-email").value,
        password: el("user-password").value,
        enabled: el("user-enabled").checked,
        roleIds: parseRoleIds()
      })
    });
    setConsole("User Created", result);
  } catch (error) {
    setConsole("Create User Failed", error.data || error);
  }
});

el("btn-roles-list").addEventListener("click", async () => {
  try {
    setConsole("Roles", await api("/api/v1/rbac/roles"));
  } catch (error) {
    setConsole("Roles Failed", error.data || error);
  }
});

el("btn-permissions-list").addEventListener("click", async () => {
  try {
    setConsole("Permissions", await api("/api/v1/rbac/permissions"));
  } catch (error) {
    setConsole("Permissions Failed", error.data || error);
  }
});

el("btn-views-list").addEventListener("click", async () => {
  try {
    setConsole("Views", await api("/api/v1/rbac/views"));
  } catch (error) {
    setConsole("Views Failed", error.data || error);
  }
});

el("role-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const result = await api("/api/v1/rbac/roles", {
      method: "POST",
      body: JSON.stringify({
        code: el("role-code").value,
        name: el("role-name").value,
        description: el("role-description").value
      })
    });
    setConsole("Role Created", result);
  } catch (error) {
    setConsole("Create Role Failed", error.data || error);
  }
});

el("permission-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const result = await api("/api/v1/rbac/permissions", {
      method: "POST",
      body: JSON.stringify({
        code: el("permission-code").value,
        name: el("permission-name").value,
        description: el("permission-description").value,
        moduleName: el("permission-module").value
      })
    });
    setConsole("Permission Created", result);
  } catch (error) {
    setConsole("Create Permission Failed", error.data || error);
  }
});

el("btn-integrations-list").addEventListener("click", async () => {
  try {
    const result = await api("/api/v1/integrations");
    setConsole("Integrations", result);
    setTooltip(summarizeResponse(result), "info");
  } catch (error) {
    setTooltip(summarizeError(error), "error");
    setConsole("Integrations Failed", error.data || error);
  }
});

el("integration-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  clearTooltip();
  const isGitLab = el("integration-provider").value === "GITLAB";
  const isSaas = el("integration-deployment-mode").value === "SAAS";
  try {
    const result = await api("/api/v1/integrations", {
      method: "POST",
      body: JSON.stringify({
        name: el("integration-name").value,
        providerType: el("integration-provider").value,
        deploymentMode: isGitLab ? el("integration-deployment-mode").value : null,
        namespacePath: isGitLab && isSaas ? el("integration-namespace").value.trim() : "",
        baseUrl: el("integration-url").value,
        credentialsJson: el("integration-credentials").value,
        active: el("integration-active").checked
      })
    });
    setTooltip(summarizeResponse(result), "success");
    setConsole("Integration Created", result);
  } catch (error) {
    setTooltip(summarizeError(error), "error");
    setConsole("Create Integration Failed", error.data || error);
  }
});

el("license-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const result = await api("/api/v1/licenses/install", {
      method: "POST",
      body: JSON.stringify({
        payloadJson: el("license-payload").value.trim(),
        signature: el("license-signature").value.trim()
      })
    });
    setConsole("License Installed", result);
  } catch (error) {
    setConsole("License Install Failed", error.data || error);
  }
});

el("btn-encrypt-bundle").addEventListener("click", async () => {
  try {
    const result = await api("/api/v1/licenses/bundles/encrypt", {
      method: "POST",
      body: JSON.stringify({
        payloadJson: el("license-payload").value.trim(),
        signature: el("license-signature").value.trim(),
        passphrase: el("bundle-passphrase").value
      })
    });
    el("bundle-output").value = JSON.stringify(result, null, 2);
    setConsole("Encrypted Bundle", result);
  } catch (error) {
    setConsole("Encrypt Bundle Failed", error.data || error);
  }
});

el("btn-license-report").addEventListener("click", async () => {
  try {
    setConsole("Capacity Report", await api("/api/v1/licenses/reports/capacity"));
  } catch (error) {
    setConsole("Capacity Report Failed", error.data || error);
  }
});

el("btn-license-audit").addEventListener("click", async () => {
  try {
    setConsole("License Audit", await api("/api/v1/licenses/audit?limit=25"));
  } catch (error) {
    setConsole("License Audit Failed", error.data || error);
  }
});

el("btn-license-debug").addEventListener("click", async () => {
  try {
    const result = await api("/api/v1/licenses/debug/verify", {
      method: "POST",
      body: JSON.stringify({
        payloadJson: el("license-payload").value.trim(),
        signature: el("license-signature").value.trim()
      })
    });
    setConsole("License Debug Verify", result);
  } catch (error) {
    setConsole("License Debug Failed", error.data || error);
  }
});

el("integration-provider").addEventListener("change", updateIntegrationUi);
el("integration-deployment-mode").addEventListener("change", updateIntegrationUi);
el("integration-url").addEventListener("input", updateNamespacePreview);
el("integration-namespace").addEventListener("input", updateNamespacePreview);

updateAuthUi();
updateIntegrationUi();
