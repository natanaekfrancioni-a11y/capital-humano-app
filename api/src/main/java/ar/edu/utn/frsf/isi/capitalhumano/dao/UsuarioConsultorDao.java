package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.UsuarioConsultor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioConsultorDao extends JpaRepository<UsuarioConsultor, Long> {
    Optional<UsuarioConsultor> findByUsernameIgnoreCase(String username);
}
