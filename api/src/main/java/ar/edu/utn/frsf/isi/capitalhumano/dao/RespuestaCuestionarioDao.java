package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RespuestaCuestionarioDao extends JpaRepository<RespuestaCuestionario, Long> {
    Optional<RespuestaCuestionario> findByPreguntaCuestionarioId(Long preguntaCuestionarioId);
}
