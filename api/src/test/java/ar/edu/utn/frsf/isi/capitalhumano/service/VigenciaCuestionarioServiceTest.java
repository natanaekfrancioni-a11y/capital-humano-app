package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frsf.isi.capitalhumano.dao.ConfiguracionCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CU026 - controles de vigencia (tiempo máximo, plazo para empezar y cantidad de accesos) */
@ExtendWith(MockitoExtension.class)
class VigenciaCuestionarioServiceTest {

    @Mock
    private CuestionarioDao cuestionarioDao;

    @Mock
    private ConfiguracionCuestionarioDao configuracionDao;

    private VigenciaCuestionarioService service;

    @BeforeEach
    void crearServicio() {
        // la configuración la da EvaluacionCuestionarioService, se usa la clase real con su DAO mockeado
        EvaluacionCuestionarioService evaluacion = new EvaluacionCuestionarioService(
                null, null, null, null, null, cuestionarioDao, null, null, configuracionDao);
        service = new VigenciaCuestionarioService(cuestionarioDao, evaluacion);
    }

    private Cuestionario guardado(String estado) {
        Cuestionario q = Datos.cuestionario(5L, null, null, estado);
        when(cuestionarioDao.findById(5L)).thenReturn(Optional.of(q));
        return q;
    }

    @Test
    void cuestionarioInexistente_informaYNoGuardaNada() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.verificarVigencia(99L, true));

        assertEquals("Cuestionario no encontrado", e.getMessage());
        verify(cuestionarioDao, never()).save(any());
    }

    @Test
    void activoVencido_pasaASinContestarYSeGuardaAunqueSeInformeElError() {
        Cuestionario q = guardado("ACTIVO");
        q.setFechaCreacion(LocalDateTime.now().minusDays(11));
        when(configuracionDao.findAll()).thenReturn(List.of(Datos.configuracion(2, 5, 90, 10)));

        BusinessException e = assertThrows(BusinessException.class, () -> service.verificarVigencia(5L, true));

        assertTrue(e.getMessage().contains("10 días"));
        assertEquals("SIN_CONTESTAR", q.getEstado());
        verify(cuestionarioDao).save(q);
    }

    @Test
    void activoDentroDelPlazo_noGuarda() {
        Cuestionario q = guardado("ACTIVO");
        q.setFechaCreacion(LocalDateTime.now().minusDays(1));
        when(configuracionDao.findAll()).thenReturn(List.of(Datos.configuracion(2, 5, 90, 10)));

        service.verificarVigencia(5L, true);

        assertEquals("ACTIVO", q.getEstado());
        verify(cuestionarioDao, never()).save(any());
    }

    @Test
    void sinConfiguracionCargada_usaLosValoresPorDefecto() {
        // sin fila de configuración el plazo por defecto para empezar es de 30 días
        Cuestionario q = guardado("ACTIVO");
        q.setFechaCreacion(LocalDateTime.now().minusDays(31));

        BusinessException e = assertThrows(BusinessException.class, () -> service.verificarVigencia(5L, true));

        assertTrue(e.getMessage().contains("30 días"));
        verify(cuestionarioDao).save(q);
    }

    @Test
    void enProcesoFueraDeTiempo_pasaAIncompletoYSeGuarda() {
        Cuestionario q = guardado("EN_PROCESO");
        q.setFechaInicio(LocalDateTime.now().minusMinutes(120));
        when(configuracionDao.findAll()).thenReturn(List.of(Datos.configuracion(2, 5, 90, 30)));

        assertThrows(BusinessException.class, () -> service.verificarVigencia(5L, false));

        assertEquals("INCOMPLETO", q.getEstado());
        verify(cuestionarioDao).save(q);
    }

    @Test
    void enProcesoConAccesosAgotados_soloVenceAlVolverAEntrar() {
        Cuestionario q = guardado("EN_PROCESO");
        q.setFechaInicio(LocalDateTime.now().minusMinutes(5));
        q.setCantidadAccesos(5);
        when(configuracionDao.findAll()).thenReturn(List.of(Datos.configuracion(2, 5, 90, 30)));

        service.verificarVigencia(5L, false);
        assertEquals("EN_PROCESO", q.getEstado());

        assertThrows(BusinessException.class, () -> service.verificarVigencia(5L, true));
        assertEquals("INCOMPLETO", q.getEstado());
        verify(cuestionarioDao).save(q);
    }

    @Test
    void completo_noEstaDisponibleYNoSeModifica() {
        Cuestionario q = guardado("COMPLETO");

        BusinessException e = assertThrows(BusinessException.class, () -> service.verificarVigencia(5L, true));

        assertEquals("El cuestionario no se encuentra disponible para ser completado.", e.getMessage());
        assertEquals("COMPLETO", q.getEstado());
        verify(cuestionarioDao, never()).save(any());
    }
}
