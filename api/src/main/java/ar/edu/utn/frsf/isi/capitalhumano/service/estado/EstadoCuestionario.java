package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** patrón State: lo que se puede hacer con un cuestionario depende de su estado */
public interface EstadoCuestionario {

    /** nombre del estado como se guarda en la base */
    String nombre();

    /** controla que siga vigente, si venció lo pasa al estado que corresponde y lanza BusinessException */
    void verificarVigencia(Cuestionario cuestionario, ConfiguracionCuestionario configuracion,
                           LocalDateTime ahora, boolean esNuevoIngreso);

    /** si al ingresar hay que sortear las preguntas (solo la primera vez, en ACTIVO) */
    boolean requiereSortearPreguntas();

    /** CU026: el candidato ingresa al cuestionario (cuenta el acceso y si corresponde lo inicia) */
    void registrarIngreso(Cuestionario cuestionario, LocalDateTime ahora);

    /** falla si en este estado no se pueden responder bloques */
    void validarQueAdmiteRespuestas(Cuestionario cuestionario);

    /** CU026: se respondió el último bloque, guarda el puntaje y cierra el cuestionario */
    void completar(Cuestionario cuestionario, BigDecimal puntajeTotal, LocalDateTime ahora);
}
