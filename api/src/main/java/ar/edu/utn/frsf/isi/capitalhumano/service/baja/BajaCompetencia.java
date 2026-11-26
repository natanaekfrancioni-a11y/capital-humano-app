package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CompetenciaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import java.util.Optional;

/** CU015 - Eliminar competencia. No se puede eliminar si tiene factores o si algún puesto la exige. */
public class BajaCompetencia extends BajaLogica<Competencia> {

    private final CompetenciaDao competenciaDao;
    private final FactorDao factorDao;
    private final PuestoDao puestoDao;

    public BajaCompetencia(CompetenciaDao competenciaDao, FactorDao factorDao, PuestoDao puestoDao,
                           AuditoriaEliminacionDao auditoriaDao) {
        super(auditoriaDao);
        this.competenciaDao = competenciaDao;
        this.factorDao = factorDao;
        this.puestoDao = puestoDao;
    }

    @Override
    public Optional<String> motivoPorElQueNoSePuedeEliminar(Competencia competencia) {
        boolean tieneFactores = !factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(competencia.getId()).isEmpty();
        boolean laExigeUnPuesto = puestoDao.findByActivoTrueOrderByNombreAsc().stream()
                .flatMap(puesto -> puesto.getCompetencias().stream())
                .anyMatch(pc -> pc.getCompetencia().getId().equals(competencia.getId()));
        if (tieneFactores || laExigeUnPuesto) {
            return Optional.of("La competencia " + competencia.getNombre()
                    + " está siendo usada en la base de datos y no puede eliminarse");
        }
        return Optional.empty();
    }

    @Override protected Optional<Competencia> buscarPorId(Long id) { return competenciaDao.findById(id); }
    @Override protected boolean estaActiva(Competencia competencia) { return competencia.isActiva(); }
    @Override protected String mensajeNoEncontrada() { return "Competencia no encontrada"; }
    @Override protected void desactivar(Competencia competencia) { competencia.setActiva(false); }
    @Override protected void guardar(Competencia competencia) { competenciaDao.save(competencia); }
    @Override protected String tipoEntidad() { return "COMPETENCIA"; }
    @Override protected String nombreParaMensajes() { return "competencia"; }
    @Override protected Long idDe(Competencia competencia) { return competencia.getId(); }
    @Override protected String identificadorDe(Competencia competencia) { return competencia.getNombre(); }
}
