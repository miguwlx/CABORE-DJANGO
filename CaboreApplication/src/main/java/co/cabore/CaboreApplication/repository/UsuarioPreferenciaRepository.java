package co.cabore.CaboreApplication.repository;

import co.cabore.CaboreApplication.model.UsuarioPreferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioPreferenciaRepository extends JpaRepository<UsuarioPreferencia, Integer> {

    Optional<UsuarioPreferencia> findByUsuarioId(Integer usuarioId);
}
