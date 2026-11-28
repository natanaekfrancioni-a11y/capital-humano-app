package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** por defecto se comporta como un estado final (no admite ingresos ni respuestas) */
abstract class EstadoBase implements EstadoCuestionario {

    static final String NO_DISPONIBLE = "El cuestionario no se encuentra disponible para ser completado.";
    static final String NO_EN_PROCESO = "El cuestionario no se encuentra en proceso";

    @Override
    public void verificarVigencia(Cuestionario cuestionario, ConfiguracionCuestionario configuracion,
                                  LocalDateTime ahora, boolean esNuevoIngreso) {
        throw new BusinessException(NO_DISPONIBLE);
    }

    @Override
    public boolean requiereSortearPreguntas() {
        return false;
    }

    @Override
    public void registrarIngreso(Cuestionario cuestionario, LocalDateTime ahora) {
        throw new BusinessException(NO_DISPONIBLE);
    }

    @Override
    public void validarQueAdmiteRespuestas(Cuestionario cuestionario) {
        throw new BusinessException(NO_EN_PROCESO);
    }

    @Override
    public void completar(Cuestionario cuestionario, BigDecimal puntajeTotal, LocalDateTime ahora) {
        throw new BusinessException(NO_EN_PROCESO);
    }

    /** cambio de estado, lo hacen siempre los estados y nunca los servicios */
    protected void pasarA(Cuestionario cuestionario, EstadoCuestionario nuevoEstado, LocalDateTime ahora) {
        cuestionario.setEstado(nuevoEstado.nombre());
        cuestionario.setFechaCambioEstado(ahora);
    }

    protected void contarAcceso(Cuestionario cuestionario, LocalDateTime ahora) {
        cuestionario.setCantidadAccesos(cuestionario.getCantidadAccesos() + 1);
        cuestionario.setFechaUltimoAcceso(ahora);
    }

    @Override
    public String toString() {
        return nombre();
    }
}
