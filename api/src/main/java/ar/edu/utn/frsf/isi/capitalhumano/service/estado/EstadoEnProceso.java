package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** EN_PROCESO: el candidato ya empezó y puede seguir respondiendo bloques. */
public class EstadoEnProceso extends EstadoBase {

    @Override
    public String nombre() {
        return "EN_PROCESO";
    }

    /** CU026 1.A (tiempo máximo) y precondición de cantidad máxima de accesos: si se superan, queda INCOMPLETO. */
    @Override
    public void verificarVigencia(Cuestionario cuestionario, ConfiguracionCuestionario configuracion,
                                  LocalDateTime ahora, boolean esNuevoIngreso) {
        if (cuestionario.getFechaInicio() != null
                && ahora.isAfter(cuestionario.getFechaInicio().plusMinutes(configuracion.getTiempoMaxMinutos()))) {
            pasarA(cuestionario, EstadosCuestionario.INCOMPLETO, ahora);
            throw new BusinessException("Se superó el tiempo máximo de " + configuracion.getTiempoMaxMinutos()
                    + " minutos para completar el cuestionario. Las respuestas enviadas quedaron guardadas y el "
                    + "cuestionario pasó al estado Incompleto.");
        }
        if (esNuevoIngreso && cuestionario.getCantidadAccesos() >= configuracion.getMaxAccesos()) {
            pasarA(cuestionario, EstadosCuestionario.INCOMPLETO, ahora);
            throw new BusinessException("Se superó la cantidad máxima de " + configuracion.getMaxAccesos()
                    + " accesos permitidos. El cuestionario pasó al estado Incompleto.");
        }
    }

    /** Vuelve a entrar para continuar donde dejó: solo se cuenta el acceso. */
    @Override
    public void registrarIngreso(Cuestionario cuestionario, LocalDateTime ahora) {
        contarAcceso(cuestionario, ahora);
    }

    @Override
    public void validarQueAdmiteRespuestas(Cuestionario cuestionario) {
        // En proceso se pueden responder bloques.
    }

    @Override
    public void completar(Cuestionario cuestionario, BigDecimal puntajeTotal, LocalDateTime ahora) {
        cuestionario.setPuntajeTotal(puntajeTotal);
        cuestionario.setFechaFin(ahora);
        pasarA(cuestionario, EstadosCuestionario.COMPLETO, ahora);
    }
}
