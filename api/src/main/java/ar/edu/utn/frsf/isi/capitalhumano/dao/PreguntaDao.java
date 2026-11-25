package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PreguntaDao extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByActivaTrueOrderByIdAsc();
    List<Pregunta> findByFactorIdAndActivaTrueOrderByIdAsc(Long factorId);
    Optional<Pregunta> findByCodigoIgnoreCase(String codigo);
    boolean existsByFactorIdAndNombreIgnoreCaseAndActivaTrue(Long factorId, String nombre);
    boolean existsByFactorIdAndEnunciadoIgnoreCaseAndActivaTrue(Long factorId, String enunciado);
    boolean existsByOpcionRespuestaIdAndActivaTrue(Long opcionRespuestaId);
    long countByFactorId(Long factorId);
}
