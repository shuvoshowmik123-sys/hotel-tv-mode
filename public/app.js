const appRoot = document.getElementById("app");
const overlayRoot = document.getElementById("overlay-root");

const state = {
  user: null,
  panel: null,
  route: "dashboard",
  roomSearch: "",
  selectedRoom: null,
  menuTab: "breakfast",
  editingMenuId: "",
  showNotifications: false,
  showUserForm: false,
  checkoutRoom: null,
  loginError: "",
  formMessage: ""
};

const ROLE_ROUTES = {
  SUPER_ADMIN: ["dashboard", "binding", "rooms", "sessions", "content", "menus", "policies", "users", "audit", "settings"],
  ADMIN: ["dashboard", "rooms", "sessions", "content", "menus", "policies"],
  RECEPTIONIST: ["dashboard", "binding", "rooms", "sessions"]
};

const ROLE_LABELS = {
  SUPER_ADMIN: "Super Admin",
  ADMIN: "Admin",
  RECEPTIONIST: "Receptionist"
};

const ROUTE_LABELS = {
  dashboard: "Dashboard",
  binding: "Device Binding",
  rooms: "Room Management",
  sessions: "Guest Sessions",
  content: "Launcher Content",
  menus: "Menu Management",
  policies: "Apps and Inputs Policy",
  users: "User Management",
  audit: "Audit Log",
  settings: "Settings"
};

const NAV_GROUPS = [
  { label: "Operations", routes: ["dashboard", "binding", "rooms", "sessions"] },
  { label: "Content", routes: ["content", "menus", "policies"] },
  { label: "Admin", routes: ["users", "audit", "settings"] }
];

function icon(name) {
  const paths = {
    dashboard: '<path d="M3 12h7V3H3z"/><path d="M14 21h7v-7h-7z"/><path d="M14 10h7V3h-7z"/><path d="M3 21h7v-4H3z"/>',
    binding: '<path d="M8 12a4 4 0 0 1 0-6l2-2a4 4 0 0 1 6 6l-1 1"/><path d="M16 12a4 4 0 0 1 0 6l-2 2a4 4 0 0 1-6-6l1-1"/>',
    rooms: '<path d="M4 20V8l8-4 8 4v12"/><path d="M2 20h20"/><path d="M9 20v-6h6v6"/>',
    sessions: '<path d="M8 7V3"/><path d="M16 7V3"/><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M3 10h18"/>',
    content: '<path d="M4 18V6a2 2 0 0 1 2-2h12"/><path d="M8 22h10a2 2 0 0 0 2-2V8"/><path d="M10 12h6"/><path d="M10 16h6"/>',
    menus: '<path d="M8 5h13"/><path d="M8 12h13"/><path d="M8 19h13"/><path d="M3 5h.01"/><path d="M3 12h.01"/><path d="M3 19h.01"/>',
    policies: '<path d="M4 7h10"/><path d="M14 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0"/><path d="M10 17h10"/><path d="M6 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0"/>',
    users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    audit: '<path d="M12 8v5l4 2"/><circle cx="12" cy="12" r="9"/>',
    settings: '<path d="M12 15a3 3 0 1 0 0-6"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33"/><path d="M4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33"/><path d="M3 12a2 2 0 0 1 2-2h.09"/><path d="M21 12a2 2 0 0 1-2 2h-.09"/>',
    bell: '<path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5"/><path d="M10 21a2 2 0 0 0 4 0"/>'
  };
  return `<svg viewBox="0 0 24 24" aria-hidden="true">${paths[name] || ""}</svg>`;
}

async function api(url, options = {}) {
  const response = await fetch(url, {
    credentials: "include",
    headers: options.body instanceof FormData ? {} : { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || "Request failed");
  return data;
}

async function loadPanel() {
  state.panel = await api("/api/admin/state");
  if (!state.selectedRoom) state.selectedRoom = Object.keys(state.panel.rooms || {})[0] || null;
  render();
}

function allowedRoutes() {
  return ROLE_ROUTES[state.user?.role] || [];
}

function relativeTime(value) {
  if (!value) return "just now";
  const diff = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
  if (diff < 60) return `${diff}m ago`;
  const hours = Math.floor(diff / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function roomEntries() {
  const rooms = Object.values(state.panel?.rooms || {});
  return rooms.filter((room) => room.roomNumber.includes(state.roomSearch.trim()));
}

function currentRoom() {
  return state.panel?.rooms?.[state.selectedRoom] || roomEntries()[0] || null;
}

function pill(label, klass = "") { return `<span class="pill ${klass}">${label}</span>`; }
function statusPill(value) { return `<span class="status-pill ${value.toLowerCase()}">${value}</span>`; }
function rolePill(value) { return `<span class="role-pill ${value.toLowerCase()}">${ROLE_LABELS[value] || value}</span>`; }
function button(label, attrs = "", primary = false) { return `<button class="pill-button ${primary ? "primary" : ""}" ${attrs}>${label}</button>`; }
function cardHeader(title, eyebrow = "", actions = "") {
  return `<div class="card-head"><div class="head-copy">${eyebrow ? `<div class="eyebrow">${eyebrow}</div>` : ""}<h3>${title}</h3></div>${actions ? `<div class="card-head-actions">${actions}</div>` : ""}</div>`;
}
function formActions(content) {
  return `<div class="button-row end">${content}</div>`;
}

function renderLogin() {
  return `
    <div class="login-shell">
      <section class="card login-card">
        <div class="brand-mark">AG</div>
        <div class="eyebrow login-kicker">Private Operations Portal</div>
        <h1>Asteria Grand Central Admin Panel</h1>
        <p class="muted">Sign in to manage rooms, guest sessions, content, and launcher operations.</p>
        <form id="loginForm">
          <div class="field"><input name="email" type="email" placeholder="Email" /></div>
          <div class="field"><input name="password" type="password" placeholder="Password" /></div>
          ${state.loginError ? `<div class="error-text">${state.loginError}</div>` : ""}
          <button class="pill-button primary" type="submit">Log In</button>
          <div class="muted login-demo">Demo: superadmin@asteriagrand.local / Asteria@2026!</div>
        </form>
      </section>
    </div>`;
}

function renderPendingBindings() {
  const items = Object.values(state.panel.pendingActivations || {});
  if (!items.length) return `<div class="empty-state">No pending activation codes right now.</div>`;
  return `<div class="list">${items.map(item => `<div class="list-row list-row-split"><div class="list-label"><div class="mono metric-code value-gold">${item.activationCode}</div><div class="mono muted">${item.macAddress || item.deviceId}</div></div><div class="list-row-actions"><span class="muted">${relativeTime(item.createdAt)}</span>${button("Bind", `data-action="prefill-bind" data-code="${item.activationCode}"`, true)}</div></div>`).join("")}</div>`;
}

function renderGuestEditor(room, quick = false) {
  const r = room || { roomNumber: "", guestName: "", welcomeNote: "", language: "English" };
  return `<form id="${quick ? "quickGuestForm" : "roomDetailForm"}" class="form-shell">
    <div class="form-stack">
    <div class="field"><input name="roomNumber" value="${r.roomNumber || ""}" placeholder="Room number" ${quick ? "" : "readonly"} /></div>
    <div class="field"><input name="guestName" value="${r.guestName || ""}" placeholder="Guest name" /></div>
    <div class="field"><textarea name="welcomeNote" rows="3" placeholder="Welcome note">${r.welcomeNote || ""}</textarea></div>
    <div class="field"><select name="language"><option ${r.language === "English" ? "selected" : ""}>English</option><option ${r.language === "Bangla" ? "selected" : ""}>Bangla</option></select></div>
    </div>
    ${formActions(`${!quick ? button("Checkout", 'type="button" data-action="checkout-room"') : ""}${button(quick ? "Check In" : "Save", "", true)}`)}
  </form>`;
}

function renderAuditList(entries) {
  return `<div class="list">${entries.map(entry => `<div class="list-row list-row-split"><div class="card-row"><span class="role-pill ${String(entry.actorRole || entry.label || "SYSTEM").toLowerCase()}">${entry.actorName || entry.label || "System"}</span><div>${entry.action || entry.message}</div></div><div class="list-tail"><div class="mono muted">${relativeTime(entry.createdAt)}</div></div></div>`).join("")}</div>`;
}

function renderDashboard() {
  const m = state.panel.metrics;
  const stats = [
    ["TVs Online", m.onlineTvs, "Current property sync"],
    ["Rooms Occupied", m.occupiedRooms, "Live guest sessions"],
    ["Pending Bindings", m.pendingBindings, "Waiting activation codes"],
    ["Unbound Devices", m.unboundDevices, "Needs front desk action"]
  ];
  const statRow = stats.map(([label, value, sub]) => `<div class="card stat-tile span-3"><div class="eyebrow">${label}</div><div class="stat-value mono">${value}</div><div class="stat-subcopy">${sub}</div></div>`).join("");
  if (state.user.role === "RECEPTIONIST") {
    return `<div class="bento-grid stat-row">${statRow}
      <section class="card span-6">${cardHeader("Pending Device Activation", "Front Desk Queue")}${renderPendingBindings()}</section>
      <section class="card span-6">${cardHeader("Quick Guest Session", "Reception Check-In")}${renderGuestEditor(currentRoom(), true)}</section>
      <section class="card span-12"><div class="pill-group">${pill(`Occupied ${m.occupiedRooms}`, "status-pill occupied")}${pill(`Vacant ${m.vacantRooms}`, "status-pill vacant")}${pill(`Offline ${m.offlineRooms}`, "status-pill warning")}</div></section>
    </div>`;
  }
  return `<div class="bento-grid stat-row">${statRow}
    <section class="card span-8">${cardHeader("Live Activity Feed", "Recent Hotel Operations")}${renderAuditList(state.user.role === "SUPER_ADMIN" ? state.panel.auditLogs.slice(0, 8) : state.panel.notifications)}</section>
    <section class="span-4 stack-column">
      <div class="card">${cardHeader("Receptionist Workflow", "Current Process")}<div class="list">${state.panel.workflowSteps.map(step => `<div class="list-row list-row-split"><div>${step.label}</div><div class="list-tail"><span class="status-pill ${step.status === "done" ? "occupied" : "vacant"}">${step.status}</span></div></div>`).join("")}</div></div>
      <div class="card">${cardHeader("System Health", "Operations Signal")}<div class="list"><div class="list-row list-row-split"><div>API Sync Status</div><div class="list-tail muted">${state.panel.systemHealth.apiStatus}</div></div><div class="list-row list-row-split"><div>Last Push</div><div class="list-tail mono muted">${relativeTime(state.panel.systemHealth.lastPushTime)}</div></div><div class="list-row list-row-split"><div>Launcher Version</div><div class="list-tail mono muted">${state.panel.systemHealth.launcherVersion}</div></div></div></div>
    </section></div>`;
}

function renderRoomMap() {
  return roomEntries().map(room => `<button class="room-tile ${state.selectedRoom === room.roomNumber ? "selected" : ""}" data-action="select-room" data-room="${room.roomNumber}"><div class="mono">${room.roomNumber}</div><div class="muted">${room.status}</div></button>`).join("");
}

function renderRooms() {
  const room = currentRoom();
  return `<div class="bento-grid"><section class="card span-12"><div class="pill-group">${pill("Occupied", "status-pill occupied")}${pill("Vacant", "status-pill vacant")}${pill("Unbound", "status-pill warning")}</div></section><section class="card span-6">${cardHeader("Floor Map", "Room Status Overview")}<div class="room-map">${renderRoomMap()}</div></section><section class="card span-6">${cardHeader("Room Detail", "Selected Room Profile")}${room ? `<div class="detail-hero"><div class="mono metric-code value-gold">${room.roomNumber}</div>${statusPill(room.status)}</div><div class="detail-grid"><div class="detail-item"><div class="eyebrow">Guest</div><div class="muted">${room.guestName || "Vacant"}</div></div><div class="detail-item"><div class="eyebrow">Device ID</div><div class="mono muted">${room.deviceId || "Not bound"}</div></div><div class="detail-item"><div class="eyebrow">Last Sync</div><div class="mono muted">${relativeTime(room.lastSyncAt)}</div></div><div class="detail-item"><div class="eyebrow">Override</div><div>${button(room.overrideEnabled ? "Disable Override" : "Enable Override", 'type="button" data-action="toggle-override"', room.overrideEnabled)}</div></div></div>${renderGuestEditor(room)}` : `<div class="empty-state">No room selected.</div>`}</section></div>`;
}

function renderSessions() {
  const occupied = Object.values(state.panel.rooms).filter(room => room.guestName);
  return `<div class="bento-grid"><section class="card span-6">${cardHeader("Active Sessions", "Current Guests")}<div class="table-shell"><div class="table"><div class="table-head grid-sessions"><div>Room</div><div>Guest</div><div>Status</div><div>Check In</div><div></div></div>${occupied.map(room => `<div class="table-row grid-sessions"><div class="mono">${room.roomNumber}</div><div>${room.guestName}</div><div>${statusPill(room.status)}</div><div class="mono muted">${relativeTime(room.checkInAt)}</div><div>${button("Edit", `data-action="select-room" data-room="${room.roomNumber}"`)}</div></div>`).join("") || '<div class="empty-state">No active sessions.</div>'}</div></div></section><section class="card span-6">${cardHeader("Session Editor", "Guest Details")}${renderGuestEditor(currentRoom())}</section></div>`;
}

function renderContent() {
  const v = state.panel.visibility.destinations;
  return `<div class="bento-grid"><section class="card span-4">${cardHeader("Hotel Logo", "Brand Asset")}<form id="startupUploadForm" class="form-shell"><div class="upload-zone"><input name="file" type="file" accept="image/*" /></div>${formActions(button("Upload Logo", "", true))}</form></section><section class="card span-4">${cardHeader("Startup Animation", "Arrival Branding")}<form id="startupAnimationForm" class="form-shell"><div class="upload-zone"><input name="file" type="file" accept="image/*" /></div>${formActions(button("Upload Startup Asset", "", true))}</form></section><section class="card span-4">${cardHeader("Background Slideshow", "Ambient Media")}<form id="backgroundUploadForm" class="form-shell"><div class="field"><select name="bucket"><option value="home">Home</option><option value="roomService">Room Service</option><option value="foodMenu">Food Menu</option><option value="inputs">Inputs</option></select></div><div class="upload-zone"><input name="file" type="file" accept="image/*" /></div>${formActions(button("Upload Background", "", true))}</form></section><section class="card span-6">${cardHeader("Tile Visibility", "Launcher Destinations")}${["home","roomService","foodMenu","inputs"].map(key => `<div class="toggle-row"><div><div>${ROUTE_LABELS[key] || key}</div><div class="muted">${key} destination visibility</div></div><div class="toggle ${v[key] ? "on" : ""}" data-action="toggle-destination" data-key="${key}"></div></div>`).join("")}</section><section class="card span-6">${cardHeader("Popup and Branding", "Guest-Facing Copy")}<form id="contentForm" class="form-shell"><div class="form-stack"><div class="field"><input name="hotelName" value="${state.panel.hotel.hotelName}" placeholder="Hotel name" /></div><div class="field"><input name="helpTitle" value="${state.panel.popup.helpTitle || ""}" placeholder="Help title" /></div><div class="field"><input name="callNumber" value="${state.panel.popup.callNumber}" placeholder="Call number" /></div><div class="field"><input name="ratingText" value="${state.panel.popup.ratingText}" placeholder="Rating prompt" /></div></div>${formActions(button("Push Changes to All TVs", "", true))}</form></section></div>`;
}

function renderMenuEditor(item) {
  const current = item || { title: "", subtitle: "", description: "", price: "", available: true };
  return `<form id="menuEditorForm" class="form-shell"><input type="hidden" name="id" value="${item?.id || ""}" /><div class="form-stack"><div class="split"><div class="field"><input name="title" value="${current.title}" placeholder="Item name" /></div><div class="field"><input name="subtitle" value="${current.subtitle || ""}" placeholder="Subtitle" /></div></div><div class="field"><textarea name="description" rows="3" placeholder="Description">${current.description || ""}</textarea></div><div class="split"><div class="field"><input name="price" value="${current.price || ""}" placeholder="Price" /></div><div class="field"><select name="available"><option value="true" ${current.available ? "selected" : ""}>Available</option><option value="false" ${!current.available ? "selected" : ""}>Unavailable</option></select></div></div></div>${formActions(button("Save Item", "", true))}</form>`;
}

function renderMenus() {
  const items = state.panel.meals[state.menuTab] || [];
  return `<div class="bento-grid"><section class="card span-12">${cardHeader("Menu Catalog", "Breakfast, Lunch, Dinner, and Beverages", `<div class="pill-group">${["breakfast","lunch","dinner","beverages"].map(tab => button(tab[0].toUpperCase() + tab.slice(1), `type="button" data-action="menu-tab" data-tab="${tab}"`, state.menuTab === tab)).join("")}</div>${button("Add Item", 'type="button" data-action="new-menu-item"', true)}`)}<div class="table-shell"><div class="table"><div class="table-head grid-menu"><div>Item</div><div>Description</div><div>Price</div><div>Available</div><div></div></div>${items.map(item => `<div class="table-row grid-menu"><div><div>${item.title}</div><div class="muted">${item.subtitle || ""}</div></div><div class="muted">${item.description || ""}</div><div class="mono value-gold">${item.price || ""}</div><div>${item.available ? statusPill("occupied") : statusPill("vacant")}</div><div>${button("Edit", `type="button" data-action="edit-menu-item" data-id="${item.id}"`)}</div></div>`).join("") || '<div class="empty-state">No menu items in this category.</div>'}</div></div></section><section class="card span-12">${cardHeader(state.editingMenuId ? "Edit Menu Item" : "Add Menu Item", "Menu Form")}${renderMenuEditor(items.find(item => item.id === state.editingMenuId))}</section></div>`;
}

function renderPolicies() {
  const apps = state.panel.availableApps.map(app => ({ key: app.packageName, name: app.name, description: app.description, on: state.panel.visibility.visibleAppPackages.includes(app.packageName) }));
  const inputs = state.panel.availableInputs.map(input => ({ key: input.title, name: input.title, description: input.description, on: state.panel.visibility.visibleSourceTitles.includes(input.title) }));
  return `<div class="bento-grid"><section class="card span-6">${cardHeader("App Visibility", "Installed App Policy")}${apps.map(item => `<div class="toggle-row"><div><div>${item.name}</div><div class="muted">${item.description}</div></div><div class="toggle ${item.on ? "on" : ""}" data-action="toggle-app" data-key="${item.key}"></div></div>`).join("")}</section><section class="card span-6">${cardHeader("Input Source Visibility", "TV Source Policy")}${inputs.map(item => `<div class="toggle-row"><div><div>${item.name}</div><div class="muted">${item.description}</div></div><div class="toggle ${item.on ? "on" : ""}" data-action="toggle-input" data-key="${item.key}"></div></div>`).join("")}${cardHeader("Tile Priority", "Display Order")}<div class="list">${state.panel.visibility.visibleSourceTitles.map(title => `<div class="list-row list-row-split"><span class="mono">${title}</span><span class="list-tail muted">drag later</span></div>`).join("")}</div></section></div>`;
}

function renderUserForm(user) {
  return `<form id="userForm" class="form-shell"><input type="hidden" name="id" value="${user?.id || ""}" /><div class="form-stack"><div class="split"><div class="field"><input name="name" value="${user?.name || ""}" placeholder="Name" /></div><div class="field"><input name="email" value="${user?.email || ""}" placeholder="Email" /></div></div><div class="split"><div class="field"><select name="role">${Object.keys(ROLE_LABELS).map(role => `<option value="${role}" ${user?.role === role ? "selected" : ""}>${ROLE_LABELS[role]}</option>`).join("")}</select></div><div class="field"><select name="status"><option value="ACTIVE">Active</option><option value="DISABLED" ${user?.status === "DISABLED" ? "selected" : ""}>Disabled</option></select></div></div><div class="field"><input name="password" placeholder="${user ? "New password (optional)" : "Password"}" /></div></div>${formActions(button(user ? "Update Account" : "Create Account", "", true))}</form>`;
}

function renderUsers() {
  return `<div class="bento-grid"><section class="card span-12">${cardHeader("User Management", "Role Access and Accounts", button(state.showUserForm ? "Close" : "New Account", 'type="button" data-action="toggle-user-form"', true))}${state.showUserForm ? renderUserForm() : ""}<div class="table-shell"><div class="table"><div class="table-head grid-users"><div>Name</div><div>Email</div><div>Role</div><div>Property</div><div>Status</div><div></div></div>${state.panel.users.map(user => `<div class="table-row grid-users"><div>${user.name}</div><div>${user.email}</div><div>${rolePill(user.role)}</div><div>${state.panel.property.name}</div><div>${statusPill(user.status.toLowerCase() === "active" ? "active" : "disabled")}</div><div>${button("Edit", `type="button" data-action="edit-user" data-user="${user.id}"`)}</div></div>`).join("")}</div></div></section></div>`;
}

function renderAudit() {
  return `<div class="bento-grid"><section class="card span-12">${cardHeader("Audit Log", "Role-Based Activity", button("Export CSV", 'type="button" data-action="export-audit"'))}${renderAuditList(state.panel.auditLogs)}</section></div>`;
}

function renderSettings() {
  const s = state.panel.settings;
  return `<div class="bento-grid"><section class="card span-6">${cardHeader("Property Management", "Hotel Profile")}<form id="settingsForm" class="form-shell"><div class="form-stack"><div class="field"><input name="propertyName" value="${s.property.name}" /></div><div class="field"><input name="address" value="${s.property.address}" /></div><div class="field"><input name="timezone" value="${s.property.timezone}" /></div></div>${formActions(button("Save Property", "", true))}</form></section><section class="card span-6">${cardHeader("API and Integration", "Connectivity")}<div class="list"><div class="list-row list-row-split"><div>API key</div><div class="list-tail mono muted">${s.integration.apiKeyPreview}</div></div><div class="list-row list-row-split"><div>Webhook URL</div><div class="list-tail mono muted">${s.integration.webhookUrl}</div></div><div class="list-row list-row-split"><div>Environment</div><div class="list-tail">${pill(s.subscription.environment)}</div></div></div></section><section class="card span-12">${cardHeader("Subscription and Deployment", "Plan Status")}<div class="list"><div class="list-row list-row-split"><div>Current plan</div><div class="list-tail">${s.subscription.plan}</div></div><div class="list-row list-row-split"><div>Renewal date</div><div class="list-tail mono muted">${s.subscription.renewalDate}</div></div></div></section></div>`;
}

function renderNotifications() {
  if (!state.showNotifications) return "";
  return `<aside class="notification-panel">${cardHeader("Notifications", "Recent System Events", button("Close", 'type="button" data-action="toggle-notifications"'))}${renderAuditList(state.panel.notifications)}</aside>`;
}

function renderMainContent() {
  if (state.route === "binding") return `<div class="bento-grid"><section class="card span-6">${cardHeader("Pending Bindings", "Activation Queue")}${renderPendingBindings()}</section><section class="card span-6">${cardHeader("Binding Form", "Assign Room and Guest")}<form id="bindForm" class="form-shell"><div class="form-stack"><div class="field"><input name="activationCode" placeholder="Activation code" /></div><div class="field"><input name="roomNumber" value="${currentRoom()?.roomNumber || ""}" placeholder="Room number" /></div><div class="field"><input name="guestName" value="${currentRoom()?.guestName || ""}" placeholder="Guest name" /></div><div class="field"><textarea name="welcomeNote" rows="3" placeholder="Welcome note"></textarea></div></div>${formActions(button("Confirm Binding", "", true))}</form></section></div>`;
  if (state.route === "rooms") return renderRooms();
  if (state.route === "sessions") return renderSessions();
  if (state.route === "content") return renderContent();
  if (state.route === "menus") return renderMenus();
  if (state.route === "policies") return renderPolicies();
  if (state.route === "users") return renderUsers();
  if (state.route === "audit") return renderAudit();
  if (state.route === "settings") return renderSettings();
  return renderDashboard();
}

function renderApp() {
  const routes = allowedRoutes();
  if (!routes.includes(state.route)) state.route = routes[0];
  appRoot.innerHTML = `<div class="app-shell"><aside class="sidebar"><div class="brand-card"><div class="brand-mark">AG</div><h3>Asteria Grand</h3><div class="muted">Central Admin Panel</div></div>${NAV_GROUPS.map(group => { const items = group.routes.filter(route => routes.includes(route)); if (!items.length) return ""; return `<div><div class="nav-group-label">${group.label}</div><div class="nav-list">${items.map(route => `<button class="nav-item ${state.route === route ? "active" : ""}" data-action="route" data-route="${route}">${icon(route)}<span>${ROUTE_LABELS[route]}</span></button>`).join("")}</div></div>`; }).join("")}<div class="sidebar-footer"><div class="avatar">${state.user.name.slice(0, 1)}</div><div class="list-label"><div>${state.user.name}</div>${rolePill(state.user.role)}</div></div></aside><main class="content-shell"><div class="topbar"><div class="topbar-title"><div class="eyebrow">Current page</div><div class="page-title">${ROUTE_LABELS[state.route]}</div></div><div class="field"><input class="search-input" id="roomSearchInput" value="${state.roomSearch}" placeholder="Search room number" /></div><div class="topbar-actions"><div class="live-sync"><span class="sync-dot"></span><span>Last synced ${relativeTime(state.panel.systemHealth.lastPushTime)}</span></div><div class="toolbar-actions"><button class="icon-btn" data-action="toggle-notifications">${icon("bell")}</button>${button("Primary Action", 'type="button" data-action="primary-action"', true)}${button("Logout", 'type="button" data-action="logout"')}</div></div></div>${state.formMessage ? `<div class="success-text notice-banner">${state.formMessage}</div>` : ""}${renderMainContent()}</main></div>`;
  overlayRoot.innerHTML = `${renderNotifications()}${state.checkoutRoom ? `<div class="overlay"><div class="modal-card">${cardHeader("Confirm Checkout", "Guest Session")}<p class="muted">Clear the guest session for room ${state.checkoutRoom}?</p>${formActions(`${button("Cancel", 'type="button" data-action="cancel-checkout"')}${button("Confirm Checkout", 'type="button" data-action="confirm-checkout"', true)}`)}</div></div>` : ""}`;
  bindEvents();
}

function render() {
  if (!state.user) { appRoot.innerHTML = renderLogin(); overlayRoot.innerHTML = ""; bindEvents(); return; }
  renderApp();
}

async function refreshAfter(message) {
  state.formMessage = message || "";
  await loadPanel();
}

function bindEvents() {
  document.getElementById("loginForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const payload = Object.fromEntries(new FormData(event.currentTarget));
      const data = await api("/api/auth/login", { method: "POST", body: JSON.stringify(payload) });
      state.user = data.user;
      state.loginError = "";
      await loadPanel();
    } catch (error) {
      state.loginError = error.message;
      render();
    }
  });

  document.getElementById("bindForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    await api("/api/admin/bind", { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget))) });
    await refreshAfter("Binding confirmed.");
  });
  document.getElementById("roomDetailForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    await api("/api/admin/rooms", { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget))) });
    await refreshAfter("Room updated.");
  });
  document.getElementById("quickGuestForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    await api("/api/admin/rooms", { method: "POST", body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget))) });
    await refreshAfter("Guest session saved.");
  });
  document.getElementById("contentForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = Object.fromEntries(new FormData(event.currentTarget));
    await api("/api/admin/config", { method: "POST", body: JSON.stringify({ hotel: { hotelName: form.hotelName }, popup: { helpTitle: form.helpTitle, callNumber: form.callNumber, ratingText: form.ratingText } }) });
    await refreshAfter("Launcher content pushed.");
  });
  document.getElementById("menuEditorForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = Object.fromEntries(new FormData(event.currentTarget));
    const meals = structuredClone(state.panel.meals);
    const current = meals[state.menuTab] || [];
    const item = { id: form.id || `menu-${Date.now()}`, title: form.title, subtitle: form.subtitle, description: form.description, price: form.price, available: form.available === "true", badge: "NEW", accentColor: "#C9A84C" };
    meals[state.menuTab] = form.id ? current.map(entry => entry.id === form.id ? item : entry) : [...current, item];
    await api("/api/admin/config", { method: "POST", body: JSON.stringify({ meals }) });
    state.editingMenuId = "";
    await refreshAfter("Menu saved.");
  });
  document.getElementById("userForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = Object.fromEntries(new FormData(event.currentTarget));
    if (form.id) await api(`/api/admin/users/${form.id}`, { method: "PATCH", body: JSON.stringify(form) });
    else await api("/api/admin/users", { method: "POST", body: JSON.stringify(form) });
    state.showUserForm = false;
    await refreshAfter("User saved.");
  });
  document.getElementById("settingsForm")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = Object.fromEntries(new FormData(event.currentTarget));
    await api("/api/admin/config", { method: "POST", body: JSON.stringify({ property: { name: form.propertyName, address: form.address, timezone: form.timezone } }) });
    await refreshAfter("Settings updated.");
  });

  ["startupUploadForm", "startupAnimationForm", "backgroundUploadForm"].forEach((id) => {
    document.getElementById(id)?.addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = new FormData(event.currentTarget);
      const isBackground = id === "backgroundUploadForm";
      const bucket = isBackground ? form.get("bucket") : "";
      await api(`/api/admin/upload?kind=${isBackground ? "background" : "startup"}${bucket ? `&bucket=${encodeURIComponent(bucket)}` : ""}`, { method: "POST", body: form });
      await refreshAfter("Asset uploaded.");
    });
  });

  document.querySelectorAll("[data-action]").forEach((node) => {
    node.addEventListener("click", async (event) => {
      const el = event.currentTarget;
      const action = el.dataset.action;
      if (action === "route") { state.route = el.dataset.route; render(); }
      if (action === "toggle-notifications") { state.showNotifications = !state.showNotifications; render(); }
      if (action === "logout") { await api("/api/auth/logout", { method: "POST" }); state.user = null; state.panel = null; state.formMessage = ""; render(); }
      if (action === "prefill-bind") { state.route = "binding"; render(); document.querySelector('#bindForm [name="activationCode"]').value = el.dataset.code; }
      if (action === "select-room") { state.selectedRoom = el.dataset.room; render(); }
      if (action === "checkout-room") { state.checkoutRoom = currentRoom()?.roomNumber; render(); }
      if (action === "cancel-checkout") { state.checkoutRoom = null; render(); }
      if (action === "confirm-checkout") { await api(`/api/admin/rooms/${state.checkoutRoom}/checkout`, { method: "POST" }); state.checkoutRoom = null; await refreshAfter("Checkout completed."); }
      if (action === "toggle-override") { const room = currentRoom(); await api(`/api/admin/rooms/${room.roomNumber}/override`, { method: "POST", body: JSON.stringify({ enabled: !room.overrideEnabled, customContentLabel: room.customContentLabel || "" }) }); await refreshAfter("Room override updated."); }
      if (action === "toggle-destination") { const key = el.dataset.key; const visibility = structuredClone(state.panel.visibility); visibility.destinations[key] = !visibility.destinations[key]; await api("/api/admin/config", { method: "POST", body: JSON.stringify({ visibility }) }); await refreshAfter("Visibility updated."); }
      if (action === "toggle-app") { const key = el.dataset.key; const visibility = structuredClone(state.panel.visibility); visibility.visibleAppPackages = visibility.visibleAppPackages.includes(key) ? visibility.visibleAppPackages.filter(v => v !== key) : [...visibility.visibleAppPackages, key]; await api("/api/admin/config", { method: "POST", body: JSON.stringify({ visibility }) }); await refreshAfter("App policy updated."); }
      if (action === "toggle-input") { const key = el.dataset.key; const visibility = structuredClone(state.panel.visibility); visibility.visibleSourceTitles = visibility.visibleSourceTitles.includes(key) ? visibility.visibleSourceTitles.filter(v => v !== key) : [...visibility.visibleSourceTitles, key]; await api("/api/admin/config", { method: "POST", body: JSON.stringify({ visibility }) }); await refreshAfter("Input policy updated."); }
      if (action === "menu-tab") { state.menuTab = el.dataset.tab; state.editingMenuId = ""; render(); }
      if (action === "new-menu-item") { state.editingMenuId = ""; render(); }
      if (action === "edit-menu-item") { state.editingMenuId = el.dataset.id; render(); }
      if (action === "toggle-user-form") { state.showUserForm = !state.showUserForm; render(); }
      if (action === "edit-user") { state.showUserForm = true; render(); const user = state.panel.users.find(item => item.id === el.dataset.user); document.querySelector("#userForm [name='id']").value = user.id; document.querySelector("#userForm [name='name']").value = user.name; document.querySelector("#userForm [name='email']").value = user.email; document.querySelector("#userForm [name='role']").value = user.role; document.querySelector("#userForm [name='status']").value = user.status; }
      if (action === "export-audit") { const csv = ["Actor,Role,Action,Time", ...state.panel.auditLogs.map(row => `"${row.actorName}","${row.actorRole}","${String(row.action).replaceAll('"', '""')}","${row.createdAt}"`)].join("\n"); const blob = new Blob([csv], { type: "text/csv" }); const link = document.createElement("a"); link.href = URL.createObjectURL(blob); link.download = "audit-log.csv"; link.click(); }
      if (action === "primary-action") { state.route = state.user.role === "RECEPTIONIST" ? "binding" : "content"; render(); }
    });
  });

  document.getElementById("roomSearchInput")?.addEventListener("input", (event) => {
    state.roomSearch = event.currentTarget.value;
    render();
  });
}

async function bootstrap() {
  try {
    const auth = await api("/api/auth/me");
    state.user = auth.user;
    await loadPanel();
  } catch {
    render();
  }
}

setInterval(() => {
  if (state.user && state.panel) render();
}, 60000);

bootstrap();
