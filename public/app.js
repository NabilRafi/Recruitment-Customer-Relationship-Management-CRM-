// Cadre — Talent Operations frontend.
// Plain fetch() against the Java backend, session cookies for auth.
// No framework, no build step.

const STAGES = [
  { key: "APPLIED",             label: "Applied",     color: "#5A6880" },
  { key: "SHORTLISTED",         label: "Shortlisted", color: "#9A6B1F" },
  { key: "INTERVIEW_SCHEDULED", label: "Interview",   color: "#2F5D8C" },
  { key: "HIRED",               label: "Offer",       color: "#2E6B4F" },
  { key: "REJECTED",            label: "Closed",      color: "#8C2F39" },
];

const STRATEGIES = [
  { key: "",           label: "No evaluation" },
  { key: "TECHNICAL",  label: "Technical assessment" },
  { key: "HR",         label: "HR / culture fit" },
  { key: "BEHAVIORAL", label: "Behavioural" },
];

let jobsCache = [];
let currentUser = null;

// ---------- helpers ----------

async function apiGet(path) {
  const res = await fetch(path, { credentials: "same-origin" });
  if (!res.ok) throw new Error((await res.json()).error || res.statusText);
  return res.json();
}

async function apiPost(path, fields) {
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(fields).toString(),
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
  showToast._t = setTimeout(() => { toast.hidden = true; }, 3600);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

function stageOf(key) {
  return STAGES.find((s) => s.key === key) || STAGES[0];
}

const isLoggedIn  = () => currentUser?.authenticated === true;
const isRecruiter = () => isLoggedIn() && currentUser.role === "RECRUITER";
const isCandidate = () => isLoggedIn() && currentUser.role === "CANDIDATE";

// ---------- gate vs app ----------

function showGate() {
  document.getElementById("authGate").hidden = false;
  document.getElementById("appShell").hidden = true;
}

function showApp() {
  document.getElementById("authGate").hidden = true;
  document.getElementById("appShell").hidden = false;
  applyRoleToChrome();
}

/**
 * Shows only the navigation and controls this role is allowed to use.
 *
 * Note this is presentation only. The server independently enforces every
 * one of these rules - the Pipeline data is filtered in ApplicationsHandler,
 * so hiding the tab is a convenience, not the security boundary.
 */
function applyRoleToChrome() {
  document.getElementById("authUser").textContent =
    `${currentUser.name} · ${currentUser.role.toLowerCase()}`;

  document.querySelectorAll("[data-recruiter-only]").forEach((el) => {
    el.hidden = !isRecruiter();
  });
  document.querySelectorAll("[data-candidate-only]").forEach((el) => {
    el.hidden = !isCandidate();
  });
  document.getElementById("postRoleBtn").hidden = !isRecruiter();

  document.getElementById("openingsSub").textContent = isRecruiter()
    ? "Roles you and your team have posted. Feature or flag a role to lift its visibility score."
    : "Browse open positions and apply. You'll be emailed at every stage.";

  // If the active tab is not allowed for this role, fall back to Open roles.
  const active = document.querySelector(".tab.is-active");
  if (!active || active.hidden) switchView("openings");
}

function switchView(name) {
  document.querySelectorAll(".tab").forEach((t) => {
    const on = t.dataset.view === name;
    t.classList.toggle("is-active", on);
    t.setAttribute("aria-selected", String(on));
  });
  document.querySelectorAll(".view").forEach((v) => {
    v.classList.toggle("is-active", v.id === "view-" + name);
  });
}

document.getElementById("mainTabs").addEventListener("click", (e) => {
  const tab = e.target.closest(".tab");
  if (tab) switchView(tab.dataset.view);
});

document.addEventListener("click", (e) => {
  const goto = e.target.closest("[data-goto]");
  if (goto) switchView(goto.dataset.goto);
});

// ---------- gate forms ----------

document.querySelectorAll(".gate-tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".gate-tab").forEach((t) => t.classList.remove("is-active"));
    tab.classList.add("is-active");
    document.getElementById("loginForm").classList.toggle("is-active", tab.dataset.gate === "login");
    document.getElementById("registerForm").classList.toggle("is-active", tab.dataset.gate === "register");
  });
});

document.getElementById("registerType").addEventListener("change", (e) => {
  const label = document.getElementById("registerExtraLabel");
  const input = label.querySelector("input");
  const labels = {
    CANDIDATE: ["Resume link", "Link to your CV or portfolio"],
    RECRUITER: ["Company", "TechNova Ltd"],
    COMPANY:   ["Industry", "Software & IT services"],
  };
  const [text, placeholder] = labels[e.target.value] || labels.CANDIDATE;
  label.firstChild.textContent = text + " ";
  input.placeholder = placeholder;
});

document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    currentUser = await apiPost("/api/auth/login", Object.fromEntries(new FormData(e.target)));
    e.target.reset();
    showApp();
    await refreshAll();
    showToast(`Signed in as ${currentUser.name}.`);
  } catch (err) {
    showToast("Sign in failed — " + err.message);
  }
});

document.getElementById("registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    currentUser = await apiPost("/api/auth/register", Object.fromEntries(new FormData(e.target)));
    e.target.reset();
    showApp();
    await refreshAll();
    showToast("Account created. Welcome to Cadre.");
  } catch (err) {
    showToast("Couldn't create account — " + err.message);
  }
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    await apiPost("/api/auth/logout", {});
  } catch (err) {
    // Even if the call fails, drop the local session.
  }
  currentUser = { authenticated: false };
  jobsCache = [];
  showGate();
});

// ---------- Open roles ----------

async function loadJobs() {
  jobsCache = await apiGet("/api/jobs");
  jobsCache.sort((a, b) => b.visibilityScore - a.visibilityScore);
  renderJobs();
}

function renderJobs() {
  const grid = document.getElementById("jobsGrid");
  const empty = document.getElementById("jobsEmpty");
  document.getElementById("emptyPostRoleBtn").hidden = !isRecruiter();

  if (!jobsCache.length) {
    grid.innerHTML = "";
    empty.hidden = false;
    return;
  }
  empty.hidden = true;

  grid.innerHTML = jobsCache.map((job) => {
    const meta = [
      job.type.replace("_", " "),
      job.location,
      job.salaryRange,
    ].filter(Boolean).map((m) => `<span>${escapeHtml(m)}</span>`).join("");

    const actions = isCandidate()
      ? `<button class="btn btn-primary btn-small" data-apply="${job.id}" data-title="${escapeHtml(job.title)}">Apply</button>`
      : isRecruiter()
        ? `<button class="btn btn-ghost btn-small ${job.featured ? "is-on" : ""}" data-toggle="feature" data-job="${job.id}">${job.featured ? "Unfeature" : "Feature"}</button>
           <button class="btn btn-ghost btn-small ${job.urgent ? "is-on" : ""}" data-toggle="urgent" data-job="${job.id}">${job.urgent ? "Clear urgent" : "Mark urgent"}</button>`
        : "";

    return `
      <article class="job-card">
        <div class="job-badges">
          ${job.featured ? '<span class="badge badge-featured">Featured</span>' : ""}
          ${job.urgent ? '<span class="badge badge-urgent">Urgent</span>' : ""}
        </div>
        <div class="job-title">${escapeHtml(job.title)}</div>
        <div class="job-company">${escapeHtml(job.company)}</div>
        <div class="job-meta">${meta}</div>
        <p class="job-desc">${escapeHtml(job.description)}</p>
        <div class="job-score">Visibility ${job.visibilityScore} · computed by the Decorator chain</div>
        <div class="job-actions">${actions}</div>
      </article>
    `;
  }).join("");
}

document.getElementById("jobsGrid").addEventListener("click", async (e) => {
  const applyBtn = e.target.closest("[data-apply]");
  if (applyBtn) {
    openApplyModal(applyBtn.dataset.apply, applyBtn.dataset.title);
    return;
  }
  const toggleBtn = e.target.closest("[data-toggle]");
  if (toggleBtn) {
    try {
      await apiPost(`/api/jobs/${toggleBtn.dataset.job}/${toggleBtn.dataset.toggle}`, {});
      await loadJobs();
    } catch (err) {
      showToast("Couldn't update role — " + err.message);
    }
  }
});

// ---------- modals ----------

const openModal = (id) => { document.getElementById(id).hidden = false; };
const closeModal = (id) => { document.getElementById(id).hidden = true; };

document.querySelectorAll("[data-close-modal]").forEach((btn) => {
  btn.addEventListener("click", () => btn.closest(".modal-backdrop").hidden = true);
});
document.querySelectorAll(".modal-backdrop").forEach((backdrop) => {
  backdrop.addEventListener("click", (e) => { if (e.target === backdrop) backdrop.hidden = true; });
});

document.getElementById("postRoleBtn").addEventListener("click", () => openModal("postRoleModal"));
document.getElementById("emptyPostRoleBtn").addEventListener("click", () => openModal("postRoleModal"));

document.getElementById("postRoleForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    await apiPost("/api/jobs", Object.fromEntries(new FormData(e.target)));
    e.target.reset();
    closeModal("postRoleModal");
    showToast("Role posted.");
    await loadJobs();
  } catch (err) {
    showToast("Couldn't post role — " + err.message);
  }
});

function openApplyModal(jobId, title) {
  document.getElementById("applyJobTitle").textContent = title;
  document.querySelector('#applyForm [name="jobId"]').value = jobId;
  document.querySelector('#applyForm [name="candidateEmail"]').value = currentUser.email || "";
  document.querySelector('#applyForm [name="candidateName"]').value = currentUser.name || "";
  openModal("applyModal");
}

document.getElementById("applyForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    await apiPost("/api/applications", Object.fromEntries(new FormData(e.target)));
    closeModal("applyModal");
    showToast("Application submitted. Check your email for confirmation.");
    await loadApplications();
    switchView("mine");
  } catch (err) {
    showToast("Couldn't submit application — " + err.message);
  }
});

// ---------- applications ----------

async function loadApplications() {
  // The server decides what comes back: recruiters get everything,
  // candidates get only their own rows.
  const applications = await apiGet("/api/applications");
  if (isRecruiter()) renderPipeline(applications);
  if (isCandidate()) renderMyApplications(applications);
}

function renderPipeline(applications) {
  document.getElementById("pipelineBoard").innerHTML = STAGES.map((stage) => {
    const inStage = applications.filter((a) => a.status === stage.key);
    return `
      <div class="pipeline-column" style="--stage-color:${stage.color}">
        <div class="column-head">
          <span class="column-title">${stage.label}</span>
          <span class="column-count">${inStage.length}</span>
        </div>
        <div class="column-cards">
          ${inStage.length === 0
            ? '<div class="column-empty">Nothing here</div>'
            : inStage.map(renderAppCard).join("")}
        </div>
      </div>`;
  }).join("");
}

function renderAppCard(app) {
  const stageOptions = STAGES.map((s) =>
    `<option value="${s.key}" ${s.key === app.status ? "selected" : ""}>${s.label}</option>`).join("");
  const strategyOptions = STRATEGIES.map((s) =>
    `<option value="${s.key}">${s.label}</option>`).join("");

  return `
    <div class="app-card" style="--stage-color:${stageOf(app.status).color}">
      <div class="app-candidate">${escapeHtml(app.candidateName)}</div>
      <div class="app-job">${escapeHtml(app.jobTitle)}</div>
      <div class="app-id">#${escapeHtml(app.id)}</div>
      ${app.evaluationSummary ? `<div class="app-eval">${escapeHtml(app.evaluationSummary)}</div>` : ""}
      <div class="app-controls">
        <select data-status-for="${app.id}">${stageOptions}</select>
        <select data-strategy-for="${app.id}">${strategyOptions}</select>
        <textarea data-interview-for="${app.id}" rows="2"
                  placeholder="Interview details — date, time, place (sent to the candidate)"></textarea>
        <button class="btn btn-primary btn-small" data-advance="${app.id}">Move stage</button>
      </div>
    </div>`;
}

function renderMyApplications(applications) {
  const wrap = document.getElementById("myApplications");
  const empty = document.getElementById("myAppsEmpty");

  if (!applications.length) {
    wrap.innerHTML = "";
    empty.hidden = false;
    return;
  }
  empty.hidden = true;

  wrap.innerHTML = applications.map((app) => {
    const stage = stageOf(app.status);
    return `
      <article class="my-app">
        <div class="my-app-main">
          <div class="my-app-role">${escapeHtml(app.jobTitle)}</div>
          <div class="my-app-co">Reference ${escapeHtml(app.id)}</div>
          ${app.evaluationSummary
            ? `<div class="my-app-eval">${escapeHtml(app.evaluationSummary)}</div>` : ""}
        </div>
        <span class="status-pill" style="--stage-color:${stage.color}">${stage.label}</span>
      </article>`;
  }).join("");
}

document.getElementById("pipelineBoard").addEventListener("click", async (e) => {
  const btn = e.target.closest("[data-advance]");
  if (!btn) return;
  const id = btn.dataset.advance;
  const status = document.querySelector(`[data-status-for="${id}"]`).value;
  const strategy = document.querySelector(`[data-strategy-for="${id}"]`).value;
  const interviewDetails = document.querySelector(`[data-interview-for="${id}"]`).value;

  try {
    await apiPost(`/api/applications/${id}/status`, { status, strategy, interviewDetails });
    showToast("Stage updated — candidate notified by email.");
    await loadApplications();
  } catch (err) {
    showToast("Couldn't update — " + err.message);
  }
});

// ---------- init ----------

async function refreshAll() {
  await loadJobs();
  await loadApplications();
}

(async function start() {
  try {
    currentUser = await apiGet("/api/auth/me");
  } catch (err) {
    currentUser = { authenticated: false };
  }

  if (isLoggedIn()) {
    showApp();
    try {
      await refreshAll();
    } catch (err) {
      showToast("Couldn't load your data — " + err.message);
    }
  } else {
    showGate();
  }
})();
