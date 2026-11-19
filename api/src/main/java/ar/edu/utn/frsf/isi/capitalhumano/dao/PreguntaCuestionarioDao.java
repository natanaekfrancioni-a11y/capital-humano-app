package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaCuestionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreguntaCuestionarioDao extends JpaRepository<PreguntaCuestionario, Long> {
    List<PreguntaCuestionario> findByCuestionarioIdOrderByOrdenVisualAsc(Long cuestionarioId);
    List<PreguntaCuestionario> findByCuestionarioIdAndNroBloqueOrderByOrdenVisualAsc(Long cuestionarioId, Integer nroBloque);
}
