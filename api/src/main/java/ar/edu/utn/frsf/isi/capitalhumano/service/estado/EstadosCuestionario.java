package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** los cinco estados posibles, como no guardan datos hay una sola instancia de cada uno */
public final class EstadosCuestionario {

    public static final EstadoCuestionario ACTIVO = new EstadoActivo();
    public static final EstadoCuestionario EN_PROCESO = new EstadoEnProceso();
    public static final EstadoCuestionario COMPLETO = new EstadoCompleto();
    public static final EstadoCuestionario INCOMPLETO = new EstadoIncompleto();
    public static final EstadoCuestionario SIN_CONTESTAR = new EstadoSinContestar();

    private static final Map<String, EstadoCuestionario> POR_NOMBRE = List
            .of(ACTIVO, EN_PROCESO, COMPLETO, INCOMPLETO, SIN_CONTESTAR).stream()
            .collect(Collectors.toUnmodifiableMap(EstadoCuestionario::nombre, Function.identity()));

    private EstadosCuestionario() {
    }

    public static EstadoCuestionario de(String nombre) {
        EstadoCuestionario estado = nombre == null ? null : POR_NOMBRE.get(nombre);
        if (estado == null) {
            throw new BusinessException("Estado de cuestionario desconocido: " + nombre);
        }
        return estado;
    }

    public static EstadoCuestionario de(Cuestionario cuestionario) {
        return de(cuestionario.getEstado());
    }
}
