package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CuestionarioDao extends JpaRepository<Cuestionario, Long> {
    List<Cuestionario> findByCandidatoIdAndEstadoIn(Long candidatoId, List<String> estados);
    Optional<Cuestionario> findFirstByCandidatoIdAndEstadoInOrderByIdDesc(Long candidatoId, List<String> estados);
    List<Cuestionario> findByPuestoIdOrderByIdAsc(Long puestoId);
    boolean existsByPuestoId(Long puestoId);
}
