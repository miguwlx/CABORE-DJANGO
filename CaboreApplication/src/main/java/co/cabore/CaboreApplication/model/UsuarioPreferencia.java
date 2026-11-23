package co.cabore.CaboreApplication.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Preferencias visuales individuales por usuario (tabla usuario_preferencias).
 * Almacena el tema elegido ('claro' o 'oscuro') de forma independiente para cada cuenta.
 */
@Entity
@Table(name = "usuario_preferencias")
@Getter
@Setter
public class UsuarioPreferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Integer usuarioId;

    @Column(length = 16, nullable = false)
    private String tema = "claro";

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
