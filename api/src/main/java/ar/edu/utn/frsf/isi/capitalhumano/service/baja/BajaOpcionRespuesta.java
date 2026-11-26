package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.OpcionRespuestaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import java.util.Optional;

/** CU031 - Eliminar opción de respuesta. No se puede eliminar si alguna pregunta activa la usa. */
public class BajaOpcionRespuesta extends BajaLogica<OpcionRespuesta> {

    private final OpcionRespuestaDao opcionRespuestaDao;
    private final PreguntaDao preguntaDao;

    public BajaOpcionRespuesta(OpcionRespuestaDao opcionRespuestaDao, PreguntaDao preguntaDao,
                               AuditoriaEliminacionDao auditoriaDao) {
        super(auditoriaDao);
        this.opcionRespuestaDao = opcionRespuestaDao;
        this.preguntaDao = preguntaDao;
    }

    @Override
    public Optional<String> motivoPorElQueNoSePuedeEliminar(OpcionRespuesta opcion) {
        if (preguntaDao.existsByOpcionRespuestaIdAndActivaTrue(opcion.getId())) {
            return Optional.of("La opción de respuesta " + opcion.getNombre()
                    + " está siendo usada en la base de datos y no puede eliminarse");
        }
        return Optional.empty();
    }

    @Override protected Optional<OpcionRespuesta> buscarPorId(Long id) { return opcionRespuestaDao.findById(id); }
    @Override protected boolean estaActiva(OpcionRespuesta opcion) { return opcion.isActiva(); }
    @Override protected String mensajeNoEncontrada() { return "Opción de respuesta no encontrada"; }
    @Override protected void desactivar(OpcionRespuesta opcion) { opcion.setActiva(false); }
    @Override protected void guardar(OpcionRespuesta opcion) { opcionRespuestaDao.save(opcion); }
    @Override protected String tipoEntidad() { return "OPCION_RESPUESTA"; }
    @Override protected String nombreParaMensajes() { return "opción de respuesta"; }
    @Override protected Long idDe(OpcionRespuesta opcion) { return opcion.getId(); }
    @Override protected String identificadorDe(OpcionRespuesta opcion) { return opcion.getNombre(); }
}
