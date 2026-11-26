package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.Datos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Patrón State: cada estado del cuestionario decide qué se puede hacer y a qué estado se pasa.
 * Son objetos sin dependencias, así que se prueban directamente (sin mocks).
 */
class EstadosCuestionarioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 26, 10, 0);
    private static final ConfiguracionCuestionario CONFIG = Datos.configuracion(2, 3, 90, 30);
    private static final String NO_DISPONIBLE = "El cuestionario no se encuentra disponible para ser completado.";
    private static final String NO_EN_PROCESO = "El cuestionario no se encuentra en proceso";

    private static Cuestionario cuestionario(String estado) {
        return Datos.cuestionario(1L, null, null, estado);
    }

    // ------------------------------------------------------------------ fábrica de estados

    @Test
    void de_traduceElTextoGuardadoAlObjetoEstado() {
        assertSame(EstadosCuestionario.ACTIVO, EstadosCuestionario.de("ACTIVO"));
        assertSame(EstadosCuestionario.EN_PROCESO, EstadosCuestionario.de("EN_PROCESO"));
        assertSame(EstadosCuestionario.COMPLETO, EstadosCuestionario.de("COMPLETO"));
        assertSame(EstadosCuestionario.INCOMPLETO, EstadosCuestionario.de("INCOMPLETO"));
        assertSame(EstadosCuestionario.SIN_CONTESTAR, EstadosCuestionario.de(cuestionario("SIN_CONTESTAR")));
        assertEquals("EN_PROCESO", EstadosCuestionario.EN_PROCESO.toString());
    }

    @Test
    void de_estadoDesconocidoONulo_informaElError() {
        assertEquals("Estado de cuestionario desconocido: PAUSADO",
                assertThrows(BusinessException.class, () -> EstadosCuestionario.de("PAUSADO")).getMessage());
        assertEquals("Estado de cuestionario desconocido: null",
                assertThrows(BusinessException.class, () -> EstadosCuestionario.de((String) null)).getMessage());
    }

    // ------------------------------------------------------------------ ACTIVO

    @Test
    void activo_dentroDelPlazo_noCambiaDeEstado() {
        Cuestionario q = cuestionario("ACTIVO");
        q.setFechaCreacion(AHORA.minusDays(29));

        EstadosCuestionario.ACTIVO.verificarVigencia(q, CONFIG, AHORA, true);

        assertEquals("ACTIVO", q.getEstado());
        assertNull(q.getFechaCambioEstado());
    }

    @Test
    void activo_vencidoElPlazoParaEmpezarlo_pasaASinContestar() {
        Cuestionario q = cuestionario("ACTIVO");
        q.setFechaCreacion(AHORA.minusDays(31));

        BusinessException e = assertThrows(BusinessException.class,
                () -> EstadosCuestionario.ACTIVO.verificarVigencia(q, CONFIG, AHORA, true));

        assertEquals("El cuestionario superó el plazo de 30 días para ser iniciado y pasó al estado Sin contestar.", e.getMessage());
        assertEquals("SIN_CONTESTAR", q.getEstado());
        assertEquals(AHORA, q.getFechaCambioEstado());
    }

    @Test
    void activo_primerIngreso_pasaAEnProcesoEnElBloque1() {
        Cuestionario q = cuestionario("ACTIVO");
        q.setBloqueActual(3);

        assertTrue(EstadosCuestionario.ACTIVO.requiereSortearPreguntas());
        EstadosCuestionario.ACTIVO.registrarIngreso(q, AHORA);

        assertEquals("EN_PROCESO", q.getEstado());
        assertEquals(1, q.getBloqueActual());
        assertEquals(AHORA, q.getFechaInicio());
        assertEquals(1, q.getCantidadAccesos());
        assertEquals(AHORA, q.getFechaUltimoAcceso());
    }

    @Test
    void activo_noAdmiteRespuestasNiSePuedeCompletar() {
        Cuestionario q = cuestionario("ACTIVO");

        assertEquals(NO_EN_PROCESO, assertThrows(BusinessException.class,
                () -> EstadosCuestionario.ACTIVO.validarQueAdmiteRespuestas(q)).getMessage());
        assertEquals(NO_EN_PROCESO, assertThrows(BusinessException.class,
                () -> EstadosCuestionario.ACTIVO.completar(q, BigDecimal.TEN, AHORA)).getMessage());
    }

    // ------------------------------------------------------------------ EN_PROCESO

    @Test
    void enProceso_dentroDelTiempoYDeLosAccesos_sigueVigente() {
        Cuestionario q = cuestionario("EN_PROCESO");
        q.setFechaInicio(AHORA.minusMinutes(89));
        q.setCantidadAccesos(2);

        EstadosCuestionario.EN_PROCESO.verificarVigencia(q, CONFIG, AHORA, true);

        assertEquals("EN_PROCESO", q.getEstado());
    }

    @Test
    void enProceso_superaElTiempoMaximo_pasaAIncompleto() {
        Cuestionario q = cuestionario("EN_PROCESO");
        q.setFechaInicio(AHORA.minusMinutes(91));

        BusinessException e = assertThrows(BusinessException.class,
                () -> EstadosCuestionario.EN_PROCESO.verificarVigencia(q, CONFIG, AHORA, false));

        assertTrue(e.getMessage().startsWith("Se superó el tiempo máximo de 90 minutos"));
        assertEquals("INCOMPLETO", q.getEstado());
        assertEquals(AHORA, q.getFechaCambioEstado());
    }

    @Test
    void enProceso_nuevoIngresoConLosAccesosAgotados_pasaAIncompleto() {
        Cuestionario q = cuestionario("EN_PROCESO");
        q.setFechaInicio(null);
        q.setCantidadAccesos(3);

        BusinessException e = assertThrows(BusinessException.class,
                () -> EstadosCuestionario.EN_PROCESO.verificarVigencia(q, CONFIG, AHORA, true));

        assertEquals("Se superó la cantidad máxima de 3 accesos permitidos. El cuestionario pasó al estado Incompleto.", e.getMessage());
        assertEquals("INCOMPLETO", q.getEstado());
    }

    @Test
    void enProceso_respondiendoUnBloque_noCuentaComoNuevoAcceso() {
        Cuestionario q = cuestionario("EN_PROCESO");
        q.setFechaInicio(AHORA.minusMinutes(10));
        q.setCantidadAccesos(3);

        EstadosCuestionario.EN_PROCESO.verificarVigencia(q, CONFIG, AHORA, false);

        assertEquals("EN_PROCESO", q.getEstado());
    }

    @Test
    void enProceso_reingreso_soloCuentaElAcceso() {
        Cuestionario q = cuestionario("EN_PROCESO");
        q.setBloqueActual(2);
        q.setCantidadAccesos(1);

        assertFalse(EstadosCuestionario.EN_PROCESO.requiereSortearPreguntas());
        EstadosCuestionario.EN_PROCESO.registrarIngreso(q, AHORA);
        EstadosCuestionario.EN_PROCESO.validarQueAdmiteRespuestas(q);

        assertEquals("EN_PROCESO", q.getEstado());
        assertEquals(2, q.getBloqueActual());
        assertEquals(2, q.getCantidadAccesos());
        assertEquals(AHORA, q.getFechaUltimoAcceso());
    }

    @Test
    void enProceso_completar_guardaPuntajeYPasaACompleto() {
        Cuestionario q = cuestionario("EN_PROCESO");

        EstadosCuestionario.EN_PROCESO.completar(q, new BigDecimal("4.75"), AHORA);

        assertEquals("COMPLETO", q.getEstado());
        assertEquals(new BigDecimal("4.75"), q.getPuntajeTotal());
        assertEquals(AHORA, q.getFechaFin());
        assertEquals(AHORA, q.getFechaCambioEstado());
    }

    // ------------------------------------------------------------------ estados finales

    @Test
    void estadosFinales_noPermitenIngresarNiResponder() {
        for (EstadoCuestionario estado : List.of(EstadosCuestionario.COMPLETO, EstadosCuestionario.INCOMPLETO,
                EstadosCuestionario.SIN_CONTESTAR)) {
            Cuestionario q = cuestionario(estado.nombre());

            assertFalse(estado.requiereSortearPreguntas());
            assertEquals(NO_DISPONIBLE, assertThrows(BusinessException.class,
                    () -> estado.verificarVigencia(q, CONFIG, AHORA, true)).getMessage());
            assertEquals(NO_DISPONIBLE, assertThrows(BusinessException.class,
                    () -> estado.registrarIngreso(q, AHORA)).getMessage());
            assertEquals(NO_EN_PROCESO, assertThrows(BusinessException.class,
                    () -> estado.validarQueAdmiteRespuestas(q)).getMessage());
            assertEquals(NO_EN_PROCESO, assertThrows(BusinessException.class,
                    () -> estado.completar(q, BigDecimal.ONE, AHORA)).getMessage());
            assertEquals(estado.nombre(), q.getEstado());
        }
    }
}
