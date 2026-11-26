package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.RespuestaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.EvaluacionesPuestoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.FilaOrdenMeritoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.PuestoResumenDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.ReporteOrdenMeritoDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CU028 (orden de mérito) y el cálculo de puntajes por competencia. */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PuestoDao puestoDao;

    @Mock
    private CuestionarioDao cuestionarioDao;

    @Mock
    private RespuestaCuestionarioDao respuestaDao;

    @InjectMocks
    private ReporteService service;

    // ------------------------------------------------------------------ datos de prueba

    private static final LocalDateTime EVALUACION_1 = LocalDateTime.of(2026, 11, 20, 9, 0, 0);
    private static final LocalDateTime EVALUACION_2 = LocalDateTime.of(2026, 11, 22, 15, 30, 0);

    private final Competencia gp = Datos.competencia(1L, "COMP-GP", "GESTION DE PROYECTOS");
    private final Competencia le = Datos.competencia(2L, "COMP-LE", "LEALTAD");
    private final Factor planificacion = Datos.factor(11L, gp, "FAC-GP-01", "PLANIFICACION", 1);
    private final Factor comunicacion = Datos.factor(21L, le, "FAC-LE-01", "COMUNICACION", 1);
    private final OpcionRespuesta escala = Datos.opcion(1L, "ESCALA", "NUNCA", "A VECES", "SIEMPRE");
    private final Puesto pm = Datos.puesto(1L, "PUE-PM-01", "PROJECT MANAGER TIC", "TECNOLOGIA DEL SUR S.A.");
    private final Map<Long, RespuestaCuestionario> respuestas = new HashMap<>();

    ReporteServiceTest() {
        Datos.exige(pm, le, "3");
        Datos.exige(pm, gp, "4");
    }

    /**
     * Cuestionario de un candidato con dos preguntas por competencia. Las ponderaciones obtenidas
     * se dan en orden: dos de GESTION DE PROYECTOS y dos de LEALTAD.
     */
    private Cuestionario cuestionario(long id, String apellido, String estado, LocalDateTime creado, String... ponderaciones) {
        Cuestionario q = Datos.cuestionario(id, Datos.candidato(id, "CAND-00" + id, apellido, "NOMBRE " + apellido), pm, estado);
        q.setFechaCreacion(creado);
        for (int i = 0; i < ponderaciones.length; i++) {
            Factor factor = i < 2 ? planificacion : comunicacion;
            long pqId = id * 100 + i + 1;
            var pq = Datos.item(pqId, q, Datos.pregunta(pqId, factor, escala, "PREGUNTA " + pqId), 1);
            respuestas.put(pqId, Datos.respuesta(pq, ponderaciones[i]));
        }
        return q;
    }

    /** Respuestas "guardadas": el DAO devuelve las que se cargaron al armar cada cuestionario. */
    private void respuestasGuardadas() {
        when(respuestaDao.findByPreguntaCuestionarioId(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(respuestas.get(inv.getArgument(0))));
    }

    private Cuestionario sosa() {
        // GESTION (10 + 1,5) / 2 = 5,75 ; LEALTAD (5 + 2,5) / 2 = 3,75 -> cumple los mínimos 4 y 3
        Cuestionario q = cuestionario(2L, "SOSA", "COMPLETO", EVALUACION_1.plusNanos(123_000_000), "10", "1.5", "5", "2.5");
        q.setPuntajeTotal(new BigDecimal("4.75"));
        q.setFechaFin(EVALUACION_1.plusHours(2));
        return q;
    }

    private Cuestionario gutierrez() {
        // GESTION (10 + 0,5) / 2 = 5,25 ; LEALTAD 2,5 / 2 = 1,25 -> no alcanza el mínimo de 3. Sin puntaje guardado.
        Cuestionario q = cuestionario(3L, "GUTIERREZ", "COMPLETO", EVALUACION_1.plusNanos(456_000_000), "10", "0.5", "2.5", "0");
        q.setFechaFin(EVALUACION_1.plusHours(3));
        return q;
    }

    private Cuestionario benitez() {
        Cuestionario q = cuestionario(8L, "BENITEZ", "COMPLETO", EVALUACION_2, "10", "2", "10", "2");
        q.setPuntajeTotal(new BigDecimal("6.00"));
        return q;
    }

    private List<Cuestionario> todos() {
        return List.of(sosa(), gutierrez(),
                cuestionario(4L, "PEREZ", "EN_PROCESO", EVALUACION_2),
                cuestionario(5L, "DIAZ", "ACTIVO", EVALUACION_2),
                cuestionario(6L, "ROMERO", "SIN_CONTESTAR", EVALUACION_2),
                cuestionario(7L, "ACOSTA", "INCOMPLETO", EVALUACION_2),
                benitez());
    }

    private static List<String> apellidos(List<FilaOrdenMeritoDTO> filas) {
        List<String> r = new ArrayList<>();
        filas.forEach(f -> r.add(f.apellido()));
        return r;
    }

    // ------------------------------------------------------------------ CU028

    @Test
    void buscarPuestos_filtraPorCodigoNombreYEmpresaYCuentaCandidatos() {
        Puesto arquitecto = Datos.puesto(2L, "PUE-SR-02", "ARQUITECTO DE SOFTWARE SR", "FINTECH LITORAL S.R.L.");
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arquitecto, pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(todos());

        List<PuestoResumenDTO> puestos = service.buscarPuestos(" pm ", "project", "sur");

        assertEquals(1, puestos.size());
        assertEquals("PUE-PM-01", puestos.get(0).codigo());
        assertEquals(7, puestos.get(0).candidatos());
        assertEquals(3, puestos.get(0).evaluacionesCompletas());
    }

    @Test
    void buscarPuestos_sinFiltros_devuelveTodosYCuentaCadaCandidatoUnaVez() {
        Puesto arquitecto = Datos.puesto(2L, "PUE-SR-02", "ARQUITECTO DE SOFTWARE SR", "FINTECH LITORAL S.R.L.");
        Cuestionario primero = sosa();
        Cuestionario reevaluado = Datos.cuestionario(9L, primero.getCandidato(), pm, "ACTIVO");
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arquitecto, pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(2L)).thenReturn(List.of());
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(List.of(primero, reevaluado));

        List<PuestoResumenDTO> puestos = service.buscarPuestos(null, null, null);

        assertEquals(2, puestos.size());
        assertEquals(0, puestos.get(0).candidatos());
        assertEquals(1, puestos.get(1).candidatos());
        assertEquals(1, puestos.get(1).evaluacionesCompletas());
    }

    @Test
    void listarEvaluaciones_agrupaPorFechaDeGeneracionEnOrdenCronologico() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(todos());

        EvaluacionesPuestoDTO evaluaciones = service.listarEvaluaciones(1L);

        assertEquals("PROJECT MANAGER TIC", evaluaciones.puestoNombre());
        assertEquals(7, evaluaciones.totalCandidatos());
        assertEquals(3, evaluaciones.totalCompletos());
        assertEquals(2, evaluaciones.evaluaciones().size());
        // SOSA y GUTIERREZ se generaron en el mismo segundo: son la misma evaluación.
        assertEquals("EVA-001", evaluaciones.evaluaciones().get(0).codigo());
        assertEquals(2, evaluaciones.evaluaciones().get(0).candidatos());
        assertEquals(2, evaluaciones.evaluaciones().get(0).completos());
        assertEquals("EVA-002", evaluaciones.evaluaciones().get(1).codigo());
        assertEquals(EVALUACION_2, evaluaciones.evaluaciones().get(1).fechaGeneracion());
        assertEquals(5, evaluaciones.evaluaciones().get(1).candidatos());
        assertEquals(1, evaluaciones.evaluaciones().get(1).completos());
    }

    @Test
    void listarEvaluaciones_puestoSinEvaluacionesOInexistente_informa() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertEquals("El puesto PROJECT MANAGER TIC no posee evaluaciones registradas. No es posible emitir el orden de mérito.",
                assertThrows(BusinessException.class, () -> service.listarEvaluaciones(1L)).getMessage());
        assertEquals("Puesto no encontrado",
                assertThrows(BusinessException.class, () -> service.listarEvaluaciones(5L)).getMessage());
        assertEquals("Debe seleccionar un puesto",
                assertThrows(BusinessException.class, () -> service.listarEvaluaciones(null)).getMessage());
    }

    @Test
    void ordenDeMerito_todasLasEvaluaciones_separaQuienesAlcanzanLosMinimos() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(todos());
        respuestasGuardadas();

        ReporteOrdenMeritoDTO reporte = service.emitirOrdenDeMerito(1L, null, "  MARIANA GOMEZ ");

        assertEquals("TODAS", reporte.evaluacion());
        assertEquals("MARIANA GOMEZ", reporte.emitidoPor());
        assertNotNull(reporte.fechaEmision());
        assertEquals(List.of("BENITEZ", "SOSA"), apellidos(reporte.ordenMerito()));
        assertEquals(1, reporte.ordenMerito().get(0).posicion());
        assertEquals(2, reporte.ordenMerito().get(1).posicion());
        assertEquals(new BigDecimal("4.75"), reporte.ordenMerito().get(1).puntaje());
        // GUTIERREZ completó pero no llegó al mínimo de LEALTAD; su puntaje se calcula: (5,25 + 1,25) / 2.
        assertEquals(List.of("GUTIERREZ"), apellidos(reporte.noAlcanzanMinimo()));
        assertEquals(new BigDecimal("3.25"), reporte.noAlcanzanMinimo().get(0).puntaje());
        // El resto, agrupado por estado: En proceso, Activo, Incompleto, Sin contestar.
        assertEquals(List.of("PEREZ", "DIAZ", "ACOSTA", "ROMERO"), apellidos(reporte.restantes()));
        assertEquals("GESTION DE PROYECTOS", reporte.competencias().get(0).nombre());
        assertEquals(new BigDecimal("3"), reporte.competencias().get(1).ponderacionMinima());
    }

    @Test
    void ordenDeMerito_unaEvaluacion_soloIncluyeSusCuestionarios() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(todos());
        respuestasGuardadas();

        ReporteOrdenMeritoDTO reporte = service.emitirOrdenDeMerito(1L, " eva-001 ", " ");

        assertEquals("EVA-001", reporte.evaluacion());
        assertEquals("-", reporte.emitidoPor());
        assertEquals(List.of("SOSA"), apellidos(reporte.ordenMerito()));
        assertEquals(List.of("GUTIERREZ"), apellidos(reporte.noAlcanzanMinimo()));
        assertTrue(reporte.restantes().isEmpty());
    }

    @Test
    void ordenDeMerito_completoSinRespuestas_quedaConPuntajeCeroYNoAlcanzaLosMinimos() {
        Cuestionario vacio = cuestionario(9L, "VACIO", "COMPLETO", EVALUACION_1);
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(List.of(vacio));

        ReporteOrdenMeritoDTO reporte = service.emitirOrdenDeMerito(1L, "TODAS", null);

        assertTrue(reporte.ordenMerito().isEmpty());
        assertEquals(new BigDecimal("0.00"), reporte.noAlcanzanMinimo().get(0).puntaje());
        verifyNoInteractions(respuestaDao);
    }

    @Test
    void ordenDeMerito_evaluacionInexistenteOPuestoSinEvaluaciones_informa() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(todos()).thenReturn(List.of());

        assertEquals("La evaluación EVA-009 no existe para el puesto seleccionado",
                assertThrows(BusinessException.class, () -> service.emitirOrdenDeMerito(1L, "EVA-009", "X")).getMessage());
        assertTrue(assertThrows(BusinessException.class, () -> service.emitirOrdenDeMerito(1L, null, "X"))
                .getMessage().contains("no posee evaluaciones registradas"));
    }

    @Test
    void puntajesPorCompetencia_promedioDeFactoresDividido2() {
        Cuestionario q = sosa();
        respuestasGuardadas();

        Map<Long, BigDecimal> puntajes = service.puntajesPorCompetencia(q);

        assertEquals(new BigDecimal("5.75"), puntajes.get(1L));
        assertEquals(new BigDecimal("3.75"), puntajes.get(2L));
    }
}
