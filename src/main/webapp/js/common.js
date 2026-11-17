/**
 * common.js - shared helpers used by every page.
 *  - NB.request(): fetch wrapper that sends form data, attaches the CSRF token and
 *                  handles expired sessions.
 *  - NB.toast() / NB.modal(): success & error notifications and popups.
 *  - NB.initProtectedPage(): draws the navbar + sidebar and loads the session info.
 *  - Field-error helpers and Indian Rupee formatting.
 * All user-supplied text is inserted with textContent (never innerHTML) to prevent XSS.
 */
const NB = (() => {
    'use strict';

    let csrfToken = null;

    const inrFormatter = new Intl.NumberFormat('en-IN', {
        style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 2
    });

    /** Formats a number as Indian Rupees, e.g. 150000 -> "₹1,50,000.00". */
    function formatINR(value) {
        const n = Number(value);
        return inrFormatter.format(Number.isFinite(n) ? n : 0);
    }

    /** Formats "yyyy-MM-dd HH:mm:ss" from the server as "17 Nov 2026, 04:18 PM". */
    function formatDateTime(value) {
        if (!value) return '-';
        const d = new Date(String(value).replace(' ', 'T'));
        if (isNaN(d)) return value;
        return d.toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true
        });
    }

    /**
     * Calls a JSON API. `data` (object) is sent as application/x-www-form-urlencoded.
     * Always resolves to { ok, status, body } - network errors become a friendly message.
     */
    async function request(url, { method = 'GET', data = null } = {}) {
        const opts = { method, headers: { Accept: 'application/json' }, credentials: 'same-origin' };
        if (data) {
            opts.body = new URLSearchParams(data);
            opts.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
        }
        if (method !== 'GET' && csrfToken) {
            opts.headers['X-CSRF-Token'] = csrfToken;
        }

        let res;
        try {
            res = await fetch(url, opts);
        } catch (err) {
            return { ok: false, status: 0, body: { success: false, message: 'Unable to reach the server. Please check that Tomcat is running.' } };
        }

        let body;
        try {
            body = await res.json();
        } catch (err) {
            body = { success: false, message: 'Unexpected response from the server (HTTP ' + res.status + ').' };
        }

        // Session expired on a protected API -> back to login
        if (res.status === 401 && !url.startsWith('api/login')) {
            window.location.href = 'login?expired=1';
        }
        return { ok: res.ok, status: res.status, body };
    }

    /* ------------------------------------------------------------------ */
    /* Notifications                                                       */
    /* ------------------------------------------------------------------ */

    function toastContainer() {
        let c = document.getElementById('toast-container');
        if (!c) {
            c = document.createElement('div');
            c.id = 'toast-container';
            c.setAttribute('aria-live', 'polite');
            document.body.appendChild(c);
        }
        return c;
    }

    /** Small auto-dismissing notification. type = success | error | info */
    function toast(message, type = 'success', timeout = 4000) {
        const t = document.createElement('div');
        t.className = 'toast toast-' + type;
        t.textContent = message;
        toastContainer().appendChild(t);
        requestAnimationFrame(() => t.classList.add('show'));
        setTimeout(() => {
            t.classList.remove('show');
            setTimeout(() => t.remove(), 300);
        }, timeout);
    }

    /**
     * Centered popup.
     * options: { title, message, type: success|error|info, details: [[label, value], ...],
     *            highlight: {label, value}, buttonText, onClose }
     */
    function modal({ title, message, type = 'success', details = [], highlight = null, buttonText = 'OK', onClose = null }) {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');

        const box = document.createElement('div');
        box.className = 'modal modal-' + type;

        const icon = document.createElement('div');
        icon.className = 'modal-icon';
        icon.textContent = type === 'success' ? '✓' : (type === 'error' ? '!' : 'i');
        box.appendChild(icon);

        const h = document.createElement('h2');
        h.textContent = title;
        box.appendChild(h);

        if (message) {
            message.split('\n').forEach(line => {
                const p = document.createElement('p');
                p.textContent = line;
                box.appendChild(p);
            });
        }

        if (highlight) {
            const hl = document.createElement('div');
            hl.className = 'modal-highlight';
            const l = document.createElement('span');
            l.textContent = highlight.label;
            const v = document.createElement('strong');
            v.textContent = highlight.value;
            hl.append(l, v);
            box.appendChild(hl);
        }

        if (details.length) {
            const dl = document.createElement('dl');
            dl.className = 'modal-details';
            details.forEach(([label, value]) => {
                const dt = document.createElement('dt');
                dt.textContent = label;
                const dd = document.createElement('dd');
                dd.textContent = value;
                dl.append(dt, dd);
            });
            box.appendChild(dl);
        }

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-primary btn-block';
        btn.textContent = buttonText;
        box.appendChild(btn);

        overlay.appendChild(box);
        document.body.appendChild(overlay);
        requestAnimationFrame(() => overlay.classList.add('show'));
        btn.focus();

        const close = () => {
            overlay.classList.remove('show');
            document.removeEventListener('keydown', onKey);
            setTimeout(() => overlay.remove(), 200);
            if (typeof onClose === 'function') onClose();
        };
        const onKey = (e) => { if (e.key === 'Escape') close(); };
        btn.addEventListener('click', close);
        document.addEventListener('keydown', onKey);
    }

    /* ------------------------------------------------------------------ */
    /* Form helpers                                                        */
    /* ------------------------------------------------------------------ */

    function clearErrors(form) {
        form.querySelectorAll('.field-error').forEach(e => { e.textContent = ''; });
        form.querySelectorAll('.invalid').forEach(e => e.classList.remove('invalid'));
    }

    /** Shows { fieldName: message } errors under the matching inputs. */
    function showErrors(form, errors) {
        if (!errors) return;
        Object.entries(errors).forEach(([field, msg]) => {
            const slot = form.querySelector('[data-error-for="' + field + '"]');
            if (slot) slot.textContent = msg;
            const input = form.querySelector('[name="' + field + '"]');
            if (input) input.classList.add('invalid');
        });
        const first = form.querySelector('.invalid');
        if (first) first.focus();
    }

    /** Disables a submit button and shows a busy label while a request runs. */
    function setBusy(button, busy, busyText = 'Please wait...') {
        if (!button) return;
        if (busy) {
            button.dataset.label = button.textContent;
            button.textContent = busyText;
            button.disabled = true;
        } else {
            button.textContent = button.dataset.label || button.textContent;
            button.disabled = false;
        }
    }

    function formData(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    /** Wires every "Show / Hide" password toggle on the page. */
    function initPasswordToggles() {
        document.querySelectorAll('.toggle-password').forEach(btn => {
            btn.addEventListener('click', () => {
                const input = document.getElementById(btn.dataset.target);
                const show = input.type === 'password';
                input.type = show ? 'text' : 'password';
                btn.textContent = show ? 'Hide' : 'Show';
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /* Layout for logged-in pages                                          */
    /* ------------------------------------------------------------------ */

    const MENU = [
        { id: 'dashboard', href: 'dashboard', label: 'Dashboard', icon: '▦' },
        { id: 'deposit', href: 'deposit', label: 'Deposit Money', icon: '↓' },
        { id: 'withdraw', href: 'withdraw', label: 'Withdraw Money', icon: '↑' },
        { id: 'transactions', href: 'transactions', label: 'Transaction History', icon: '≡' }
    ];

    function el(tag, className, text) {
        const e = document.createElement(tag);
        if (className) e.className = className;
        if (text !== undefined) e.textContent = text;
        return e;
    }

    function renderLayout(active) {
        // Navbar
        const nav = document.getElementById('app-nav');
        const left = el('div', 'nav-left');
        const burger = el('button', 'hamburger', '☰');
        burger.type = 'button';
        burger.setAttribute('aria-label', 'Toggle menu');
        const brand = el('a', 'nav-brand');
        brand.href = 'dashboard';
        brand.append(el('span', 'brand-logo small', 'NB'), el('span', '', 'Net Banking'));
        left.append(burger, brand);

        const right = el('div', 'nav-right');
        const user = el('span', 'nav-user');
        user.id = 'navUser';
        const logoutBtn = el('button', 'btn btn-light btn-sm', 'Logout');
        logoutBtn.type = 'button';
        logoutBtn.addEventListener('click', logout);
        right.append(user, logoutBtn);
        nav.replaceChildren(left, right);

        // Sidebar
        const side = document.getElementById('app-sidebar');
        const ul = el('ul', 'side-menu');
        MENU.forEach(item => {
            const li = el('li');
            const a = el('a', item.id === active ? 'active' : '');
            a.href = item.href;
            a.append(el('span', 'menu-icon', item.icon), el('span', '', item.label));
            li.appendChild(a);
            ul.appendChild(li);
        });
        const li = el('li');
        const out = el('a', 'logout-link');
        out.href = '#';
        out.append(el('span', 'menu-icon', '⏻'), el('span', '', 'Logout'));
        out.addEventListener('click', (e) => { e.preventDefault(); logout(); });
        li.appendChild(out);
        ul.appendChild(li);
        side.replaceChildren(ul);

        // Mobile sidebar toggle
        const backdrop = el('div', 'sidebar-backdrop');
        document.body.appendChild(backdrop);
        const toggle = () => document.body.classList.toggle('sidebar-open');
        burger.addEventListener('click', toggle);
        backdrop.addEventListener('click', toggle);
    }

    /** Renders the layout, then loads /api/me (name, account, CSRF token). */
    async function initProtectedPage(active) {
        renderLayout(active);
        const res = await request('api/me');
        if (!res.ok) {
            if (res.status !== 401) toast(res.body.message || 'Could not load your profile.', 'error');
            return null;
        }
        const me = res.body.data;
        csrfToken = me.csrfToken;
        document.getElementById('navUser').textContent = me.fullName;
        return me;
    }

    async function logout() {
        await request('api/logout', { method: 'POST', data: {} });
        sessionStorage.clear();
        window.location.replace('login?logout=1');
    }

    return {
        formatINR, formatDateTime, request, toast, modal,
        clearErrors, showErrors, setBusy, formData, initPasswordToggles,
        initProtectedPage, logout
    };
})();
