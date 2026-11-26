package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import java.util.Optional;

/** CU011 - Dar de baja puesto. No se puede eliminar si tiene evaluaciones (cuestionarios). */
public class BajaPuesto extends BajaLogica<Puesto> {

    private final PuestoDao puestoDao;
    private final CuestionarioDao cuestionarioDao;

    public BajaPuesto(PuestoDao puestoDao, CuestionarioDao cuestionarioDao, AuditoriaEliminacionDao auditoriaDao) {
        super(auditoriaDao);
        this.puestoDao = puestoDao;
        this.cuestionarioDao = cuestionarioDao;
    }

    @Override
    public Optional<String> motivoPorElQueNoSePuedeEliminar(Puesto puesto) {
        if (cuestionarioDao.existsByPuestoId(puesto.getId())) {
            return Optional.of("El puesto " + puesto.getNombre()
                    + " está siendo usado en la base de datos y no puede eliminarse");
        }
        return Optional.empty();
    }

    @Override protected Optional<Puesto> buscarPorId(Long id) { return puestoDao.findById(id); }
    @Override protected boolean estaActiva(Puesto puesto) { return puesto.isActivo(); }
    @Override protected String mensajeNoEncontrada() { return "Puesto no encontrado"; }
    @Override protected void desactivar(Puesto puesto) { puesto.setActivo(false); }
    @Override protected void guardar(Puesto puesto) { puestoDao.save(puesto); }
    @Override protected String tipoEntidad() { return "PUESTO"; }
    @Override protected String nombreParaMensajes() { return "puesto"; }
    @Override protected Long idDe(Puesto puesto) { return puesto.getId(); }
    @Override protected String identificadorDe(Puesto puesto) { return puesto.getNombre(); }
}
