package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OpcionRespuestaDao extends JpaRepository<OpcionRespuesta, Long> {
    List<OpcionRespuesta> findByActivaTrueOrderByNombreAsc();
    List<OpcionRespuesta> findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);
    Optional<OpcionRespuesta> findByNombreIgnoreCase(String nombre);
}
