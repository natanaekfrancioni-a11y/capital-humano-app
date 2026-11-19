package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompetenciaDao extends JpaRepository<Competencia, Long> {
    List<Competencia> findByActivaTrueOrderByNombreAsc();
    List<Competencia> findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);
    Optional<Competencia> findByCodigoIgnoreCase(String codigo);
    Optional<Competencia> findByNombreIgnoreCase(String nombre);
}
