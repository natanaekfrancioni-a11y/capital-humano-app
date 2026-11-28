package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import java.util.Optional;

/** CU023 - Dar de baja pregunta, usa los ganchos por defecto (no verifica uso ni tiene bajas asociadas) */
public class BajaPregunta extends BajaLogica<Pregunta> {

    private final PreguntaDao preguntaDao;

    public BajaPregunta(PreguntaDao preguntaDao, AuditoriaEliminacionDao auditoriaDao) {
        super(auditoriaDao);
        this.preguntaDao = preguntaDao;
    }

    @Override protected Optional<Pregunta> buscarPorId(Long id) { return preguntaDao.findById(id); }
    @Override protected boolean estaActiva(Pregunta pregunta) { return pregunta.isActiva(); }
    @Override protected String mensajeNoEncontrada() { return "Pregunta no encontrada"; }
    @Override protected void desactivar(Pregunta pregunta) { pregunta.setActiva(false); }
    @Override protected void guardar(Pregunta pregunta) { preguntaDao.save(pregunta); }
    @Override protected String tipoEntidad() { return "PREGUNTA"; }
    @Override protected String nombreParaMensajes() { return "pregunta"; }
    @Override protected Long idDe(Pregunta pregunta) { return pregunta.getId(); }
    @Override protected String identificadorDe(Pregunta pregunta) { return pregunta.getNombre(); }
}
