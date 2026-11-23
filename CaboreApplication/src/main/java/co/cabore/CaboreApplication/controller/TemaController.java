package co.cabore.CaboreApplication.controller;

import co.cabore.CaboreApplication.config.UsuarioPrincipal;
import co.cabore.CaboreApplication.service.TemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API de preferencias de tema visual (claro / oscuro) para usuarios autenticados
 * de cualquier rol (cliente, emprendedor, administrador).
 */
@RestController
@RequestMapping("/api/tema")
@RequiredArgsConstructor
public class TemaController {

    private final TemaService temaService;

    @GetMapping
    public Map<String, Object> obtener(@AuthenticationPrincipal UsuarioPrincipal yo) {
        String tema = yo == null ? TemaService.TEMA_CLARO : temaService.obtenerTemaUsuario(yo.getId());
        return Map.of("ok", true, "tema", tema);
    }

    @PostMapping
    public Map<String, Object> guardar(
            @AuthenticationPrincipal UsuarioPrincipal yo,
            @RequestParam(name = "tema", defaultValue = "claro") String tema) {
        String guardado = temaService.guardarTemaUsuario(yo.getId(), tema);
        return Map.of("ok", true, "tema", guardado);
    }
}
