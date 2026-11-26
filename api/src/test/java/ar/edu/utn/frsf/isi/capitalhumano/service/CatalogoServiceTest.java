package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CompetenciaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.OpcionRespuestaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.AuditoriaEliminacion;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Preguntas (CU020 a CU023) y las búsquedas de competencias, factores, opciones de respuesta y puestos
 * que usan el alta de pregunta y el CU025. La baja pasa por la clase del patrón Template Method.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private CompetenciaDao competenciaDao;

    @Mock
    private FactorDao factorDao;

    @Mock
    private OpcionRespuestaDao opcionRespuestaDao;

    @Mock
    private PreguntaDao preguntaDao;

    @Mock
    private PuestoDao puestoDao;

    @Mock
    private AuditoriaEliminacionDao auditoriaDao;

    @InjectMocks
    private CatalogoService service;

    // ------------------------------------------------------------------ datos de prueba

    private final Competencia gp = Datos.competencia(1L, "COMP-GP", "GESTION DE PROYECTOS");
    private final Competencia le = Datos.competencia(2L, "COMP-LE", "LEALTAD");
    private final Factor planificacion = Datos.factor(11L, gp, "FAC-GP-01", "PLANIFICACION", 1);
    private final Factor riesgos = Datos.factor(12L, gp, "FAC-GP-02", "RIESGOS", 2);
    private final Factor comunicacion = Datos.factor(21L, le, "FAC-LE-01", "COMUNICACION", 1);
    private final OpcionRespuesta vf = Datos.opcion(1L, "VERDADERO-FALSO", "VERDADERO", "FALSO");
    private final OpcionRespuesta frecuencia = Datos.opcion(2L, "FRECUENCIA-3", "NUNCA", "A VECES", "SIEMPRE");
    private final Pregunta p1 = Datos.pregunta(1L, planificacion, vf, "PREGUNTA 1", "10", "0");
    private final Pregunta p2 = Datos.pregunta(2L, planificacion, vf, "PREGUNTA 2", "0", "10");
    private final Pregunta p3 = Datos.pregunta(3L, comunicacion, vf, "PREGUNTA 3", "10", "0");

    private Puesto projectManager() {
        Puesto pm = Datos.puesto(1L, "PUE-PM-01", "PROJECT MANAGER TIC", "TECNOLOGIA DEL SUR S.A.");
        Datos.exige(pm, gp, "4");
        Datos.exige(pm, le, "3");
        return pm;
    }

    private Puesto arquitecto() {
        Puesto arq = Datos.puesto(2L, "PUE-SR-02", "ARQUITECTO DE SOFTWARE SR", "FINTECH LITORAL S.R.L.");
        Datos.exige(arq, gp, "5");
        return arq;
    }

    private static BusinessException error(org.junit.jupiter.api.function.Executable accion) {
        return assertThrows(BusinessException.class, accion);
    }

    private AuditoriaEliminacion auditoriaRegistrada() {
        ArgumentCaptor<AuditoriaEliminacion> auditoria = ArgumentCaptor.forClass(AuditoriaEliminacion.class);
        verify(auditoriaDao).save(auditoria.capture());
        return auditoria.getValue();
    }

    private static Map<Long, BigDecimal> ponderaciones(Object... idValor) {
        Map<Long, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 0; i < idValor.length; i += 2) {
            m.put((Long) idValor[i], idValor[i + 1] == null ? null : new BigDecimal((String) idValor[i + 1]));
        }
        return m;
    }

    // ================================================================== COMPETENCIAS

    @Test
    void buscarCompetencias_sinNombreTraeLasActivas_conNombreFiltra() {
        when(competenciaDao.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(gp, le));
        when(competenciaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc("lea")).thenReturn(List.of(le));

        assertEquals(List.of(gp, le), service.buscarCompetencias(null));
        assertEquals(List.of(gp, le), service.buscarCompetencias("  "));
        assertEquals(List.of(le), service.buscarCompetencias(" lea "));
    }

    // ================================================================== FACTORES

    @Test
    void buscarFactores_deUnaCompetencia_filtraPorNombreYOrdena() {
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(riesgos, planificacion));

        assertEquals(List.of(planificacion, riesgos), service.buscarFactores(1L, null));
        assertEquals(List.of(planificacion), service.buscarFactores(1L, " plan "));
    }

    @Test
    void buscarFactores_sinCompetencia_buscaEntreTodosLosActivos() {
        when(factorDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(comunicacion, planificacion));

        assertEquals(List.of(comunicacion, planificacion), service.buscarFactores(null, ""));
    }

    // ================================================================== OPCIONES DE RESPUESTA

    @Test
    void buscarOpcionesRespuesta_sinNombreTraeLasActivas_conNombreFiltra() {
        when(opcionRespuestaDao.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(frecuencia, vf));
        when(opcionRespuestaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc("ver")).thenReturn(List.of(vf));

        assertEquals(List.of(frecuencia, vf), service.buscarOpcionesRespuesta(null));
        assertEquals(List.of(vf), service.buscarOpcionesRespuesta(" ver "));
    }

    private static String opciones(OpcionRespuesta op) {
        return op.getDetalles().stream().map(d -> d.getNroOrden() + ":" + d.getDescripcion()).collect(Collectors.joining(","));
    }

    // ================================================================== PREGUNTAS

    @Test
    void buscarPreguntas_filtraPorFactorCompetenciaYNombre() {
        when(preguntaDao.findByActivaTrueOrderByIdAsc()).thenReturn(List.of(p1, p2, p3));

        assertEquals(List.of(p1, p2), service.buscarPreguntas(null, 11L, null));
        assertEquals(List.of(p3), service.buscarPreguntas(2L, null, ""));
        assertEquals(List.of(p2), service.buscarPreguntas(1L, 11L, " 2 "));
        assertEquals(List.of(), service.buscarPreguntas(null, 12L, null));
    }

    @Test
    void crearPregunta_conCodigoYPonderaciones_laGuardaEnMayusculas() {
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(preguntaDao.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));

        Pregunta p = service.crearPregunta(11L, 1L, " preg-x ", " ruta critica ", " ¿qué es la ruta crítica? ", " conceptos ",
                ponderaciones(11L, "10", 12L, null));

        assertEquals("PREG-X", p.getCodigo());
        assertEquals("RUTA CRITICA", p.getNombre());
        assertEquals("¿QUÉ ES LA RUTA CRÍTICA?", p.getEnunciado());
        assertEquals("CONCEPTOS", p.getDescripcion());
        assertSame(planificacion, p.getFactor());
        assertSame(vf, p.getOpcionRespuesta());
        assertTrue(p.isActiva());
        assertEquals(ponderaciones(11L, "10", 12L, "0"), p.getPonderacionesPorDetalle(), "una ponderación vacía cuenta como 0");
    }

    @Test
    void crearPregunta_sinCodigoNiPonderaciones_generaElCodigoYRepartePartesIguales() {
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findById(2L)).thenReturn(Optional.of(frecuencia));
        when(preguntaDao.countByFactorId(11L)).thenReturn(2L);
        when(preguntaDao.findByCodigoIgnoreCase("PREG-GP-01-03")).thenReturn(Optional.of(p3));
        when(preguntaDao.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));

        Pregunta p = service.crearPregunta(11L, 2L, null, "FRECUENCIA DE REVISION", "¿CON QUÉ FRECUENCIA REVISA EL PLAN?", null, null);

        // El 03 ya estaba tomado: se usa el siguiente libre.
        assertEquals("PREG-GP-01-04", p.getCodigo());
        assertNull(p.getDescripcion());
        assertEquals(new BigDecimal("3.33"), p.getPonderacionesPorDetalle().get(21L));
        assertEquals(new BigDecimal("3.33"), p.getPonderacionesPorDetalle().get(22L));
        assertEquals(new BigDecimal("3.34"), p.getPonderacionesPorDetalle().get(23L), "la última opción completa los 10 puntos");
    }

    @Test
    void crearPregunta_factorConCodigoLargo_elCodigoGeneradoNoSuperaLos30Caracteres() {
        Factor largo = Datos.factor(13L, gp, "FAC-" + "X".repeat(30), "LARGO", 3);
        when(factorDao.findById(13L)).thenReturn(Optional.of(largo));
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(preguntaDao.save(any(Pregunta.class))).thenAnswer(inv -> inv.getArgument(0));

        Pregunta p = service.crearPregunta(13L, 1L, "", "N", "E", null, ponderaciones(11L, "5", 12L, "5"));

        assertEquals("PREG-" + "X".repeat(21) + "-01", p.getCodigo());
        assertTrue(p.getCodigo().length() <= 30);
    }

    @Test
    void crearPregunta_validaciones_listaTodosLosErrores() {
        BusinessException e = error(() -> service.crearPregunta(null, null, "C".repeat(31), " ", "", "D".repeat(256), null));

        assertEquals("Validaciones no superadas al crear pregunta", e.getMessage());
        assertEquals(List.of("El factor es obligatorio", "La opción de respuesta es obligatoria", "El nombre de pregunta es obligatorio",
                "El enunciado de pregunta es obligatorio", "El código excede la longitud máxima de 30 caracteres",
                "La descripción excede la longitud máxima de 255 caracteres"), e.getDetalles());
        assertEquals(List.of("El nombre excede la longitud máxima de 120 caracteres"),
                error(() -> service.crearPregunta(11L, 1L, null, "N".repeat(121), "E", null, null)).getDetalles());
        verifyNoInteractions(factorDao, preguntaDao);
    }

    @Test
    void crearPregunta_factorUOpcionInexistente_informa() {
        assertEquals("Factor no encontrado", error(() -> service.crearPregunta(99L, 1L, null, "N", "E", null, null)).getMessage());

        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        assertEquals("Opción de respuesta no encontrada", error(() -> service.crearPregunta(11L, 9L, null, "N", "E", null, null)).getMessage());
    }

    @Test
    void crearPregunta_nombreOTextoYaRegistradosEnElFactor_noSePermite() {
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(preguntaDao.existsByFactorIdAndNombreIgnoreCaseAndActivaTrue(11L, "RUTA CRITICA")).thenReturn(true);

        assertTrue(error(() -> service.crearPregunta(11L, 1L, null, "ruta critica", "E", null, null))
                .getMessage().startsWith("Ya se encuentra registrada una pregunta con el mismo nombre"));

        when(preguntaDao.existsByFactorIdAndEnunciadoIgnoreCaseAndActivaTrue(11L, "¿QUÉ ES LA RUTA CRÍTICA?")).thenReturn(true);
        assertTrue(error(() -> service.crearPregunta(11L, 1L, null, "OTRA", "¿qué es la ruta crítica?", null, null))
                .getMessage().startsWith("Ya se encuentra registrada una pregunta con el mismo texto"));
        verify(preguntaDao, never()).save(any());
    }

    @Test
    void crearPregunta_codigoExistente_noSePermite() {
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(preguntaDao.findByCodigoIgnoreCase("PREG-1")).thenReturn(Optional.of(p1));

        assertEquals("El código de pregunta PREG-1 ya existe",
                error(() -> service.crearPregunta(11L, 1L, "preg-1", "N", "E", null, null)).getMessage());
    }

    @Test
    void crearPregunta_ponderacionesInvalidas_informaElMotivo() {
        OpcionRespuesta sinOpciones = Datos.opcion(5L, "VACIA");
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(opcionRespuestaDao.findById(5L)).thenReturn(Optional.of(sinOpciones));

        assertEquals("Las ponderaciones de las opciones deben ser 0 o positivas",
                error(() -> service.crearPregunta(11L, 1L, null, "N", "E", null, ponderaciones(11L, "11", 12L, "-1"))).getMessage());
        assertEquals("Las ponderaciones de las opciones de respuesta deben sumar 10 puntos (actual: 9)",
                error(() -> service.crearPregunta(11L, 1L, null, "N", "E", null, ponderaciones(11L, "9", 12L, "0"))).getMessage());
        assertEquals("La opción de respuesta no posee ítems configurados",
                error(() -> service.crearPregunta(11L, 5L, null, "N", "E", null, null)).getMessage());
        verify(preguntaDao, never()).save(any());
    }

    @Test
    void modificarPregunta_cambiaLaOpcionDeRespuestaYSusPonderaciones() {
        when(preguntaDao.findById(1L)).thenReturn(Optional.of(p1));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L)).thenReturn(List.of(p1, p2));
        when(opcionRespuestaDao.findById(2L)).thenReturn(Optional.of(frecuencia));
        when(preguntaDao.save(p1)).thenReturn(p1);

        Pregunta p = service.modificarPregunta(1L, 2L, " pregunta 1 ", " nuevo texto ", " d ", ponderaciones(21L, "0", 22L, "5", 23L, "5"));

        assertSame(frecuencia, p.getOpcionRespuesta());
        assertEquals("NUEVO TEXTO", p.getEnunciado());
        assertEquals("D", p.getDescripcion());
        assertEquals(ponderaciones(21L, "0", 22L, "5", 23L, "5"), p.getPonderacionesPorDetalle());
    }

    @Test
    void modificarPregunta_sinOpcionNueva_conservaLaActualYReparteLasPonderaciones() {
        when(preguntaDao.findById(1L)).thenReturn(Optional.of(p1));

        service.modificarPregunta(1L, null, "PREGUNTA 1", "TEXTO", null, new HashMap<>());

        assertSame(vf, p1.getOpcionRespuesta());
        assertEquals(new BigDecimal("5.00"), p1.getPonderacionesPorDetalle().get(11L));
        assertEquals(new BigDecimal("5.00"), p1.getPonderacionesPorDetalle().get(12L));
        verify(preguntaDao).save(p1);
    }

    @Test
    void modificarPregunta_nombreDeOtraPreguntaDelFactor_noSePermite() {
        when(preguntaDao.findById(1L)).thenReturn(Optional.of(p1));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L)).thenReturn(List.of(p1, p2));

        assertEquals("La pregunta ya existe definida para esa competencia y factor",
                error(() -> service.modificarPregunta(1L, null, "pregunta 2", "E", null, null)).getMessage());
    }

    @Test
    void modificarPregunta_inexistenteDatosInvalidosUOpcionInexistente_informa() {
        assertEquals("Pregunta no encontrada", error(() -> service.modificarPregunta(99L, null, "N", "E", null, null)).getMessage());

        when(preguntaDao.findById(1L)).thenReturn(Optional.of(p1));
        assertEquals(List.of("El nombre de pregunta es obligatorio", "El enunciado de pregunta es obligatorio",
                        "La descripción excede la longitud máxima de 255 caracteres"),
                error(() -> service.modificarPregunta(1L, null, " ", null, "D".repeat(256), null)).getDetalles());
        assertEquals(List.of("El nombre excede la longitud máxima de 120 caracteres"),
                error(() -> service.modificarPregunta(1L, null, "N".repeat(121), "E", null, null)).getDetalles());
        assertEquals("Opción de respuesta no encontrada",
                error(() -> service.modificarPregunta(1L, 9L, "N", "E", null, null)).getMessage());
    }

    @Test
    void darBajaPregunta_laDesactivaYRegistraLaAuditoria() {
        assertEquals("Pregunta no encontrada", error(() -> service.darBajaPregunta(99L, "MGOMEZ")).getMessage());
        when(preguntaDao.findById(1L)).thenReturn(Optional.of(p1));

        service.darBajaPregunta(1L, "MGOMEZ");

        assertFalse(p1.isActiva());
        verify(preguntaDao).save(p1);
        AuditoriaEliminacion auditoria = auditoriaRegistrada();
        assertEquals("PREGUNTA", auditoria.getEntidad());
        assertEquals("PREGUNTA 1", auditoria.getIdentificador());
        assertEquals("Baja lógica de pregunta", auditoria.getDetalle());
    }

    // ================================================================== PUESTOS

    @Test
    void buscarPuestos_filtraPorCodigoNombreYEmpresa() {
        Puesto pm = projectManager();
        Puesto arq = arquitecto();
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arq, pm));

        assertEquals(List.of(pm), service.buscarPuestos(" pm ", null, null));
        assertEquals(List.of(arq), service.buscarPuestos(null, "arquitecto", "fintech"));
        assertEquals(List.of(arq, pm), service.buscarPuestos(null, null, null));
    }

}
