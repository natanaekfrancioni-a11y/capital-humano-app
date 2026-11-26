package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.time.LocalDateTime;

/** ACTIVO: generado en CU025 y todavía no iniciado por el candidato. */
public class EstadoActivo extends EstadoBase {

    @Override
    public String nombre() {
        return "ACTIVO";
    }

    /** CU026 1.B: si pasó el plazo para empezarlo, queda SIN_CONTESTAR. */
    @Override
    public void verificarVigencia(Cuestionario cuestionario, ConfiguracionCuestionario configuracion,
                                  LocalDateTime ahora, boolean esNuevoIngreso) {
        LocalDateTime limite = cuestionario.getFechaCreacion().plusDays(configuracion.getDiasVencimientoActivo());
        if (ahora.isAfter(limite)) {
            pasarA(cuestionario, EstadosCuestionario.SIN_CONTESTAR, ahora);
            throw new BusinessException("El cuestionario superó el plazo de " + configuracion.getDiasVencimientoActivo()
                    + " días para ser iniciado y pasó al estado Sin contestar.");
        }
    }

    @Override
    public boolean requiereSortearPreguntas() {
        return true;
    }

    /** Primer ingreso: el cuestionario empieza en el bloque 1 y pasa a EN_PROCESO. */
    @Override
    public void registrarIngreso(Cuestionario cuestionario, LocalDateTime ahora) {
        pasarA(cuestionario, EstadosCuestionario.EN_PROCESO, ahora);
        cuestionario.setBloqueActual(1);
        cuestionario.setFechaInicio(ahora);
        contarAcceso(cuestionario, ahora);
    }
}
