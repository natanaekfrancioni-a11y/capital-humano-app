package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FactorDao extends JpaRepository<Factor, Long> {
    List<Factor> findByActivoTrueOrderByNombreAsc();
    List<Factor> findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(Long competenciaId);
    Optional<Factor> findByCodigoIgnoreCase(String codigo);
    boolean existsByCompetenciaIdAndNroOrdenAndActivoTrue(Long competenciaId, Integer nroOrden);
    boolean existsByCompetenciaIdAndNombreIgnoreCaseAndActivoTrue(Long competenciaId, String nombre);
}
