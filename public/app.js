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

// Loaded from /api/applications/metrics so recruiter-added assessment
// types appear without any change to this file.
let METRICS = [];

// Bonus justifications the recruiter can choose from.
const BONUS_REASONS = [
  "Exceptional technical assessment",
  "Strong cultural fit",
  "Outstanding behavioural assessment",
  "Prior relevant experience",
  "Specialised skill set",
  "Relocation support",
];

// Stage progression order. REJECTED sits outside it because an
// application can be closed from any point.
const FORWARD_ORDER = ["APPLIED", "SHORTLISTED", "INTERVIEW_SCHEDULED", "HIRED"];

let jobsCache = [];

// FR5: current search filters. Applied in the browser over the loaded
// listings - the full set is small enough that a round trip per keystroke
// would be wasteful.
let jobFilters = { keyword: "", location: "", type: "" };
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

async function apiDelete(path) {
  const res = await fetch(path, { method: "DELETE", credentials: "same-origin" });
  if (!res.ok) throw new Error((await res.json()).error || res.statusText);
  return res.json();
}

/**
 * Reads a File as a Base64 data URL.
 *
 * The PDF travels inside an ordinary form field rather than as a
 * multipart upload, which means the Java backend needs no multipart
 * parser - the existing form parser handles it unchanged.
 */
function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error("Could not read the file"));
    reader.readAsDataURL(file);
  });
}

/** Turns a form into a plain object, converting any chosen file to Base64. */
async function formToFields(formEl) {
  const fields = {};
  for (const [key, value] of new FormData(formEl).entries()) {
    if (value instanceof File) {
      if (value.size === 0) continue;                 // nothing chosen
      if (value.size > 2 * 1024 * 1024) {
        throw new Error("That PDF is larger than 2 MB");
      }
      fields[key] = await readFileAsBase64(value);
    } else {
      fields[key] = value;
    }
  }
  return fields;
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
  document.getElementById("addMetricBtn").hidden = !isRecruiter();

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

  // Only candidates upload a resume.
  const upload = document.getElementById("registerResumeUpload");
  if (upload) upload.hidden = e.target.value !== "CANDIDATE";
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
    currentUser = await apiPost("/api/auth/register", await formToFields(e.target));
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

/** FR5: narrows the loaded listings by keyword, location and employment type. */
function filteredJobs() {
  const kw = jobFilters.keyword.trim().toLowerCase();
  const loc = jobFilters.location.trim().toLowerCase();

  return jobsCache.filter((job) => {
    if (jobFilters.type && job.type !== jobFilters.type) return false;
    if (loc && !(job.location || "").toLowerCase().includes(loc)) return false;
    if (kw) {
      const haystack = [job.title, job.company, job.description]
        .map((v) => (v || "").toLowerCase()).join(" ");
      if (!haystack.includes(kw)) return false;
    }
    return true;
  });
}

function renderJobs() {
  const grid = document.getElementById("jobsGrid");
  const empty = document.getElementById("jobsEmpty");
  document.getElementById("emptyPostRoleBtn").hidden = !isRecruiter();

  const visible = filteredJobs();
  const filterBar = document.getElementById("jobFilters");
  if (filterBar) filterBar.hidden = false;

  const countEl = document.getElementById("jobCount");
  if (countEl) {
    countEl.textContent = visible.length === jobsCache.length
      ? `${jobsCache.length} open role${jobsCache.length === 1 ? "" : "s"}`
      : `${visible.length} of ${jobsCache.length} roles match`;
  }

  if (!visible.length) {
    grid.innerHTML = "";
    empty.hidden = false;
    return;
  }
  empty.hidden = true;

  grid.innerHTML = visible.map((job) => {
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

["keyword", "location", "type"].forEach((field) => {
  const el = document.getElementById("filter-" + field);
  if (!el) return;
  el.addEventListener("input", () => { jobFilters[field] = el.value; renderJobs(); });
  el.addEventListener("change", () => { jobFilters[field] = el.value; renderJobs(); });
});

document.getElementById("clearFilters").addEventListener("click", () => {
  jobFilters = { keyword: "", location: "", type: "" };
  ["keyword", "location", "type"].forEach((f) => {
    const el = document.getElementById("filter-" + f);
    if (el) el.value = "";
  });
  renderJobs();
});

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
    await apiPost("/api/applications", await formToFields(e.target));
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
  // Forward-only: offer the current stage plus later ones, never earlier.
  // The server enforces this too - this just keeps the UI honest.
  const currentIndex = FORWARD_ORDER.indexOf(app.status);
  const isTerminal = app.status === "HIRED" || app.status === "REJECTED";

  const stageOptions = STAGES
    .filter((s) => {
      if (isTerminal) return s.key === app.status;
      if (s.key === "REJECTED") return true;
      return FORWARD_ORDER.indexOf(s.key) >= currentIndex;
    })
    .map((s) => `<option value="${s.key}" ${s.key === app.status ? "selected" : ""}>${s.label}</option>`)
    .join("");

  const metricOptions = ['<option value="">No assessment</option>']
    .concat(METRICS.map((m) => `<option value="${m.key}">${escapeHtml(m.name)}</option>`))
    .join("");

  const reasonOptions = BONUS_REASONS
    .map((r) => `<option value="${escapeHtml(r)}">${escapeHtml(r)}</option>`)
    .join("");

  const controls = isTerminal ? `
      <div class="app-controls">
        <p class="app-locked">This application is closed. No further moves are possible.</p>
      </div>` : `
      <div class="app-controls">
        <label class="ctl">Move to
          <select data-status-for="${app.id}">${stageOptions}</select>
        </label>

        <label class="ctl">Assessment
          <select data-strategy-for="${app.id}">${metricOptions}</select>
        </label>

        <label class="ctl">Score (0-100)
          <input type="number" min="0" max="100" step="1" placeholder="e.g. 78"
                 data-score-for="${app.id}" />
        </label>

        <label class="ctl">Interview details
          <textarea data-interview-for="${app.id}" rows="5"
                    placeholder="Date: 26 August 2026&#10;Time: 11:00 AM&#10;Place: Level 7, TechNova Tower&#10;With: Rita Rahman, Engineering Manager"></textarea>
        </label>

        <details class="offer-box">
          <summary>Offer package <span class="hint">(used when moving to Offer)</span></summary>

          <div class="entitlements">
            <label><input type="checkbox" value="HOUSING"   data-ent-for="${app.id}" /> House rent (40%)</label>
            <label><input type="checkbox" value="TRANSPORT" data-ent-for="${app.id}" /> Transport (5,000)</label>
            <label><input type="checkbox" value="REMOTE"    data-ent-for="${app.id}" /> Remote stipend (3,500)</label>
            <label><input type="checkbox" value="FESTIVAL"  data-ent-for="${app.id}" /> Festival bonus (2 mo/yr)</label>
            <label><input type="checkbox" value="PERFORMANCE" data-ent-for="${app.id}" /> Performance bonus (from score)</label>
          </div>

          <p class="bonus-title">Discretionary bonuses</p>
          <div class="bonus-row">
            <select data-bonus-reason="${app.id}-1"><option value="">— reason —</option>${reasonOptions}</select>
            <input type="number" min="0" step="500" placeholder="Amount BDT" data-bonus-amount="${app.id}-1" />
          </div>
          <div class="bonus-row">
            <select data-bonus-reason="${app.id}-2"><option value="">— reason —</option>${reasonOptions}</select>
            <input type="number" min="0" step="500" placeholder="Amount BDT" data-bonus-amount="${app.id}-2" />
          </div>
        </details>

        <button class="btn btn-primary btn-small" data-advance="${app.id}">Move stage</button>
      </div>`;

  return `
    <div class="app-card" style="--stage-color:${stageOf(app.status).color}">
      <div class="app-candidate">${escapeHtml(app.candidateName)}</div>
      <div class="app-job">${escapeHtml(app.jobTitle)}</div>
      <div class="app-id">#${escapeHtml(app.id)}</div>
      ${app.evaluationSummary ? `<div class="app-eval">${escapeHtml(app.evaluationSummary)}</div>` : ""}
      ${app.requiredDocument ? `<div class="app-doc"><strong>Awaiting:</strong> ${escapeHtml(app.requiredDocument)}</div>` : ""}
      ${controls}
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
          ${app.requiredDocument
            ? `<div class="my-app-doc"><strong>Action needed:</strong> ${escapeHtml(app.requiredDocument)}</div>` : ""}
          ${currentUser?.extra && currentUser.extra.startsWith("/api/resumes/")
            ? `<div class="my-app-resume"><a href="${escapeHtml(currentUser.extra)}" target="_blank" rel="noopener">View my uploaded resume (PDF)</a></div>` : ""}
        </div>
        <div class="my-app-side">
          <span class="status-pill" style="--stage-color:${stage.color}">${stage.label}</span>
          ${app.status === "APPLIED"
            ? `<button class="btn btn-ghost btn-small" data-withdraw="${app.id}">Withdraw</button>` : ""}
        </div>
      </article>`;
  }).join("");
}

document.getElementById("pipelineBoard").addEventListener("click", async (e) => {
  const btn = e.target.closest("[data-advance]");
  if (!btn) return;
  const id = btn.dataset.advance;

  const status = document.querySelector(`[data-status-for="${id}"]`).value;
  const strategy = document.querySelector(`[data-strategy-for="${id}"]`).value;
  const score = document.querySelector(`[data-score-for="${id}"]`).value || "0";
  const interviewDetails = document.querySelector(`[data-interview-for="${id}"]`).value;

  // Build the entitlements string the Decorator chain is assembled from.
  // Fixed-rule entitlements are plain keys; discretionary bonuses carry
  // their reason and amount as "BONUS:<reason>:<amount>".
  const parts = [];
  document.querySelectorAll(`[data-ent-for="${id}"]:checked`)
    .forEach((cb) => parts.push(cb.value));

  [1, 2].forEach((n) => {
    const reason = document.querySelector(`[data-bonus-reason="${id}-${n}"]`)?.value;
    const amount = document.querySelector(`[data-bonus-amount="${id}-${n}"]`)?.value;
    if (reason && amount && Number(amount) > 0) {
      parts.push(`BONUS:${reason}:${amount}`);
    }
  });

  try {
    await apiPost(`/api/applications/${id}/status`, {
      status, strategy, score, interviewDetails,
      entitlements: parts.join(","),
    });
    showToast("Stage updated — candidate notified by email.");
    await loadApplications();
  } catch (err) {
    showToast("Couldn't update — " + err.message);
  }
});

// ---------- add a custom assessment type ----------

document.getElementById("addMetricBtn").addEventListener("click", () => openModal("metricModal"));

document.getElementById("metricForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const created = await apiPost("/api/applications/metrics", Object.fromEntries(new FormData(e.target)));
    e.target.reset();
    closeModal("metricModal");
    await loadMetrics();
    await loadApplications();
    showToast(`"${created.name}" is now available as an assessment type.`);
  } catch (err) {
    showToast("Couldn't add assessment type — " + err.message);
  }
});

// ---------- FR8: withdraw an application ----------

document.getElementById("myApplications").addEventListener("click", async (e) => {
  const btn = e.target.closest("[data-withdraw]");
  if (!btn) return;

  if (!confirm("Withdraw this application? This cannot be undone, though you may apply again later.")) {
    return;
  }
  try {
    await apiDelete(`/api/applications/${btn.dataset.withdraw}`);
    showToast("Application withdrawn.");
    await loadApplications();
  } catch (err) {
    showToast("Couldn't withdraw \u2014 " + err.message);
  }
});

// ---------- init ----------

async function loadMetrics() {
  try {
    METRICS = await apiGet("/api/applications/metrics");
  } catch (err) {
    METRICS = [];
  }
}

async function refreshAll() {
  if (isRecruiter()) await loadMetrics();
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
