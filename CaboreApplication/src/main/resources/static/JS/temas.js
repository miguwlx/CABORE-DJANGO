/**
 * CABORE — Gestor Global de Temas Visuales (Modo Claro / Modo Oscuro)
 *
 * Reglas:
 *  - Modo claro ('claro') es siempre el predeterminado si no hay preferencia guardada.
 *  - Usuarios autenticados: preferencia individual guardada en MariaDB (/api/tema)
 *    y cacheada en localStorage por ID de usuario (cabore_theme_u_<id>) para evitar
 *    afectar a otras cuentas del mismo navegador.
 *  - Usuarios no autenticados: preferencia guardada en localStorage (cabore_theme_anon).
 *  - El cambio se aplica de forma inmediata sin recargar la página.
 */
(function () {
    const ROOT = document.documentElement;
    const ANON_KEY = 'cabore_theme_anon';

    function normalizar(t) {
        return (t === 'oscuro' || t === 'dark') ? 'oscuro' : 'claro';
    }

    function obtenerUsuarioId() {
        const uid = ROOT.dataset.userId;
        return uid && uid.trim() !== '' && uid !== 'null' ? uid.trim() : null;
    }

    function aplicarTemaDOM(tema) {
        const t = normalizar(tema);
        ROOT.setAttribute('data-theme', t);
        actualizarBotones(t);
        try {
            window.dispatchEvent(new CustomEvent('cabore:theme-changed', { detail: { tema: t } }));
        } catch (_) {}
    }

    function actualizarBotones(tema) {
        const esOscuro = tema === 'oscuro';
        const titulo = esOscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro';
        const textoLabel = esOscuro ? 'Modo claro' : 'Modo oscuro';

        document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
            btn.setAttribute('aria-label', titulo);
            btn.setAttribute('title', titulo);
            btn.setAttribute('aria-pressed', esOscuro ? 'true' : 'false');
            const lbl = btn.querySelector('.theme-toggle-label');
            if (lbl) lbl.textContent = textoLabel;
        });
    }

    function inicializarTema() {
        const uid = obtenerUsuarioId();
        if (uid) {
            // Para usuarios autenticados, el servidor entrega la preferencia real desde MariaDB
            const serverTheme = normalizar(ROOT.getAttribute('data-theme'));
            aplicarTemaDOM(serverTheme);
            try { localStorage.setItem('cabore_theme_u_' + uid, serverTheme); } catch (_) {}
        } else {
            // Para visitantes anónimos, se lee la preferencia local o se usa modo claro por defecto
            let guardado = 'claro';
            try {
                guardado = normalizar(localStorage.getItem(ANON_KEY));
            } catch (_) {}
            aplicarTemaDOM(guardado);
        }
    }

    function persistirEnServidor(tema) {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (token) headers[header] = token;

        fetch('/api/tema', {
            method: 'POST',
            headers,
            body: new URLSearchParams({ tema })
        }).catch(() => {});
    }

    window.caboreToggleTheme = function () {
        const actual = normalizar(ROOT.getAttribute('data-theme'));
        const nuevo = actual === 'oscuro' ? 'claro' : 'oscuro';
        aplicarTemaDOM(nuevo);

        const uid = obtenerUsuarioId();
        if (uid) {
            try { localStorage.setItem('cabore_theme_u_' + uid, nuevo); } catch (_) {}
            persistirEnServidor(nuevo);
        } else {
            try { localStorage.setItem(ANON_KEY, nuevo); } catch (_) {}
        }
    };

    // Aplicar de inmediato para evitar destellos visuales
    inicializarTema();
    document.addEventListener('DOMContentLoaded', () => {
        actualizarBotones(normalizar(ROOT.getAttribute('data-theme')));
    });
})();
