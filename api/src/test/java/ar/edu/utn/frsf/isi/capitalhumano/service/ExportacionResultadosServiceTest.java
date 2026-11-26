package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService.ArchivoExportadoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService.CandidatoEvaluadoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService.PuestoExportableDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CU007 - Exportar resultados (archivo CSV con el resultado de cada candidato por competencia). */
@ExtendWith(MockitoExtension.class)
class ExportacionResultadosServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);

    @Mock
    private PuestoDao puestoDao;

    @Mock
    private CuestionarioDao cuestionarioDao;

    @Mock
    private RespuestaCuestionarioDao respuestaDao;

    private ExportacionResultadosService service;

    // ------------------------------------------------------------------ datos de prueba

    private final Competencia gp = Datos.competencia(1L, "COMP-GP", "GESTION DE PROYECTOS");
    private final Competencia le = Datos.competencia(2L, "COMP-LE", "LEALTAD");
    private final Factor planificacion = Datos.factor(11L, gp, "FAC-GP-01", "PLANIFICACION", 1);
    private final Factor comunicacion = Datos.factor(21L, le, "FAC-LE-01", "COMUNICACION", 1);
    private final OpcionRespuesta escala = Datos.opcion(1L, "ESCALA", "NUNCA", "SIEMPRE");
    private final Puesto pm = Datos.puesto(1L, "PUE-PM-01", "PROJECT MANAGER TIC", "TECNOLOGIA DEL SUR S.A.");
    private final Puesto arquitecto = Datos.puesto(2L, "PUE-SR-02", "ARQUITECTO DE SOFTWARE SR", "FINTECH LITORAL S.R.L.");
    private final Map<Long, RespuestaCuestionario> respuestas = new HashMap<>();

    @BeforeEach
    void crearServicio() {
        // El cálculo de puntajes por competencia lo reutiliza de ReporteService (la clase real, con DAOs mockeados).
        service = new ExportacionResultadosService(puestoDao, cuestionarioDao, new ReporteService(puestoDao, cuestionarioDao, respuestaDao));
        // Orden "desordenado" a propósito: el archivo debe ir por código de competencia.
        Datos.exige(pm, le, "3");
        Datos.exige(pm, gp, "4");
    }

    private Cuestionario evaluado(long id, String apellido, String nombre, String estado, LocalDateTime fin, String gestion, String lealtad) {
        Cuestionario q = Datos.cuestionario(id, Datos.candidato(id, "CAND-00" + id, apellido, nombre), pm, estado);
        q.setFechaFin(fin);
        if (gestion != null) {
            PreguntaCuestionario pq = Datos.item(id * 10 + 1, q, Datos.pregunta(id * 10 + 1, planificacion, escala, "P1"), 1);
            respuestas.put(pq.getId(), Datos.respuesta(pq, gestion));
        }
        if (lealtad != null) {
            PreguntaCuestionario pq = Datos.item(id * 10 + 2, q, Datos.pregunta(id * 10 + 2, comunicacion, escala, "P2"), 1);
            respuestas.put(pq.getId(), Datos.respuesta(pq, lealtad));
        }
        return q;
    }

    private Cuestionario sosa() {
        return evaluado(2L, "SOSA", "VALERIA SOFIA", "COMPLETO", LocalDateTime.of(2026, 11, 24, 5, 3), "11.5", "7.5");
    }

    private Cuestionario gutierrez() {
        return evaluado(3L, "GUTIERREZ", "TOMAS ARIEL", "COMPLETO", LocalDateTime.of(2026, 11, 24, 6, 3), "10.5", "2.5");
    }

    private List<Cuestionario> cuestionariosPm() {
        return List.of(sosa(), gutierrez(),
                evaluado(4L, "PEREZ", "LUCAS", "EN_PROCESO", null, null, null),
                evaluado(5L, "ACOSTA", "ANA", "COMPLETO", LocalDateTime.of(2026, 10, 15, 12, 0), "10", "10"));
    }

    private void respuestasGuardadas() {
        when(respuestaDao.findByPreguntaCuestionarioId(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(respuestas.get(inv.getArgument(0))));
    }

    // ------------------------------------------------------------------ búsqueda de funciones (pasos 2 y 3)

    @Test
    void buscarPuestos_sinNingunFiltro_pideCompletarAlMenosUno() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.buscarPuestos("  ", null, null, null));

        assertEquals("Debe completar al menos un filtro para buscar funciones", e.getMessage());
        verifyNoInteractions(puestoDao, cuestionarioDao);
    }

    @Test
    void buscarPuestos_periodoInvertido_informa() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.buscarPuestos(null, null, HASTA, DESDE));

        assertEquals("La fecha desde no puede ser posterior a la fecha hasta", e.getMessage());
    }

    @Test
    void buscarPuestos_porNombre_cuentaSoloLosCuestionariosCompletos() {
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arquitecto, pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(cuestionariosPm());

        List<PuestoExportableDTO> puestos = service.buscarPuestos(" project ", null, null, null);

        assertEquals(1, puestos.size());
        assertEquals("PUE-PM-01", puestos.get(0).codigo());
        assertEquals("TECNOLOGIA DEL SUR S.A.", puestos.get(0).empresa());
        assertEquals(3, puestos.get(0).evaluados());
    }

    @Test
    void buscarPuestos_porEmpresaSinFechas_incluyeFuncionesSinEvaluados() {
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arquitecto, pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(2L)).thenReturn(List.of());

        List<PuestoExportableDTO> puestos = service.buscarPuestos(null, "fintech", null, null);

        assertEquals(1, puestos.size());
        assertEquals(0, puestos.get(0).evaluados());
    }

    @Test
    void buscarPuestos_conPeriodo_soloFuncionesConEvaluadosEnEsasFechas() {
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arquitecto, pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(2L)).thenReturn(List.of());
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(cuestionariosPm());

        List<PuestoExportableDTO> puestos = service.buscarPuestos(null, null, DESDE, LocalDate.of(2026, 11, 24));

        // ACOSTA terminó en octubre: queda afuera. El 24/11 se incluye (los extremos cuentan).
        assertEquals(1, puestos.size());
        assertEquals("PROJECT MANAGER TIC", puestos.get(0).nombre());
        assertEquals(2, puestos.get(0).evaluados());
    }

    // ------------------------------------------------------------------ candidatos evaluados (paso 5)

    @Test
    void listarEvaluados_ordenadosPorApellidoYDentroDelPeriodo() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(cuestionariosPm());

        List<CandidatoEvaluadoDTO> evaluados = service.listarEvaluados(1L, DESDE, null);

        assertEquals(2, evaluados.size());
        CandidatoEvaluadoDTO primero = evaluados.get(0);
        assertEquals("GUTIERREZ", primero.apellido());
        assertEquals("TOMAS ARIEL", primero.nombre());
        assertEquals("DNI", primero.tipoDocumento());
        assertEquals("30000003", primero.nroDocumento());
        assertEquals("CAND-003", primero.nroCandidato());
        assertEquals(3L, primero.cuestionarioId());
        assertEquals(LocalDateTime.of(2026, 11, 24, 6, 3), primero.fechaFin());
        assertEquals("SOSA", evaluados.get(1).apellido());
    }

    @Test
    void listarEvaluados_sinFuncionOFuncionInexistente_informa() {
        assertEquals("Debe seleccionar una función",
                assertThrows(BusinessException.class, () -> service.listarEvaluados(null, null, null)).getMessage());
        assertEquals("Función no encontrada",
                assertThrows(BusinessException.class, () -> service.listarEvaluados(9L, null, null)).getMessage());
    }

    // ------------------------------------------------------------------ exportar (pasos 6 a 9)

    @Test
    void exportar_unaFilaPorCandidatoYCompetenciaConElFormatoDelEnunciado() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(cuestionariosPm());
        respuestasGuardadas();

        ArchivoExportadoDTO archivo = service.exportar(1L, DESDE, HASTA, LocalDateTime.of(2026, 11, 26, 8, 6, 45));

        assertEquals("2026-11-26-08-06-TECNOLOGIA DEL SUR S.A.-PROJECT MANAGER TIC.csv", archivo.nombreArchivo());
        assertEquals(4, archivo.filas());
        String esperado = String.join("\r\n",
                "Nro candidato;Apellido;Nombre;Tipo documento;Nro documento;Código Competencia;Resultado;Nombre puesto;Código puesto;Fecha finalización cuestionario",
                "CAND-003;GUTIERREZ;TOMAS ARIEL;DNI;30000003;COMP-GP;5,25;PROJECT MANAGER TIC;PUE-PM-01;2026-11-24 06:03",
                "CAND-003;GUTIERREZ;TOMAS ARIEL;DNI;30000003;COMP-LE;1,25;PROJECT MANAGER TIC;PUE-PM-01;2026-11-24 06:03",
                "CAND-002;SOSA;VALERIA SOFIA;DNI;30000002;COMP-GP;5,75;PROJECT MANAGER TIC;PUE-PM-01;2026-11-24 05:03",
                "CAND-002;SOSA;VALERIA SOFIA;DNI;30000002;COMP-LE;3,75;PROJECT MANAGER TIC;PUE-PM-01;2026-11-24 05:03") + "\r\n";
        assertEquals(esperado, archivo.contenido());
    }

    @Test
    void exportar_competenciaSinRespuestas_seExportaConResultadoCero() {
        Cuestionario soloGestion = evaluado(2L, "SOSA", "VALERIA SOFIA", "COMPLETO", LocalDateTime.of(2026, 11, 24, 5, 3), "10", null);
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(List.of(soloGestion));
        respuestasGuardadas();

        ArchivoExportadoDTO archivo = service.exportar(1L, null, null, LocalDateTime.of(2026, 11, 26, 8, 6));

        assertTrue(archivo.contenido().contains(";COMP-LE;0,00;"), archivo.contenido());
        assertTrue(archivo.contenido().contains(";COMP-GP;5,00;"), archivo.contenido());
    }

    @Test
    void exportar_funcionSinEvaluados_informa() {
        when(puestoDao.findById(2L)).thenReturn(Optional.of(arquitecto));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(2L)).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class, () -> service.exportar(2L, null, null));

        assertEquals("La función ARQUITECTO DE SOFTWARE SR no tiene candidatos evaluados para exportar", e.getMessage());
    }

    @Test
    void exportar_sinFuncionSeleccionada_flujo6A() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.exportar(null, null, null));

        assertEquals("Debe seleccionar una función", e.getMessage());
    }

    @Test
    void exportar_conLaFechaActual_nombraElArchivoConFechaYHora() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.findByPuestoIdOrderByIdAsc(1L)).thenReturn(List.of(sosa()));
        respuestasGuardadas();

        ArchivoExportadoDTO archivo = service.exportar(1L, null, null);

        assertTrue(archivo.nombreArchivo().matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-TECNOLOGIA DEL SUR S\\.A\\.-PROJECT MANAGER TIC\\.csv"),
                archivo.nombreArchivo());
        assertEquals(2, archivo.filas());
    }

    @Test
    void nombreArchivo_quitaLosCaracteresQueWindowsNoAdmite() {
        Puesto raro = Datos.puesto(3L, "PUE-X", "QA / TESTER: SR", "ACME \"SUR\" <S.A.>");

        assertEquals("2026-01-05-09-07-ACME SUR S.A.-QA TESTER SR.csv",
                service.nombreArchivo(raro, LocalDateTime.of(2026, 1, 5, 9, 7)));
    }
}
