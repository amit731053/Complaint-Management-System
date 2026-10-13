/* Shared helpers for every page: CSRF header, session cookie, 401 handling, HTML escaping. */
(function () {
    var origFetch = window.fetch.bind(window);

    function getCookie(name) {
        var m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
        return m ? decodeURIComponent(m[1]) : null;
    }

    function onLoginPage() {
        var p = location.pathname;
        return p === '/' || p.endsWith('/index.html');
    }

    // Every request: send the session cookie, and add the CSRF token on writes.
    window.fetch = function (input, init) {
        init = Object.assign({}, init || {});
        init.credentials = 'same-origin';
        var method = (init.method || 'GET').toUpperCase();
        if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
            var token = getCookie('XSRF-TOKEN');
            if (token) {
                init.headers = Object.assign({}, init.headers || {}, { 'X-XSRF-TOKEN': token });
            }
        }
        return origFetch(input, init).then(function (res) {
            // Session expired / not logged in -> back to login (but not for the login call itself).
            if (res.status === 401 && !onLoginPage() && String(input).indexOf('/api/auth/') === -1) {
                try { sessionStorage.removeItem('user'); sessionStorage.setItem('sessionNotice', 'expired'); } catch (e) { }
                window.location.href = 'index.html';
            }
            return res;
        });
    };

    // Escape any text before putting it inside innerHTML templates (prevents XSS).
    window.escapeHtml = function (value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    };

    // Ends the server session. Always call this from logout().
    window.serverLogout = function () {
        return fetch('/api/auth/logout', { method: 'POST' }).catch(function () { });
    };
})();

/* Keeps the page in sync with the real (server-side) login.
   One browser = one session cookie. If another account logs in (or out) in a different tab,
   this tab notices and returns to the login page instead of showing the wrong account. */
(function () {
    var p = location.pathname;
    if (p === '/' || p.endsWith('/index.html')) return;

    var checking = false;

    function endSession(reason) {
        try {
            sessionStorage.removeItem('user');
            sessionStorage.setItem('sessionNotice', reason);
        } catch (e) { }
        window.location.replace('index.html');
    }

    function verify() {
        if (checking) return;
        checking = true;
        var stored = null;
        try { stored = JSON.parse(sessionStorage.getItem('user') || 'null'); } catch (e) { }

        fetch('/api/auth/me').then(function (res) {
            if (res.status === 401) { endSession('expired'); return null; }
            return res.ok ? res.json() : null;
        }).then(function (me) {
            if (!me) return;
            if (!stored || stored.email !== me.email || stored.role !== me.role) {
                endSession('changed');
            }
        }).catch(function () { }).then(function () { checking = false; });
    }

    document.addEventListener('DOMContentLoaded', verify);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) verify(); });
    window.addEventListener('pageshow', function (e) { if (e.persisted) verify(); });
})();
