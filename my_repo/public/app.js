// Fieldnote — Recruitment CRM frontend.
// Talks to the Java backend over fetch() with session cookies for auth.

const STAGES = [
  { key: "APPLIED", label: "Applied", color: "#5B6A60" },
  { key: "SHORTLISTED", label: "Shortlisted", color: "#C88A34" },
  { key: "INTERVIEW_SCHEDULED", label: "Interview", color: "#5B7A96" },
  { key: "HIRED", label: "Hired", color: "#3F6B52" },
  { key: "REJECTED", label: "Rejected", color: "#A8483A" },
];

const STRATEGIES = [
  { key: "", label: "No evaluation" },
  { key: "TECHNICAL", label: "Technical" },
  { key: "HR", label: "HR" },
  { key: "BEHAVIORAL", label: "Behavioral" },
];

let jobsCache = [];
let currentUser = null;

// ---------- fetch helpers ----------

async function apiGet(path) {
  const res = await fetch(path, { credentials: "same-origin" });
  if (!res.ok) throw new Error((await res.json()).error || res.statusText);
  return res.json();
}

async function apiPost(path, fields) {
  const body = new URLSearchParams(fields).toString();
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
    credentials: "same-origin",
  });
  if (!res.ok) throw new Error((await res.json()).error || res.statusText);
  return res.json();
}

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => { toast.hidden = true; }, 3200);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

function isRecruiter() {
  return currentUser?.authenticated && currentUser.role === "RECRUITER";
}

function isLoggedIn() {
  return currentUser?.authenticated === true;
}

// ---------- auth UI ----------

async function loadSession() {
  currentUser = await apiGet("/api/auth/me");
  renderAuthBar();
}

function renderAuthBar() {
  const userEl = document.getElementById("authUser");
  const loginBtn = document.getElementById("loginBtn");
  const registerBtn = document.getElementById("registerBtn");
  const logoutBtn = document.getElementById("logoutBtn");
  const postRoleBtn = document.getElementById("postRoleBtn");

  if (isLoggedIn()) {
    userEl.textContent = `${currentUser.name} (${currentUser.role.toLowerCase()})`;
    userEl.hidden = false;
    loginBtn.hidden = true;
    registerBtn.hidden = true;
    logoutBtn.hidden = false;
    postRoleBtn.hidden = !isRecruiter();
  } else {
    userEl.hidden = true;
    loginBtn.hidden = false;
    registerBtn.hidden = false;
    logoutBtn.hidden = true;
    postRoleBtn.hidden = true;
  }
  renderJobs();
  renderPipelineControls();
}

function renderPipelineControls() {
  const board = document.getElementById("pipelineBoard");
  if (!board) return;
  board.querySelectorAll(".app-controls").forEach((el) => {
    el.style.display = isRecruiter() ? "" : "none";
  });
}

document.getElementById("loginBtn").addEventListener("click", () => openModal("loginModal"));
document.getElementById("registerBtn").addEventListener("click", () => openModal("registerModal"));

document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    await apiPost("/api/auth/logout", {});
    currentUser = { authenticated: false };
    renderAuthBar();
    showToast("Logged out.");
  } catch (err) {
    showToast("Logout failed: " + err.message);
  }
});

document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  try {
    currentUser = await apiPost("/api/auth/login", Object.fromEntries(form));
    e.target.reset();
    closeModal("loginModal");
    renderAuthBar();
    showToast("Welcome back, " + currentUser.name + ".");
  } catch (err) {
    showToast("Login failed: " + err.message);
  }
});

document.getElementById("registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  try {
    currentUser = await apiPost("/api/auth/register", Object.fromEntries(form));
    e.target.reset();
    closeModal("registerModal");
    renderAuthBar();
    showToast("Account created — you're logged in.");
  } catch (err) {
    showToast("Registration failed: " + err.message);
  }
});

document.getElementById("registerType").addEventListener("change", (e) => {
  const label = document.getElementById("registerExtraLabel");
  if (e.target.value === "RECRUITER") {
    label.firstChild.textContent = "Company ";
    label.querySelector("input").placeholder = "TechNova Ltd";
  } else {
    label.firstChild.textContent = "Resume link ";
    label.querySelector("input").placeholder = "link to your CV / portfolio";
  }
});

// ---------- tabs ----------

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((t) => { t.classList.remove("is-active"); t.setAttribute("aria-selected", "false"); });
    document.querySelectorAll(".view").forEach((v) => v.classList.remove("is-active"));
    tab.classList.add("is-active");
    tab.setAttribute("aria-selected", "true");
    document.getElementById("view-" + tab.dataset.view).classList.add("is-active");
  });
});

// ---------- Openings view ----------

async function loadJobs() {
  jobsCache = await apiGet("/api/jobs");
  jobsCache.sort((a, b) => b.visibilityScore - a.visibilityScore);
  renderJobs();
}

function renderJobs() {
  const grid = document.getElementById("jobsGrid");
  const empty = document.getElementById("jobsEmpty");

  if (jobsCache.length === 0) {
    grid.innerHTML = "";
    empty.hidden = false;
    return;
  }
  empty.hidden = true;

  grid.innerHTML = jobsCache.map((job) => `
    <article class="job-card">
      <div class="job-badges">
        ${job.featured ? '<span class="badge badge-featured">Featured</span>' : ""}
        ${job.urgent ? '<span class="badge badge-urgent">Urgent</span>' : ""}
      </div>
      <div class="job-title">${escapeHtml(job.title)}</div>
      <div class="job-company">${escapeHtml(job.company)}</div>
      <div class="job-type">${escapeHtml(job.type.replace("_", " "))}</div>
      <p class="job-desc">${escapeHtml(job.description)}</p>
      <div class="job-score">Visibility score: ${job.visibilityScore} · from the Decorator chain</div>
      <div class="job-actions">
        <button class="btn btn-primary btn-small" data-apply="${job.id}" data-title="${escapeHtml(job.title)}">Apply</button>
        ${isRecruiter() ? `
          <button class="btn btn-ghost btn-small ${job.featured ? "is-on" : ""}" data-toggle="feature" data-job="${job.id}">${job.featured ? "Unfeature" : "Feature"}</button>
          <button class="btn btn-ghost btn-small ${job.urgent ? "is-on" : ""}" data-toggle="urgent" data-job="${job.id}">${job.urgent ? "Unmark urgent" : "Mark urgent"}</button>
        ` : ""}
      </div>
    </article>
  `).join("");
}

document.getElementById("jobsGrid").addEventListener("click", async (e) => {
  const applyBtn = e.target.closest("[data-apply]");
  if (applyBtn) {
    if (!isLoggedIn()) {
      showToast("Log in to apply for a role.");
      openModal("loginModal");
      return;
    }
    openApplyModal(applyBtn.dataset.apply, applyBtn.dataset.title);
    return;
  }
  const toggleBtn = e.target.closest("[data-toggle]");
  if (toggleBtn) {
    const action = toggleBtn.dataset.toggle;
    const jobId = toggleBtn.dataset.job;
    try {
      await apiPost(`/api/jobs/${jobId}/${action}`, {});
      await loadJobs();
    } catch (err) {
      showToast("Couldn't update: " + err.message);
    }
  }
});

// ---------- Post a role ----------

function openModal(id) { document.getElementById(id).hidden = false; }
function closeModal(id) { document.getElementById(id).hidden = true; }

document.getElementById("postRoleBtn").addEventListener("click", () => {
  if (!isRecruiter()) {
    showToast("Only recruiters can post roles.");
    return;
  }
  openModal("postRoleModal");
});
document.getElementById("emptyPostRoleBtn").addEventListener("click", () => {
  if (!isRecruiter()) {
    showToast("Log in as a recruiter to post roles.");
    openModal("loginModal");
    return;
  }
  openModal("postRoleModal");
});

document.querySelectorAll("[data-close-modal]").forEach((btn) => {
  btn.addEventListener("click", () => btn.closest(".modal-backdrop").hidden = true);
});
document.querySelectorAll(".modal-backdrop").forEach((backdrop) => {
  backdrop.addEventListener("click", (e) => { if (e.target === backdrop) backdrop.hidden = true; });
});

document.getElementById("postRoleForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  try {
    await apiPost("/api/jobs", Object.fromEntries(form));
    e.target.reset();
    closeModal("postRoleModal");
    showToast("Role posted.");
    await loadJobs();
  } catch (err) {
    showToast("Couldn't post role: " + err.message);
  }
});

// ---------- Apply ----------

function openApplyModal(jobId, title) {
  document.getElementById("applyJobTitle").textContent = title;
  document.querySelector('#applyForm [name="jobId"]').value = jobId;
  if (currentUser?.email) {
    document.querySelector('#applyForm [name="candidateEmail"]').value = currentUser.email;
    document.querySelector('#applyForm [name="candidateName"]').value = currentUser.name;
  }
  openModal("applyModal");
}

document.getElementById("applyForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  try {
    await apiPost("/api/applications", Object.fromEntries(form));
    e.target.reset();
    closeModal("applyModal");
    showToast("Application submitted — check the Pipeline tab.");
    await loadApplications();
  } catch (err) {
    showToast("Couldn't submit application: " + err.message);
  }
});

// ---------- Pipeline view ----------

async function loadApplications() {
  const applications = await apiGet("/api/applications");
  renderPipeline(applications);
}

function renderPipeline(applications) {
  const board = document.getElementById("pipelineBoard");
  board.innerHTML = STAGES.map((stage) => {
    const inStage = applications.filter((a) => a.status === stage.key);
    return `
      <div class="pipeline-column" style="--stage-color:${stage.color}">
        <div class="column-head">
          <span class="column-title">${stage.label}</span>
          <span class="column-count">${inStage.length}</span>
        </div>
        <div class="column-cards">
          ${inStage.length === 0 ? '<div class="column-empty">No applications here</div>' : inStage.map((app) => renderAppCard(app)).join("")}
        </div>
      </div>
    `;
  }).join("");
  renderPipelineControls();
}

function renderAppCard(app) {
  const stageOptions = STAGES.map((s) => `<option value="${s.key}" ${s.key === app.status ? "selected" : ""}>${s.label}</option>`).join("");
  const strategyOptions = STRATEGIES.map((s) => `<option value="${s.key}">${s.label}</option>`).join("");
  const controls = isRecruiter() ? `
      <div class="app-controls">
        <select data-status-for="${app.id}">${stageOptions}</select>
        <select data-strategy-for="${app.id}">${strategyOptions}</select>
        <button class="btn btn-primary btn-small" data-advance="${app.id}">Advance</button>
      </div>
    ` : "";
  return `
    <div class="app-card" style="--stage-color:${STAGES.find((s) => s.key === app.status).color}">
      <div class="app-candidate">${escapeHtml(app.candidateName)}</div>
      <div class="app-job">${escapeHtml(app.jobTitle)}</div>
      <div class="app-id">#${escapeHtml(app.id)}</div>
      ${app.evaluationSummary ? `<div class="app-eval">${escapeHtml(app.evaluationSummary)}</div>` : ""}
      ${controls}
    </div>
  `;
}

document.getElementById("pipelineBoard").addEventListener("click", async (e) => {
  const btn = e.target.closest("[data-advance]");
  if (!btn) return;
  const appId = btn.dataset.advance;
  const status = document.querySelector(`[data-status-for="${appId}"]`).value;
  const strategy = document.querySelector(`[data-strategy-for="${appId}"]`).value;
  try {
    await apiPost(`/api/applications/${appId}/status`, { status, strategy });
    showToast("Application updated — Observers notified.");
    await loadApplications();
  } catch (err) {
    showToast("Couldn't update application: " + err.message);
  }
});

// ---------- init ----------

loadSession()
  .then(() => Promise.all([loadJobs(), loadApplications()]))
  .catch((err) => showToast("Couldn't load app: " + err.message));
