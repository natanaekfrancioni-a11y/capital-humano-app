package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * CU026 - Completar cuestionario: controles de vigencia (flujos 1.A, 1.B y precondición de accesos).
 *
 * Si el cuestionario venció se actualiza su estado y se lanza una BusinessException.
 * La transacción NO se revierte ante esa excepción (noRollbackFor) para que el cambio
 * de estado quede registrado en la base de datos.
 */
@Service
public class VigenciaCuestionarioService {

    private final CuestionarioDao cuestionarioDao;
    private final EvaluacionCuestionarioService evaluacionService;

    public VigenciaCuestionarioService(
            CuestionarioDao cuestionarioDao,
            EvaluacionCuestionarioService evaluacionService
    ) {
        this.cuestionarioDao = cuestionarioDao;
        this.evaluacionService = evaluacionService;
    }

    /**
     * @param esNuevoIngreso true cuando el candidato entra (o vuelve a entrar) al cuestionario;
     *                       false cuando solo está respondiendo un bloque.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void verificarVigencia(Long cuestionarioId, boolean esNuevoIngreso) {
        Cuestionario q = cuestionarioDao.findById(cuestionarioId)
                .orElseThrow(() -> new BusinessException("Cuestionario no encontrado"));

        ConfiguracionCuestionario cfg = evaluacionService.obtenerConfiguracion();
        LocalDateTime ahora = LocalDateTime.now();

        switch (q.getEstado()) {
            case "ACTIVO" -> {
                // 1.B: el cuestionario superó el tiempo máximo permitido para el estado activo
                LocalDateTime limiteActivo = q.getFechaCreacion().plusDays(cfg.getDiasVencimientoActivo());
                if (ahora.isAfter(limiteActivo)) {
                    cambiarEstado(q, "SIN_CONTESTAR", ahora);
                    throw new BusinessException(
                            "El cuestionario superó el plazo de " + cfg.getDiasVencimientoActivo()
                                    + " días para ser iniciado y pasó al estado Sin contestar.");
                }
            }
            case "EN_PROCESO" -> {
                // 1.A: el usuario superó el tiempo máximo para completar el cuestionario
                if (q.getFechaInicio() != null
                        && ahora.isAfter(q.getFechaInicio().plusMinutes(cfg.getTiempoMaxMinutos()))) {
                    cambiarEstado(q, "INCOMPLETO", ahora);
                    throw new BusinessException(
                            "Se superó el tiempo máximo de " + cfg.getTiempoMaxMinutos()
                                    + " minutos para completar el cuestionario. Las respuestas enviadas quedaron "
                                    + "guardadas y el cuestionario pasó al estado Incompleto.");
                }
                // Precondición: no se haya superado la cantidad máxima de accesos permitidos
                if (esNuevoIngreso && q.getCantidadAccesos() >= cfg.getMaxAccesos()) {
                    cambiarEstado(q, "INCOMPLETO", ahora);
                    throw new BusinessException(
                            "Se superó la cantidad máxima de " + cfg.getMaxAccesos()
                                    + " accesos permitidos. El cuestionario pasó al estado Incompleto.");
                }
            }
            default -> throw new BusinessException(
                    "El cuestionario no se encuentra disponible para ser completado.");
        }
    }

    private void cambiarEstado(Cuestionario q, String estado, LocalDateTime ahora) {
        q.setEstado(estado);
        q.setFechaCambioEstado(ahora);
        cuestionarioDao.save(q);
    }
}
