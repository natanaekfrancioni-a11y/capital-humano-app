package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.estado.EstadosCuestionario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/** CU026 - Completar cuestionario: controles de vigencia, con noRollbackFor el cambio de estado se guarda igual */
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

    /** verifica que siga vigente (esNuevoIngreso es true cuando el candidato entra o vuelve a entrar) */
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
