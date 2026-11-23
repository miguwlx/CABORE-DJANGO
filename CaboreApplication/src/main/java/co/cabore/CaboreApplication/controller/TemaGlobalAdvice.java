package co.cabore.CaboreApplication.controller;

import co.cabore.CaboreApplication.config.UsuarioPrincipal;
import co.cabore.CaboreApplication.service.TemaService;
import co.cabore.CaboreApplication.service.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Inyecta globalmente la preferencia de tema visual del usuario autenticado
 * ('claro' por defecto) en todas las vistas Thymeleaf de la plataforma.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TemaGlobalAdvice {

    private final TemaService temaService;

    @ModelAttribute
    public void inyectarTemaGlobal(Model model) {
        UsuarioPrincipal up = UsuarioActual.principal();
        if (up != null && up.getId() != null) {
            model.addAttribute("temaActual", temaService.obtenerTemaUsuario(up.getId()));
            model.addAttribute("temaUsuarioId", up.getId());
        } else {
            model.addAttribute("temaActual", TemaService.TEMA_CLARO);
            model.addAttribute("temaUsuarioId", null);
        }
    }
}
