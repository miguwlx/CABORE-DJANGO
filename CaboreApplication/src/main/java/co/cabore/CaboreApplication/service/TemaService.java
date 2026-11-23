package co.cabore.CaboreApplication.service;

import co.cabore.CaboreApplication.model.UsuarioPreferencia;
import co.cabore.CaboreApplication.repository.UsuarioPreferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio centralizado de gestión de temas visuales por usuario ('claro' por defecto, 'oscuro' opcional).
 */
@Service
@RequiredArgsConstructor
public class TemaService {

    public static final String TEMA_CLARO = "claro";
    public static final String TEMA_OSCURO = "oscuro";

    private final UsuarioPreferenciaRepository preferencias;

    @Transactional(readOnly = true)
    public String obtenerTemaUsuario(Integer usuarioId) {
        if (usuarioId == null) {
            return TEMA_CLARO;
        }
        return preferencias.findByUsuarioId(usuarioId)
                .map(UsuarioPreferencia::getTema)
                .map(TemaService::normalizar)
                .orElse(TEMA_CLARO);
    }

    @Transactional
    public String guardarTemaUsuario(Integer usuarioId, String temaSolicitado) {
        String temaNorm = normalizar(temaSolicitado);
        if (usuarioId == null) {
            return temaNorm;
        }
        UsuarioPreferencia pref = preferencias.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    UsuarioPreferencia nueva = new UsuarioPreferencia();
                    nueva.setUsuarioId(usuarioId);
                    return nueva;
                });
        pref.setTema(temaNorm);
        pref.setActualizadoEn(LocalDateTime.now());
        preferencias.save(pref);
        return temaNorm;
    }

    public static String normalizar(String tema) {
        if (tema == null) return TEMA_CLARO;
        String t = tema.trim().toLowerCase();
        if ("oscuro".equals(t) || "dark".equals(t)) {
            return TEMA_OSCURO;
        }
        return TEMA_CLARO;
    }
}
