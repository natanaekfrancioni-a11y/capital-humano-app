package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.estado.EstadoCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.estado.EstadosCuestionario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

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
     * Verifica que el cuestionario siga vigente. La regla depende del estado (patrón State): ver
     * {@link EstadoCuestionario#verificarVigencia}. Si el cuestionario venció, el estado ya lo pasó a
     * INCOMPLETO o SIN_CONTESTAR: ese cambio se guarda aunque se informe el error (noRollbackFor).
     *
     * @param esNuevoIngreso true cuando el candidato entra (o vuelve a entrar) al cuestionario;
     *                       false cuando solo está respondiendo un bloque.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void verificarVigencia(Long cuestionarioId, boolean esNuevoIngreso) {
        Cuestionario q = cuestionarioDao.findById(cuestionarioId)
                .orElseThrow(() -> new BusinessException("Cuestionario no encontrado"));
        ConfiguracionCuestionario cfg = evaluacionService.obtenerConfiguracion();

        String estadoAnterior = q.getEstado();
        try {
            EstadosCuestionario.de(q).verificarVigencia(q, cfg, LocalDateTime.now(), esNuevoIngreso);
        } finally {
            if (!Objects.equals(estadoAnterior, q.getEstado())) {
                cuestionarioDao.save(q);
            }
        }
    }
}
