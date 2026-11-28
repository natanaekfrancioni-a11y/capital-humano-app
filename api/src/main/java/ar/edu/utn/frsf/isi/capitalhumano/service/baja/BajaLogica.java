package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.AuditoriaEliminacion;
import java.time.LocalDateTime;
import java.util.Optional;

/** patrón Template Method para las bajas lógicas del catálogo (CU023) */
public abstract class BajaLogica<T> {

    private final AuditoriaEliminacionDao auditoriaDao;

    protected BajaLogica(AuditoriaEliminacionDao auditoriaDao) {
        this.auditoriaDao = auditoriaDao;
    }

    /** método plantilla, es final para que ninguna baja cambie el orden */
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

    /** devuelve el elemento activo o avisa que no existe */
    public final T buscarActiva(Long id) {
        return buscarPorId(id)
                .filter(this::estaActiva)
                .orElseThrow(() -> new BusinessException(mensajeNoEncontrada()));
    }

    /** gancho: si el elemento está en uso devuelve el motivo, por defecto se puede eliminar */
    public Optional<String> motivoPorElQueNoSePuedeEliminar(T entidad) {
        return Optional.empty();
    }

    /** gancho: bajas en cascada, por defecto no hay ninguna */
    protected void darDeBajaAsociados(T entidad, String usuarioActor) {
        // sin elementos asociados
    }

    protected abstract Optional<T> buscarPorId(Long id);

    protected abstract boolean estaActiva(T entidad);

    protected abstract String mensajeNoEncontrada();

    protected abstract void desactivar(T entidad);

    protected abstract void guardar(T entidad);

    /** nombre de la entidad en la auditoría, por ejemplo PUESTO */
    protected abstract String tipoEntidad();

    /** nombre de la entidad en los mensajes, por ejemplo "puesto" */
    protected abstract String nombreParaMensajes();

    protected abstract Long idDe(T entidad);

    protected abstract String identificadorDe(T entidad);

    /** auditoría, igual para todas las bajas */
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
