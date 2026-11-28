package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EvaluacionDao extends JpaRepository<Evaluacion, Long> {
    /** CU028: evaluaciones del puesto en orden (EVA-001, EVA-002, ...) */
    List<Evaluacion> findByPuestoIdOrderByNroEvaluacionAsc(Long puestoId);

    /** CU025: última evaluación del puesto, para numerar la siguiente */
    Optional<Evaluacion> findFirstByPuestoIdOrderByNroEvaluacionDesc(Long puestoId);
}
