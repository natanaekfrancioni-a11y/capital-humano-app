package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PuestoDao extends JpaRepository<Puesto, Long> {
    List<Puesto> findByActivoTrueOrderByNombreAsc();
    Optional<Puesto> findByCodigoIgnoreCase(String codigo);
    Optional<Puesto> findByNombreIgnoreCase(String nombre);
}
