package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.AuditoriaEliminacion;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Patrón Template Method.
 *
 * Todas las bajas del catálogo (CU011 puesto, CU015 competencia, CU019 factor, CU023 pregunta y
 * CU031 opción de respuesta) son bajas lógicas y siguen los mismos pasos, siempre en el mismo orden:
 * <ol>
 *   <li>buscar el elemento activo (si no existe, se informa);</li>
 *   <li>verificar que no esté en uso;</li>
 *   <li>dar de baja lo que depende de él;</li>
 *   <li>marcarlo como inactivo;</li>
 *   <li>guardarlo;</li>
 *   <li>registrar la auditoría (quién lo eliminó y cuándo).</li>
 * </ol>
 * El método plantilla {@link #ejecutar} fija ese orden. Cada subclase completa solo los pasos que
 * cambian según la entidad; los pasos 2 y 3 son "ganchos" con un comportamiento por defecto.
 *
 * @param <T> entidad que se da de baja
 */
public abstract class BajaLogica<T> {

    private final AuditoriaEliminacionDao auditoriaDao;

    protected BajaLogica(AuditoriaEliminacionDao auditoriaDao) {
        this.auditoriaDao = auditoriaDao;
    }

    /** Método plantilla. Es final para que ninguna baja se saltee ni reordene los pasos. */
    public final void ejecutar(Long id, String usuarioActor) {
        T entidad = buscarActiva(id);
        motivoPorElQueNoSePuedeEliminar(entidad).ifPresent(motivo -> {
            throw new BusinessException(motivo);
        });
        darDeBajaAsociados(entidad, usuarioActor);
        desactivar(entidad);
        guardar(entidad);
        registrarAuditoria(tipoEntidad(), idDe(entidad), identificadorDe(entidad), usuarioActor,
                "Baja lógica de " + nombreParaMensajes());
    }

    /** Paso 1: devuelve el elemento activo o informa que no existe. También lo usa la verificación previa (paso 2 de CU011 y CU015). */
    public final T buscarActiva(Long id) {
        return buscarPorId(id)
                .filter(this::estaActiva)
                .orElseThrow(() -> new BusinessException(mensajeNoEncontrada()));
    }

    /** Paso 2 (gancho): si el elemento está en uso, devuelve el motivo. Por defecto se puede eliminar. */
    public Optional<String> motivoPorElQueNoSePuedeEliminar(T entidad) {
        return Optional.empty();
    }

    /** Paso 3 (gancho): bajas en cascada. Por defecto no hay ninguna. */
    protected void darDeBajaAsociados(T entidad, String usuarioActor) {
        // Sin elementos asociados.
    }

    protected abstract Optional<T> buscarPorId(Long id);

    protected abstract boolean estaActiva(T entidad);

    protected abstract String mensajeNoEncontrada();

    /** Paso 4. */
    protected abstract void desactivar(T entidad);

    /** Paso 5. */
    protected abstract void guardar(T entidad);

    /** Nombre de la entidad en la auditoría, por ejemplo PUESTO. */
    protected abstract String tipoEntidad();

    /** Nombre de la entidad en los mensajes, por ejemplo "puesto". */
    protected abstract String nombreParaMensajes();

    protected abstract Long idDe(T entidad);

    protected abstract String identificadorDe(T entidad);

    /** Paso 6, común a todas las bajas. */
    protected final void registrarAuditoria(String entidad, Long entidadId, String identificador,
                                            String usuarioActor, String detalle) {
        AuditoriaEliminacion auditoria = new AuditoriaEliminacion();
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setIdentificador(identificador);
        auditoria.setUsuarioActor(usuarioActor == null || usuarioActor.isBlank() ? "CONSULTOR" : usuarioActor.trim().toUpperCase());
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setDetalle(detalle);
        auditoriaDao.save(auditoria);
    }
}
