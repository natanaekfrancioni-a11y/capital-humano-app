package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Patrón State.
 *
 * Lo que se puede hacer con un cuestionario depende de su estado (enunciado y CU026):
 * <ul>
 *   <li>ACTIVO: todavía no se empezó. Al ingresar se sortean las preguntas y pasa a EN_PROCESO;
 *       si vence el plazo para empezarlo, pasa a SIN_CONTESTAR (flujo 1.B).</li>
 *   <li>EN_PROCESO: se están respondiendo bloques. Al responder el último pasa a COMPLETO; si se
 *       supera el tiempo máximo o la cantidad de accesos, pasa a INCOMPLETO (flujo 1.A).</li>
 *   <li>COMPLETO, INCOMPLETO y SIN_CONTESTAR: estados finales, ya no admiten ingresos ni respuestas.</li>
 * </ul>
 * Cada estado es una clase que implementa esta interfaz, y cada una decide las transiciones.
 * Así los servicios no preguntan "¿en qué estado está?", sino que le delegan la acción al estado.
 */
public interface EstadoCuestionario {

    /** Nombre del estado tal como se guarda en la base de datos. */
    String nombre();

    /**
     * Controla que el cuestionario siga vigente antes de ingresar o responder. Si venció, lo pasa al
     * estado que corresponde y lanza una BusinessException con el mensaje para el candidato.
     */
    void verificarVigencia(Cuestionario cuestionario, ConfiguracionCuestionario configuracion,
                           LocalDateTime ahora, boolean esNuevoIngreso);

    /** Si al ingresar hay que sortear las preguntas (solo la primera vez, en estado ACTIVO). */
    boolean requiereSortearPreguntas();

    /** CU026: el candidato ingresa al cuestionario (cuenta el acceso y, si corresponde, lo inicia). */
    void registrarIngreso(Cuestionario cuestionario, LocalDateTime ahora);

    /** Falla si en este estado no se pueden responder bloques. */
    void validarQueAdmiteRespuestas(Cuestionario cuestionario);

    /** CU026: se respondió el último bloque. Guarda el puntaje y cierra el cuestionario. */
    void completar(Cuestionario cuestionario, BigDecimal puntajeTotal, LocalDateTime ahora);
}
