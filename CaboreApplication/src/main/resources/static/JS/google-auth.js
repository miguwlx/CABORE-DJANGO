/* ==========================================================
   google-auth.js — Registro / inicio de sesión con Google
   Caboré. Archivo independiente (migrado a Spring Boot).
   Renderiza el botón oficial de Google Identity Services
   adaptado al tema global (outline / filled_black) y al ancho
   del formulario sin alterar el flujo de autenticación.
   ========================================================== */
function mostrarErrorGoogle(msg) {
    const panelEmp = document.getElementById('panel-emprendedor');
    const esEmp = panelEmp && panelEmp.classList.contains('active');
    const boxLogin = document.getElementById(esEmp ? 'err-emprendedor' : 'err-cliente');
    if (boxLogin) {
        boxLogin.innerHTML = '⚠️&nbsp; ' + msg;
        boxLogin.classList.add('visible');
        return;
    }
    const boxReg = document.getElementById('err-registro');
    if (boxReg) {
        boxReg.textContent = msg;
        boxReg.style.display = 'block';
        return;
    }
    alert(msg);
}

function handleGoogleCredential(response) {
    if (!response || !response.credential) {
        mostrarErrorGoogle('No se recibió la credencial de Google.');
        return;
    }

    const rolInput = document.querySelector('input[name="rol"]:checked');
    const panelEmp = document.getElementById('panel-emprendedor');
    let rol = 'cliente';
    if (rolInput) {
        rol = rolInput.value;
    } else if (panelEmp && panelEmp.classList.contains('active')) {
        rol = 'emprendedor';
    }

    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    fetch('/auth/google', {
        method: 'POST',
        headers: headers,
        body: 'credential=' + encodeURIComponent(response.credential) + '&rol=' + encodeURIComponent(rol)
    })
    .then(r => r.json())
    .then(data => {
        if (data.ok) {
            window.location.href = data.redirect;
        } else {
            mostrarErrorGoogle(data.error || 'No se pudo continuar con Google.');
        }
    })
    .catch(() => mostrarErrorGoogle('Error de conexión al verificar con Google.'));
}

/* ── Renderizado oficial de Google Identity Services sincronizado con el tema ── */
(function () {
    let gsiInitialized = false;
    let resizeTimer = null;

    function esModoOscuro() {
        return document.documentElement.getAttribute('data-theme') === 'oscuro';
    }

    function calcularAnchoBoton(slot) {
        const contenedor = slot.closest('.google-auth-section') || slot.parentElement;
        const disponible = contenedor ? Math.floor(contenedor.clientWidth) : 340;
        if (disponible <= 0) return 320;
        return Math.max(240, Math.min(360, disponible));
    }

    function renderizarBotonesGoogle() {
        if (!window.google || !google.accounts || !google.accounts.id) return false;

        const configEl = document.getElementById('g_id_onload');
        const clientId = configEl ? configEl.getAttribute('data-client_id') : '';
        if (!clientId) return true;

        if (!gsiInitialized) {
            google.accounts.id.initialize({
                client_id: clientId,
                callback: handleGoogleCredential,
                auto_select: false
            });
            gsiInitialized = true;
        }

        const theme = esModoOscuro() ? 'filled_black' : 'outline';
        const slots = document.querySelectorAll('[data-google-btn]');
        slots.forEach(slot => {
            const width = calcularAnchoBoton(slot);
            slot.innerHTML = '';
            google.accounts.id.renderButton(slot, {
                type: 'standard',
                theme: theme,
                size: 'large',
                shape: 'pill',
                text: 'continue_with',
                logo_alignment: 'left',
                width: width,
                locale: 'es'
            });
        });
        return true;
    }

    function iniciarCuandoCargueGsi() {
        if (renderizarBotonesGoogle()) return;
        let intentos = 0;
        const intervalo = setInterval(() => {
            intentos++;
            if (renderizarBotonesGoogle() || intentos >= 50) {
                clearInterval(intervalo);
            }
        }, 120);
    }

    document.addEventListener('DOMContentLoaded', iniciarCuandoCargueGsi);
    window.addEventListener('load', renderizarBotonesGoogle);
    window.addEventListener('cabore:theme-changed', () => {
        renderizarBotonesGoogle();
    });
    window.addEventListener('resize', () => {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(renderizarBotonesGoogle, 180);
    });
})();
