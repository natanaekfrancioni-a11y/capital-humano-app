package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CandidatoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.ConfiguracionCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.RespuestaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.CandidatoClaveDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.CompetenciaPuestoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.RespuestaItemInput;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluacionCuestionarioServiceTest {

    private static final List<String> VIGENTES = List.of("ACTIVO", "EN_PROCESO");

    @Mock
    private CandidatoDao candidatoDao;

    @Mock
    private PuestoDao puestoDao;

    @Mock
    private FactorDao factorDao;

    @Mock
    private PreguntaDao preguntaDao;

    @Mock
    private CuestionarioDao cuestionarioDao;

    @Mock
    private PreguntaCuestionarioDao preguntaCuestionarioDao;

    @Mock
    private RespuestaCuestionarioDao respuestaCuestionarioDao;

    @Mock
    private ConfiguracionCuestionarioDao configuracionDao;

    @InjectMocks
    private EvaluacionCuestionarioService service;

    @Test
    void deberiaInstanciarServicioConMocks() {
        assertNotNull(service);
    }

    @Test
    void deberiaObtenerConfiguracionDesdeDao() {
        ConfiguracionCuestionario configMock = new ConfiguracionCuestionario();
        when(configuracionDao.findAll()).thenReturn(List.of(configMock));

        ConfiguracionCuestionario resultado = service.obtenerConfiguracion();

        assertNotNull(resultado);
        verify(configuracionDao).findAll();
    }

    @Test
    void deberiaBuscarCandidatosDelegandoAlDao() {
        List<Candidato> esperados = List.of(new Candidato());
        when(candidatoDao.findAll()).thenReturn(esperados);

        List<Candidato> resultado = service.buscarCandidatos(null, null, null);

        assertNotNull(resultado);
        assertEquals(esperados.size(), resultado.size());
    }

    // ------------------------------------------------------------------ datos de prueba

    private final Competencia gp = Datos.competencia(1L, "COMP-GP", "GESTION DE PROYECTOS");
    private final Competencia le = Datos.competencia(2L, "COMP-LE", "LEALTAD");
    private final Factor planificacion = Datos.factor(11L, gp, "FAC-GP-01", "PLANIFICACION", 1);
    private final Factor riesgos = Datos.factor(12L, gp, "FAC-GP-02", "RIESGOS", 2);
    private final Factor comunicacion = Datos.factor(21L, le, "FAC-LE-01", "COMUNICACION", 1);
    private final OpcionRespuesta vf = Datos.opcion(1L, "VERDADERO-FALSO", "VERDADERO", "FALSO");

    private Pregunta pregunta(long id, Factor factor) {
        return Datos.pregunta(id, factor, vf, "PREGUNTA " + id, "10", "0");
    }

    private Puesto puestoProjectManager() {
        Puesto pm = Datos.puesto(1L, "PUE-PM-01", "PROJECT MANAGER TIC", "TECNOLOGIA DEL SUR S.A.");
        Datos.exige(pm, le, "3");
        Datos.exige(pm, gp, "4");
        return pm;
    }

    /** Deja las dos competencias del puesto en condiciones de ser evaluadas (un factor con 2 preguntas cada una). */
    private void competenciasEvaluables() {
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(2L)).thenReturn(List.of(comunicacion));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L)).thenReturn(List.of(pregunta(1, planificacion), pregunta(2, planificacion)));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(21L)).thenReturn(List.of(pregunta(3, comunicacion), pregunta(4, comunicacion)));
    }

    private static boolean esClaveValida(String clave) {
        return clave.matches("[A-Z0-9]{8}") && clave.matches(".*[A-Z].*") && clave.matches(".*[0-9].*");
    }

    // ------------------------------------------------------------------ CU025: búsqueda y validaciones

    @Test
    void buscarCandidatos_filtraPorApellidoNombreYNroSinDistinguirMayusculas() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        Candidato gutierrez = Datos.candidato(3L, "CAND-003", "GUTIERREZ", "TOMAS ARIEL");
        Candidato eliminado = Datos.candidato(4L, "CAND-004", "SOSA", "MARTA");
        eliminado.setEliminado(true);
        when(candidatoDao.findAll()).thenReturn(List.of(sosa, gutierrez, eliminado));

        assertEquals(List.of(sosa), service.buscarCandidatos(" sosa ", null, null));
        assertEquals(List.of(gutierrez), service.buscarCandidatos(null, "tomas", "003"));
        assertEquals(List.of(), service.buscarCandidatos("PEREZ", "", ""));
    }

    @Test
    void validarCuestionariosActivos_devuelveLosCandidatosConCuestionarioVigente() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        when(cuestionarioDao.findByCandidatoIdAndEstadoIn(2L, VIGENTES))
                .thenReturn(List.of(Datos.cuestionario(9L, sosa, null, "EN_PROCESO")));
        when(cuestionarioDao.findByCandidatoIdAndEstadoIn(3L, VIGENTES)).thenReturn(List.of());
        when(candidatoDao.findById(2L)).thenReturn(Optional.of(sosa));

        // El 2 repetido se consulta una sola vez.
        List<Candidato> conCuestionario = service.validarCuestionariosActivos(List.of(2L, 3L, 2L));

        assertEquals(List.of(sosa), conCuestionario);
        verify(cuestionarioDao, times(1)).findByCandidatoIdAndEstadoIn(2L, VIGENTES);
    }

    @Test
    void validarCuestionariosActivos_sinCandidatos_informa() {
        assertEquals("Se debe seleccionar uno o más candidatos",
                assertThrows(BusinessException.class, () -> service.validarCuestionariosActivos(List.of())).getMessage());
        assertThrows(BusinessException.class, () -> service.validarCuestionariosActivos(null));
    }

    @Test
    void obtenerCompetenciasPuesto_ordenaPorNombreEIndicaSiSonEvaluables() {
        Puesto pm = puestoProjectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(2L)).thenReturn(List.of(comunicacion));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L)).thenReturn(List.of(pregunta(1, planificacion), pregunta(2, planificacion)));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(21L)).thenReturn(List.of(pregunta(3, comunicacion)));

        List<CompetenciaPuestoDTO> competencias = service.obtenerCompetenciasPuesto(1L);

        assertEquals(2, competencias.size());
        assertEquals("GESTION DE PROYECTOS", competencias.get(0).nombre());
        assertEquals(new BigDecimal("4"), competencias.get(0).ponderacionMinima());
        assertTrue(competencias.get(0).evaluable());
        assertEquals("LEALTAD", competencias.get(1).nombre());
        assertFalse(competencias.get(1).evaluable(), "un factor con una sola pregunta no alcanza");
    }

    @Test
    void obtenerCompetenciasPuesto_puestoInexistenteOInactivo_informa() {
        Puesto inactivo = puestoProjectManager();
        inactivo.setActivo(false);
        when(puestoDao.findById(1L)).thenReturn(Optional.of(inactivo));

        assertEquals("Puesto no encontrado",
                assertThrows(BusinessException.class, () -> service.obtenerCompetenciasPuesto(1L)).getMessage());
        assertEquals("Se debe seleccionar una función o puesto para realizar la evaluación.",
                assertThrows(BusinessException.class, () -> service.obtenerCompetenciasPuesto(null)).getMessage());
    }

    // ------------------------------------------------------------------ CU025: claves y cuestionarios

    @Test
    void generarClaves_unaClaveAlfanumericaDistintaPorCandidato() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        Candidato gutierrez = Datos.candidato(3L, "CAND-003", "GUTIERREZ", "TOMAS ARIEL");
        when(puestoDao.findById(1L)).thenReturn(Optional.of(puestoProjectManager()));
        competenciasEvaluables();
        when(candidatoDao.findById(2L)).thenReturn(Optional.of(sosa));
        when(candidatoDao.findById(3L)).thenReturn(Optional.of(gutierrez));

        List<CandidatoClaveDTO> claves = service.generarClaves(List.of(2L, 3L), 1L);

        assertEquals(2, claves.size());
        assertEquals("SOSA", claves.get(0).apellido());
        assertEquals("CAND-003", claves.get(1).nroCandidato());
        Set<String> distintas = new HashSet<>();
        for (CandidatoClaveDTO c : claves) {
            assertTrue(esClaveValida(c.claveIngreso()), "clave inválida: " + c.claveIngreso());
            distintas.add(c.claveIngreso());
        }
        assertEquals(2, distintas.size());
        verify(cuestionarioDao, never()).save(any());
    }

    @Test
    void generarClaves_candidatoConCuestionarioVigente_listaLosBloqueados() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        when(cuestionarioDao.findByCandidatoIdAndEstadoIn(2L, VIGENTES))
                .thenReturn(List.of(Datos.cuestionario(9L, sosa, null, "ACTIVO")));
        when(candidatoDao.findById(2L)).thenReturn(Optional.of(sosa));

        BusinessException e = assertThrows(BusinessException.class, () -> service.generarClaves(List.of(2L), 1L));

        assertEquals("Los siguientes candidatos poseen cuestionarios activos o en proceso y no pueden ser evaluados: "
                + "SOSA, VALERIA SOFIA.", e.getMessage());
        assertEquals(List.of("SOSA, VALERIA SOFIA"), e.getDetalles());
        verifyNoInteractions(puestoDao);
    }

    @Test
    void generarClaves_competenciaSinPreguntasSuficientes_noPermiteEvaluar() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(puestoProjectManager()));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(2L)).thenReturn(List.of(comunicacion));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(21L)).thenReturn(List.of(pregunta(3, comunicacion)));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class, () -> service.generarClaves(List.of(2L), 1L));

        assertEquals("Las siguientes competencias no poseen factores que cumplan las condiciones para ser evaluadas: "
                + "LEALTAD, GESTION DE PROYECTOS.", e.getMessage());
        assertEquals(List.of("LEALTAD", "GESTION DE PROYECTOS"), e.getDetalles());
    }

    @Test
    void generarClaves_seleccionIncompleta_informa() {
        assertEquals("Se debe seleccionar uno o más candidatos",
                assertThrows(BusinessException.class, () -> service.generarClaves(null, 1L)).getMessage());
        assertEquals("Se debe seleccionar una función o puesto para realizar la evaluación.",
                assertThrows(BusinessException.class, () -> service.generarClaves(List.of(2L), null)).getMessage());
        verifyNoInteractions(cuestionarioDao, puestoDao);
    }

    @Test
    void generarClaves_candidatoInexistente_informa() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(puestoProjectManager()));
        competenciasEvaluables();

        BusinessException e = assertThrows(BusinessException.class, () -> service.generarClaves(List.of(9L), 1L));

        assertEquals("Candidato no encontrado: 9", e.getMessage());
    }

    @Test
    void generarEvaluaciones_creaUnCuestionarioActivoPorCandidatoConLasClavesMostradas() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        Candidato gutierrez = Datos.candidato(3L, "CAND-003", "GUTIERREZ", "TOMAS ARIEL");
        Puesto pm = puestoProjectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        competenciasEvaluables();
        when(candidatoDao.findById(2L)).thenReturn(Optional.of(sosa));
        when(candidatoDao.findById(3L)).thenReturn(Optional.of(gutierrez));
        when(cuestionarioDao.save(any(Cuestionario.class))).thenAnswer(inv -> inv.getArgument(0));

        // La clave de SOSA es la del paso 11 (en minúsculas); la de GUTIERREZ no es válida y se genera otra.
        Map<Long, String> claves = Map.of(2L, " ab12cd34 ", 3L, "ABCDEFGH");
        List<Cuestionario> creados = service.generarEvaluaciones(List.of(2L, 3L), 1L, claves);

        assertEquals(2, creados.size());
        Cuestionario primero = creados.get(0);
        assertSame(sosa, primero.getCandidato());
        assertSame(pm, primero.getPuesto());
        assertEquals("AB12CD34", primero.getClaveAcceso());
        assertEquals("ACTIVO", primero.getEstado());
        assertEquals(1, primero.getBloqueActual());
        assertEquals(0, primero.getCantidadAccesos());
        assertTrue(esClaveValida(creados.get(1).getClaveAcceso()));
        assertEquals(primero.getFechaCreacion(), creados.get(1).getFechaCreacion(), "una evaluación comparte la fecha de creación");
    }

    @Test
    void generarEvaluaciones_sinClavesRecibidas_lasGenera() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        when(puestoDao.findById(1L)).thenReturn(Optional.of(puestoProjectManager()));
        competenciasEvaluables();
        when(candidatoDao.findById(2L)).thenReturn(Optional.of(sosa));
        when(cuestionarioDao.save(any(Cuestionario.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Cuestionario> creados = service.generarEvaluaciones(List.of(2L), 1L);

        assertEquals(1, creados.size());
        assertTrue(esClaveValida(creados.get(0).getClaveAcceso()));
    }

    // ------------------------------------------------------------------ CU026: inicio y sorteo

    @Test
    void iniciar_cuestionarioActivo_sorteaDosPreguntasPorFactorYArmaLosBloques() {
        Puesto pm = Datos.puesto(1L, "PUE-PM-01", "PROJECT MANAGER TIC", "TECNOLOGIA DEL SUR S.A.");
        Datos.exige(pm, gp, "4");
        Cuestionario q = Datos.cuestionario(10L, Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA"), pm, "ACTIVO");
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));
        when(configuracionDao.findAll()).thenReturn(List.of(Datos.configuracion(1, 5, 90, 30)));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion, riesgos));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L))
                .thenReturn(List.of(pregunta(1, planificacion), pregunta(2, planificacion), pregunta(3, planificacion)));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(12L)).thenReturn(List.of(pregunta(4, riesgos)));
        when(cuestionarioDao.save(q)).thenReturn(q);

        Cuestionario iniciado = service.iniciarOSortearCuestionario(10L);

        assertSame(q, iniciado);
        assertEquals("EN_PROCESO", q.getEstado());
        assertEquals(1, q.getCantidadAccesos());
        assertNotNull(q.getFechaInicio());
        // RIESGOS tiene una sola pregunta: no se sortea. Con 1 pregunta por bloque quedan 2 bloques.
        List<PreguntaCuestionario> sorteadas = q.getPreguntas();
        assertEquals(2, sorteadas.size());
        for (int i = 0; i < sorteadas.size(); i++) {
            PreguntaCuestionario pq = sorteadas.get(i);
            assertSame(planificacion, pq.getFactor());
            assertSame(gp, pq.getCompetencia());
            assertEquals(i + 1, pq.getOrdenVisual());
            assertEquals(i + 1, pq.getNroBloque());
            assertFalse(pq.isRespondida());
        }
        assertFalse(sorteadas.get(0).getPregunta() == sorteadas.get(1).getPregunta(), "no se repiten preguntas");
    }

    @Test
    void iniciar_cuestionarioEnProceso_noVuelveASortearYSoloCuentaElAcceso() {
        Cuestionario q = Datos.cuestionario(10L, null, puestoProjectManager(), "EN_PROCESO");
        Datos.item(1L, q, pregunta(1, planificacion), 1);
        q.setCantidadAccesos(1);
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));

        service.iniciarOSortearCuestionario(10L);

        assertEquals("EN_PROCESO", q.getEstado());
        assertEquals(2, q.getCantidadAccesos());
        assertEquals(1, q.getPreguntas().size());
        verifyNoInteractions(factorDao, preguntaDao);
        verify(cuestionarioDao).save(q);
    }

    @Test
    void iniciar_cuestionarioInexistenteOFinalizado_informa() {
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(Datos.cuestionario(10L, null, null, "COMPLETO")));

        assertEquals("El cuestionario no se encuentra disponible para ser completado.",
                assertThrows(BusinessException.class, () -> service.iniciarOSortearCuestionario(10L)).getMessage());
        assertEquals("Cuestionario no encontrado",
                assertThrows(BusinessException.class, () -> service.iniciarOSortearCuestionario(11L)).getMessage());
        verify(cuestionarioDao, never()).save(any());
    }

    // ------------------------------------------------------------------ CU026: responder bloques

    /** Cuestionario en proceso con 3 preguntas: dos de GESTION (bloques 1 y 2) y una de LEALTAD (bloque 2). */
    private Cuestionario cuestionarioEnProceso() {
        Cuestionario q = Datos.cuestionario(10L, null, puestoProjectManager(), "EN_PROCESO");
        Datos.item(1L, q, pregunta(1, planificacion), 1);
        Datos.item(2L, q, Datos.pregunta(2, planificacion, vf, "PREGUNTA 2", "5", "0"), 2);
        Datos.item(3L, q, pregunta(3, comunicacion), 2);
        return q;
    }

    @Test
    void responderBloque_noUltimo_guardaLasRespuestasYAvanzaAlSiguiente() {
        Cuestionario q = cuestionarioEnProceso();
        PreguntaCuestionario pq1 = q.getPreguntas().get(0);
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));
        when(preguntaCuestionarioDao.findByCuestionarioIdAndNroBloqueOrderByOrdenVisualAsc(10L, 1)).thenReturn(List.of(pq1));
        when(cuestionarioDao.save(q)).thenReturn(q);

        Cuestionario resultado = service.responderBloque(10L, 1, List.of(new RespuestaItemInput(1L, 11L), new RespuestaItemInput(null, 12L)));

        ArgumentCaptor<RespuestaCuestionario> guardada = ArgumentCaptor.forClass(RespuestaCuestionario.class);
        verify(respuestaCuestionarioDao).save(guardada.capture());
        assertSame(pq1, guardada.getValue().getPreguntaCuestionario());
        assertEquals(11L, guardada.getValue().getDetalleOpcionRespuesta().getId());
        assertEquals(new BigDecimal("10"), guardada.getValue().getPonderacionObtenida());
        assertTrue(pq1.isRespondida());
        verify(preguntaCuestionarioDao).save(pq1);
        assertSame(q, resultado);
        assertEquals(2, q.getBloqueActual());
        assertNotNull(q.getFechaUltimoAcceso());
        assertEquals("EN_PROCESO", q.getEstado());
    }

    @Test
    void responderBloque_ultimo_calculaElPuntajeYCompletaElCuestionario() {
        Cuestionario q = cuestionarioEnProceso();
        q.setBloqueActual(2);
        List<PreguntaCuestionario> bloque2 = List.of(q.getPreguntas().get(1), q.getPreguntas().get(2));
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));
        when(preguntaCuestionarioDao.findByCuestionarioIdAndNroBloqueOrderByOrdenVisualAsc(10L, 2)).thenReturn(bloque2);
        // Un "repositorio en memoria": lo que se guarda se puede volver a leer al calcular el puntaje.
        Map<Long, RespuestaCuestionario> guardadas = new HashMap<>();
        guardadas.put(1L, Datos.respuesta(q.getPreguntas().get(0), "10"));
        when(respuestaCuestionarioDao.findByPreguntaCuestionarioId(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(guardadas.get(inv.getArgument(0))));
        when(respuestaCuestionarioDao.save(any(RespuestaCuestionario.class))).thenAnswer(inv -> {
            RespuestaCuestionario r = inv.getArgument(0);
            guardadas.put(r.getPreguntaCuestionario().getId(), r);
            return r;
        });
        when(cuestionarioDao.save(q)).thenReturn(q);

        // Pregunta 2: VERDADERO vale 5. Pregunta 3: FALSO vale 0.
        service.responderBloque(10L, 2, List.of(new RespuestaItemInput(2L, 11L), new RespuestaItemInput(3L, 12L)));

        // PLANIFICACION: (10 + 5) / 2 = 7,5 -> GESTION 7,5. COMUNICACION: 0 / 2 = 0 -> LEALTAD 0. Total: 3,75.
        assertEquals("COMPLETO", q.getEstado());
        assertEquals(new BigDecimal("3.75"), q.getPuntajeTotal());
        assertNotNull(q.getFechaFin());
        assertEquals(2, q.getBloqueActual());
    }

    @Test
    void responderBloque_cuestionarioNoEnProceso_noAdmiteRespuestas() {
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(Datos.cuestionario(10L, null, null, "ACTIVO")));

        BusinessException e = assertThrows(BusinessException.class, () -> service.responderBloque(10L, 1, List.of()));

        assertEquals("El cuestionario no se encuentra en proceso", e.getMessage());
        verifyNoInteractions(preguntaCuestionarioDao, respuestaCuestionarioDao);
    }

    @Test
    void responderBloque_otroBloqueQueElActual_noSePermite() {
        Cuestionario q = cuestionarioEnProceso();
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));

        BusinessException e = assertThrows(BusinessException.class, () -> service.responderBloque(10L, 2, List.of()));

        assertEquals("Solo puede responder el bloque activo actual (1)", e.getMessage());
    }

    @Test
    void responderBloque_faltanRespuestas_noGuardaNada() {
        Cuestionario q = cuestionarioEnProceso();
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));
        when(preguntaCuestionarioDao.findByCuestionarioIdAndNroBloqueOrderByOrdenVisualAsc(10L, 1))
                .thenReturn(List.of(q.getPreguntas().get(0)));

        BusinessException e = assertThrows(BusinessException.class, () -> service.responderBloque(10L, 1, null));

        assertEquals("Existen preguntas sin responder en el bloque actual", e.getMessage());
        verifyNoInteractions(respuestaCuestionarioDao);
    }

    @Test
    void responderBloque_opcionQueNoEsDeLaPregunta_informa() {
        Cuestionario q = cuestionarioEnProceso();
        when(cuestionarioDao.findById(10L)).thenReturn(Optional.of(q));
        when(preguntaCuestionarioDao.findByCuestionarioIdAndNroBloqueOrderByOrdenVisualAsc(10L, 1))
                .thenReturn(List.of(q.getPreguntas().get(0)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.responderBloque(10L, 1, List.of(new RespuestaItemInput(1L, 99L))));

        assertEquals("Opcion de respuesta invalida para la pregunta 1", e.getMessage());
    }

    @Test
    void responderBloque_cuestionarioInexistente_informa() {
        assertEquals("Cuestionario no encontrado",
                assertThrows(BusinessException.class, () -> service.responderBloque(10L, 1, List.of())).getMessage());
    }

    // ------------------------------------------------------------------ cálculo y configuración

    @Test
    void calcularPuntajeTotal_sinPreguntas_esCero() {
        assertEquals(new BigDecimal("0.00"), service.calcularPuntajeTotal(Datos.cuestionario(1L, null, null, "EN_PROCESO")));
    }

    @Test
    void calcularPuntajeTotal_preguntaSinRespuesta_sumaCero() {
        Cuestionario q = Datos.cuestionario(1L, null, null, "EN_PROCESO");
        Datos.item(1L, q, pregunta(1, planificacion), 1);
        Datos.item(2L, q, pregunta(2, planificacion), 1);
        when(respuestaCuestionarioDao.findByPreguntaCuestionarioId(1L)).thenReturn(Optional.of(Datos.respuesta(q.getPreguntas().get(0), "10")));
        when(respuestaCuestionarioDao.findByPreguntaCuestionarioId(2L)).thenReturn(Optional.empty());

        // (10 + 0) / 2 = 5
        assertEquals(new BigDecimal("5.00"), service.calcularPuntajeTotal(q));
    }

    @Test
    void obtenerConfiguracion_sinFilaCargada_usaLosValoresPorDefecto() {
        ConfiguracionCuestionario cfg = service.obtenerConfiguracion();

        assertEquals(2, cfg.getPreguntasPorBloque());
        assertEquals(5, cfg.getMaxAccesos());
        assertEquals(90, cfg.getTiempoMaxMinutos());
        assertEquals(30, cfg.getDiasVencimientoActivo());
        assertNull(cfg.getId());
    }

    @Test
    void calcularPuntajeTotal_promediaLosFactoresDeCadaCompetencia() {
        Cuestionario q = Datos.cuestionario(1L, null, null, "COMPLETO");
        Datos.item(1L, q, pregunta(1, planificacion), 1);
        Datos.item(2L, q, pregunta(2, riesgos), 1);
        when(respuestaCuestionarioDao.findByPreguntaCuestionarioId(1L)).thenReturn(Optional.of(Datos.respuesta(q.getPreguntas().get(0), "8")));
        when(respuestaCuestionarioDao.findByPreguntaCuestionarioId(2L)).thenReturn(Optional.of(Datos.respuesta(q.getPreguntas().get(1), "3")));

        // PLANIFICACION 8 / 2 = 4; RIESGOS 3 / 2 = 1,5; GESTION = (4 + 1,5) / 2 = 2,75
        assertEquals(new BigDecimal("2.75"), service.calcularPuntajeTotal(q));
    }
}
