package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import java.util.Optional;

/** CU019 - Eliminar factor. Las preguntas del factor también se dan de baja (paso 3 de la plantilla). */
public class BajaFactor extends BajaLogica<Factor> {

    private final FactorDao factorDao;
    private final PreguntaDao preguntaDao;

    public BajaFactor(FactorDao factorDao, PreguntaDao preguntaDao, AuditoriaEliminacionDao auditoriaDao) {
        super(auditoriaDao);
        this.factorDao = factorDao;
        this.preguntaDao = preguntaDao;
    }

    @Override
    protected void darDeBajaAsociados(Factor factor, String usuarioActor) {
        for (Pregunta pregunta : preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(factor.getId())) {
            pregunta.setActiva(false);
            preguntaDao.save(pregunta);
            registrarAuditoria("PREGUNTA", pregunta.getId(), pregunta.getNombre(), usuarioActor,
                    "Baja lógica por eliminación de factor " + factor.getNombre());
        }
    }

    @Override protected Optional<Factor> buscarPorId(Long id) { return factorDao.findById(id); }
    @Override protected boolean estaActiva(Factor factor) { return factor.isActivo(); }
    @Override protected String mensajeNoEncontrada() { return "Factor no encontrado"; }
    @Override protected void desactivar(Factor factor) { factor.setActivo(false); }
    @Override protected void guardar(Factor factor) { factorDao.save(factor); }
    @Override protected String tipoEntidad() { return "FACTOR"; }
    @Override protected String nombreParaMensajes() { return "factor"; }
    @Override protected Long idDe(Factor factor) { return factor.getId(); }
    @Override protected String identificadorDe(Factor factor) { return factor.getNombre(); }
}
