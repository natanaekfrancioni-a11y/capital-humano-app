package ar.edu.utn.frsf.isi.capitalhumano.service;

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

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CompetenciaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.OpcionRespuestaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.AuditoriaEliminacion;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.DetalleOpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.PuestoCompetencia;
import ar.edu.utn.frsf.isi.capitalhumano.service.CatalogoService.CompetenciaPuestoInput;
import ar.edu.utn.frsf.isi.capitalhumano.service.CatalogoService.EnUsoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.CatalogoService.ItemOpcionInput;
import java.math.BigDecimal;
import java.util.ArrayList;
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
 * Altas, modificaciones, búsquedas y bajas lógicas del catálogo: competencias (CU012-CU015),
 * factores (CU016-CU019), preguntas (CU020-CU023), opciones de respuesta (CU024, CU029-CU031)
 * y puestos (CU008-CU011). Las bajas pasan por las clases del patrón Template Method.
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

    @Mock
    private CuestionarioDao cuestionarioDao;

    @InjectMocks
    private CatalogoService service;

    // ------------------------------------------------------------------ datos de prueba

    private final Competencia gp = Datos.competencia(1L, "COMP-GP", "GESTION DE PROYECTOS");
    private final Competencia le = Datos.competencia(2L, "COMP-LE", "LEALTAD");
    private final Competencia tl = Datos.competencia(3L, "COMP-TL", "TRABAJO EN EQUIPO");
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

    @Test
    void crearCompetencia_seGuardaEnMayusculasYEsTecnicaPorDefecto() {
        when(competenciaDao.save(any(Competencia.class))).thenAnswer(inv -> inv.getArgument(0));

        Competencia tecnica = service.crearCompetencia(" comp-ba ", " bases de datos ", " modela datos ", null);
        Competencia actitudinal = service.crearCompetencia("COMP-NE", "NEGOCIACION", "ACUERDOS", " actitudinal ");

        assertEquals("COMP-BA", tecnica.getCodigo());
        assertEquals("BASES DE DATOS", tecnica.getNombre());
        assertEquals("MODELA DATOS", tecnica.getDescripcion());
        assertEquals("TECNICA", tecnica.getTipo());
        assertTrue(tecnica.isActiva());
        assertEquals("ACTITUDINAL", actitudinal.getTipo());
    }

    @Test
    void crearCompetencia_faltanDatos_listaTodosLosErrores() {
        BusinessException e = error(() -> service.crearCompetencia(null, " ", null, "TECNICA"));

        assertEquals("Datos incompletos para crear competencia", e.getMessage());
        assertEquals(List.of("El código es obligatorio", "El nombre es obligatorio", "La descripción es obligatoria"), e.getDetalles());
        verifyNoInteractions(competenciaDao);
    }

    @Test
    void crearCompetencia_codigoONombreRepetido_noSeGuarda() {
        when(competenciaDao.findByCodigoIgnoreCase("COMP-GP")).thenReturn(Optional.of(gp));
        assertEquals("El código y/o nombre de competencia ya existe",
                error(() -> service.crearCompetencia("comp-gp", "OTRA", "D", null)).getMessage());

        when(competenciaDao.findByNombreIgnoreCase("LEALTAD")).thenReturn(Optional.of(le));
        assertEquals("El código y/o nombre de competencia ya existe",
                error(() -> service.crearCompetencia("COMP-NUEVA", "lealtad", "D", null)).getMessage());
        verify(competenciaDao, never()).save(any());
    }

    @Test
    void modificarCompetencia_actualizaNombreYDescripcion_mantenerElMismoNombreEsValido() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(competenciaDao.findByNombreIgnoreCase("GESTION DE PROYECTOS")).thenReturn(Optional.of(gp));
        when(competenciaDao.save(gp)).thenReturn(gp);

        Competencia modificada = service.modificarCompetencia(1L, " gestion de proyectos ", " nueva descripcion ");

        assertSame(gp, modificada);
        assertEquals("NUEVA DESCRIPCION", gp.getDescripcion());
    }

    @Test
    void modificarCompetencia_nombreDeOtraActiva_noSePermite_deUnaDadaDeBajaSi() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(competenciaDao.findByNombreIgnoreCase("LEALTAD")).thenReturn(Optional.of(le));

        assertEquals("El nombre de competencia ya existe",
                error(() -> service.modificarCompetencia(1L, "LEALTAD", "D")).getMessage());

        le.setActiva(false);
        service.modificarCompetencia(1L, "LEALTAD", "D");
        assertEquals("LEALTAD", gp.getNombre());
        verify(competenciaDao).save(gp);
    }

    @Test
    void modificarCompetencia_inexistenteOConDatosVacios_informa() {
        assertEquals("Competencia no encontrada", error(() -> service.modificarCompetencia(9L, "N", "D")).getMessage());

        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        BusinessException e = error(() -> service.modificarCompetencia(1L, " ", null));

        assertEquals(List.of("El nombre es obligatorio", "La descripción es obligatoria"), e.getDetalles());
    }

    @Test
    void darBajaCompetencia_sinUso_laDesactivaYRegistraQuienLaElimino() {
        when(competenciaDao.findById(3L)).thenReturn(Optional.of(tl));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(3L)).thenReturn(List.of());
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(projectManager()));

        service.darBajaCompetencia(3L, " mgomez ");

        assertFalse(tl.isActiva());
        verify(competenciaDao).save(tl);
        AuditoriaEliminacion auditoria = auditoriaRegistrada();
        assertEquals("COMPETENCIA", auditoria.getEntidad());
        assertEquals(3L, auditoria.getEntidadId());
        assertEquals("TRABAJO EN EQUIPO", auditoria.getIdentificador());
        assertEquals("MGOMEZ", auditoria.getUsuarioActor());
        assertEquals("Baja lógica de competencia", auditoria.getDetalle());
        assertNotNull(auditoria.getFechaHora());
    }

    @Test
    void darBajaCompetencia_conFactores_estaEnUso() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion));

        BusinessException e = error(() -> service.darBajaCompetencia(1L, "MGOMEZ"));

        assertEquals("La competencia GESTION DE PROYECTOS está siendo usada en la base de datos y no puede eliminarse", e.getMessage());
        assertTrue(gp.isActiva());
        verify(competenciaDao, never()).save(any());
        verifyNoInteractions(auditoriaDao);
    }

    @Test
    void darBajaCompetencia_exigidaPorUnPuesto_estaEnUso() {
        when(competenciaDao.findById(2L)).thenReturn(Optional.of(le));
        when(puestoDao.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(arquitecto(), projectManager()));

        assertTrue(error(() -> service.darBajaCompetencia(2L, "MGOMEZ")).getMessage().startsWith("La competencia LEALTAD está siendo usada"));
        verifyNoInteractions(auditoriaDao);
    }

    @Test
    void darBajaCompetencia_inexistenteOYaDadaDeBaja_informa() {
        assertEquals("Competencia no encontrada", error(() -> service.darBajaCompetencia(9L, "MGOMEZ")).getMessage());

        tl.setActiva(false);
        when(competenciaDao.findById(3L)).thenReturn(Optional.of(tl));
        assertEquals("Competencia no encontrada", error(() -> service.darBajaCompetencia(3L, "MGOMEZ")).getMessage());
    }

    @Test
    void verificarUsoCompetencia_enUso_explicaElMotivo() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion));

        assertEquals(new EnUsoDTO(true, "La competencia GESTION DE PROYECTOS está siendo usada en la base de datos y no puede eliminarse"),
                service.verificarUsoCompetencia(1L));
    }

    @Test
    void verificarUsoCompetencia_libre_avisaQueSeEliminaranLosDatos() {
        when(competenciaDao.findById(3L)).thenReturn(Optional.of(tl));

        assertEquals(new EnUsoDTO(false, "Los datos de la competencia TRABAJO EN EQUIPO serán eliminados del sistema"),
                service.verificarUsoCompetencia(3L));
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

    @Test
    void crearFactor_datosValidos_loAsociaALaCompetencia() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.save(any(Factor.class))).thenAnswer(inv -> inv.getArgument(0));

        Factor f = service.crearFactor(1L, " fac-gp-03 ", " alcance ", " define el alcance ", 3);

        assertSame(gp, f.getCompetencia());
        assertEquals("FAC-GP-03", f.getCodigo());
        assertEquals("ALCANCE", f.getNombre());
        assertEquals("DEFINE EL ALCANCE", f.getDescripcion());
        assertEquals(3, f.getNroOrden());
        assertTrue(f.isActivo());
    }

    @Test
    void crearFactor_faltanDatos_listaTodosLosErrores() {
        BusinessException e = error(() -> service.crearFactor(null, " ", null, "", 0));

        assertEquals(List.of("La competencia es obligatoria", "El código es obligatorio", "El nombre es obligatorio",
                "La descripción es obligatoria", "El número de orden debe ser mayor a 0"), e.getDetalles());
        assertEquals(1, error(() -> service.crearFactor(1L, "C", "N", "D", null)).getDetalles().size());
        verifyNoInteractions(factorDao, competenciaDao);
    }

    @Test
    void crearFactor_competenciaInexistente_informa() {
        assertEquals("Competencia no encontrada", error(() -> service.crearFactor(9L, "C", "N", "D", 1)).getMessage());
    }

    @Test
    void crearFactor_codigoONombreRepetido_noSePermite() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.findByCodigoIgnoreCase("FAC-GP-01")).thenReturn(Optional.of(planificacion));
        assertEquals("El código y/o nombre de factor ya existe",
                error(() -> service.crearFactor(1L, "FAC-GP-01", "OTRO", "D", 5)).getMessage());

        when(factorDao.existsByCompetenciaIdAndNombreIgnoreCaseAndActivoTrue(1L, "PLANIFICACION")).thenReturn(true);
        assertEquals("El código y/o nombre de factor ya existe",
                error(() -> service.crearFactor(1L, "FAC-GP-09", "planificacion", "D", 5)).getMessage());
    }

    @Test
    void crearFactor_nroDeOrdenRepetidoEnLaCompetencia_noSePermite() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.existsByCompetenciaIdAndNroOrdenAndActivoTrue(1L, 1)).thenReturn(true);

        assertEquals("El número de orden ya existe para la competencia seleccionada",
                error(() -> service.crearFactor(1L, "FAC-GP-09", "NUEVO", "D", 1)).getMessage());
        verify(factorDao, never()).save(any());
    }

    @Test
    void modificarFactor_datosValidos_actualizaElFactor() {
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion, riesgos));
        when(factorDao.save(planificacion)).thenReturn(planificacion);

        Factor f = service.modificarFactor(11L, " planificacion agil ", " sprints ", 3);

        assertEquals("PLANIFICACION AGIL", f.getNombre());
        assertEquals("SPRINTS", f.getDescripcion());
        assertEquals(3, f.getNroOrden());
    }

    @Test
    void modificarFactor_nombreOOrdenDeOtroFactorDeLaCompetencia_noSePermite() {
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(1L)).thenReturn(List.of(planificacion, riesgos));

        assertEquals("El nombre de factor ya existe para la competencia seleccionada",
                error(() -> service.modificarFactor(11L, "riesgos", "D", 1)).getMessage());
        assertEquals("El número de orden ya existe para la competencia seleccionada",
                error(() -> service.modificarFactor(11L, "PLANIFICACION", "D", 2)).getMessage());
        verify(factorDao, never()).save(any());
    }

    @Test
    void modificarFactor_inexistenteOConDatosVacios_informa() {
        assertEquals("Factor no encontrado", error(() -> service.modificarFactor(99L, "N", "D", 1)).getMessage());

        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        assertEquals(3, error(() -> service.modificarFactor(11L, "", " ", null)).getDetalles().size());
    }

    @Test
    void darBajaFactor_tambienDaDeBajaSusPreguntasYAuditaCadaUna() {
        assertEquals("Factor no encontrado", error(() -> service.darBajaFactor(99L, "MGOMEZ")).getMessage());
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L)).thenReturn(List.of(p1, p2));

        service.darBajaFactor(11L, null);

        assertFalse(planificacion.isActivo());
        assertFalse(p1.isActiva());
        assertFalse(p2.isActiva());
        verify(preguntaDao).save(p1);
        verify(preguntaDao).save(p2);
        verify(factorDao).save(planificacion);
        ArgumentCaptor<AuditoriaEliminacion> auditorias = ArgumentCaptor.forClass(AuditoriaEliminacion.class);
        verify(auditoriaDao, times(3)).save(auditorias.capture());
        List<AuditoriaEliminacion> registradas = auditorias.getAllValues();
        assertEquals("PREGUNTA", registradas.get(0).getEntidad());
        assertEquals("Baja lógica por eliminación de factor PLANIFICACION", registradas.get(1).getDetalle());
        assertEquals("FACTOR", registradas.get(2).getEntidad());
        assertEquals("CONSULTOR", registradas.get(2).getUsuarioActor(), "sin usuario se registra CONSULTOR");
    }

    // ================================================================== OPCIONES DE RESPUESTA

    @Test
    void buscarOpcionesRespuesta_sinNombreTraeLasActivas_conNombreFiltra() {
        when(opcionRespuestaDao.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(frecuencia, vf));
        when(opcionRespuestaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc("ver")).thenReturn(List.of(vf));

        assertEquals(List.of(frecuencia, vf), service.buscarOpcionesRespuesta(null));
        assertEquals(List.of(vf), service.buscarOpcionesRespuesta(" ver "));
    }

    @Test
    void crearOpcionRespuesta_datosValidos_creaLasOpcionesEnMayusculas() {
        when(opcionRespuestaDao.save(any(OpcionRespuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        OpcionRespuesta op = service.crearOpcionRespuesta(" si-no ", " respuesta simple ",
                List.of(new ItemOpcionInput(null, 2, " si "), new ItemOpcionInput(null, 1, "no")));

        assertEquals("SI-NO", op.getNombre());
        assertEquals("RESPUESTA SIMPLE", op.getDescripcion());
        assertTrue(op.isActiva());
        assertEquals(2, op.getDetalles().size());
        DetalleOpcionRespuesta si = op.getDetalles().get(0);
        assertEquals("SI", si.getDescripcion());
        assertEquals(2, si.getNroOrden());
        assertSame(op, si.getOpcionRespuesta());
    }

    @Test
    void crearOpcionRespuesta_faltanDatos_listaTodosLosErrores() {
        assertEquals(List.of("El nombre es obligatorio", "La descripción es obligatoria", "Debe ingresar al menos una opción"),
                error(() -> service.crearOpcionRespuesta(" ", null, null)).getDetalles());
        assertEquals(1, error(() -> service.crearOpcionRespuesta("N", "D", List.of())).getDetalles().size());
        verifyNoInteractions(opcionRespuestaDao);
    }

    @Test
    void crearOpcionRespuesta_nombreRepetido_noSePermite() {
        when(opcionRespuestaDao.findByNombreIgnoreCase("VERDADERO-FALSO")).thenReturn(Optional.of(vf));

        assertEquals("El nombre de la opción de respuesta ya existe",
                error(() -> service.crearOpcionRespuesta("verdadero-falso", "D", List.of(new ItemOpcionInput(null, 1, "A")))).getMessage());
    }

    @Test
    void crearOpcionRespuesta_opcionesInvalidas_cadaReglaTieneSuMensaje() {
        assertEquals("La descripción de cada ítem es obligatoria",
                error(() -> service.crearOpcionRespuesta("N", "D", List.of(new ItemOpcionInput(null, 1, " ")))).getMessage());
        assertEquals("La descripción de opción está más de una vez en la Opción de Respuesta",
                error(() -> service.crearOpcionRespuesta("N", "D",
                        List.of(new ItemOpcionInput(null, 1, "si"), new ItemOpcionInput(null, 2, "SI ")))).getMessage());
        assertEquals("El orden de visualización debe ser positivo",
                error(() -> service.crearOpcionRespuesta("N", "D", List.of(new ItemOpcionInput(null, 0, "A")))).getMessage());
        assertEquals("El orden de visualización debe ser positivo",
                error(() -> service.crearOpcionRespuesta("N", "D", List.of(new ItemOpcionInput(null, null, "A")))).getMessage());
        assertEquals("El orden de visualización no es consecutivo",
                error(() -> service.crearOpcionRespuesta("N", "D",
                        List.of(new ItemOpcionInput(null, 1, "A"), new ItemOpcionInput(null, 3, "B")))).getMessage());
        verify(opcionRespuestaDao, never()).save(any());
    }

    private static String opciones(OpcionRespuesta op) {
        return op.getDetalles().stream().map(d -> d.getNroOrden() + ":" + d.getDescripcion()).collect(Collectors.joining(","));
    }

    @Test
    void modificarOpcionRespuesta_intercambiarElOrden_pasaPrimeroPorValoresProvisorios() {
        List<String> guardados = new ArrayList<>();
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(opcionRespuestaDao.saveAndFlush(vf)).thenAnswer(inv -> {
            guardados.add(opciones(vf));
            return vf;
        });

        OpcionRespuesta op = service.modificarOpcionRespuesta(1L, " nueva descripcion ",
                List.of(new ItemOpcionInput(11L, 2, "verdadero"), new ItemOpcionInput(12L, 1, "falso")));

        // La base no admite dos opciones con el mismo orden: primero se guardan valores provisorios.
        assertEquals(List.of("1000:~11,1001:~12", "2:VERDADERO,1:FALSO"), guardados);
        assertEquals("NUEVA DESCRIPCION", op.getDescripcion());
    }

    @Test
    void modificarOpcionRespuesta_sinCambiosEnLasOpciones_guardaUnaSolaVez() {
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));

        service.modificarOpcionRespuesta(1L, "OTRA", List.of(new ItemOpcionInput(11L, 1, "VERDADERO"), new ItemOpcionInput(12L, 2, "FALSO")));
        service.modificarOpcionRespuesta(1L, "SOLO DESCRIPCION", null);

        verify(opcionRespuestaDao, times(2)).saveAndFlush(vf);
        assertEquals("SOLO DESCRIPCION", vf.getDescripcion());
        assertEquals("1:VERDADERO,2:FALSO", opciones(vf));
    }

    @Test
    void modificarOpcionRespuesta_agregarOQuitarOpciones_noSePermite() {
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        String mensaje = "No se pueden agregar ni quitar opciones de una opción de respuesta existente";

        assertEquals(mensaje, error(() -> service.modificarOpcionRespuesta(1L, "D", List.of(new ItemOpcionInput(11L, 1, "VERDADERO")))).getMessage());
        assertEquals(mensaje, error(() -> service.modificarOpcionRespuesta(1L, "D",
                List.of(new ItemOpcionInput(11L, 1, "A"), new ItemOpcionInput(null, 2, "B")))).getMessage());
        verify(opcionRespuestaDao, never()).saveAndFlush(any());
    }

    @Test
    void modificarOpcionRespuesta_datosBlanqueadosOInexistente_informa() {
        assertEquals("Opción de respuesta no encontrada", error(() -> service.modificarOpcionRespuesta(9L, "D", null)).getMessage());

        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        assertEquals(List.of("La descripción es obligatoria", "La descripción de cada opción es obligatoria"),
                error(() -> service.modificarOpcionRespuesta(1L, " ", List.of(new ItemOpcionInput(11L, 1, ""), new ItemOpcionInput(12L, 2, "B"))))
                        .getDetalles());
    }

    @Test
    void darBajaOpcionRespuesta_usadaPorUnaPregunta_noSePuede() {
        when(opcionRespuestaDao.findById(1L)).thenReturn(Optional.of(vf));
        when(preguntaDao.existsByOpcionRespuestaIdAndActivaTrue(1L)).thenReturn(true);

        assertEquals("La opción de respuesta VERDADERO-FALSO está siendo usada en la base de datos y no puede eliminarse",
                error(() -> service.darBajaOpcionRespuesta(1L, "MGOMEZ")).getMessage());
        verifyNoInteractions(auditoriaDao);
    }

    @Test
    void darBajaOpcionRespuesta_sinUso_laDesactiva() {
        when(opcionRespuestaDao.findById(2L)).thenReturn(Optional.of(frecuencia));

        service.darBajaOpcionRespuesta(2L, "mgomez");

        assertFalse(frecuencia.isActiva());
        verify(opcionRespuestaDao).save(frecuencia);
        AuditoriaEliminacion auditoria = auditoriaRegistrada();
        assertEquals("OPCION_RESPUESTA", auditoria.getEntidad());
        assertEquals("Baja lógica de opción de respuesta", auditoria.getDetalle());
        assertEquals("Opción de respuesta no encontrada", error(() -> service.darBajaOpcionRespuesta(2L, "MGOMEZ")).getMessage());
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

    @Test
    void crearPuesto_datosValidos_loCreaConSusCompetencias() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(competenciaDao.findById(2L)).thenReturn(Optional.of(le));
        when(puestoDao.save(any(Puesto.class))).thenAnswer(inv -> inv.getArgument(0));

        Puesto p = service.crearPuesto(" pue-qa-03 ", " analista qa ", " acme ", " pruebas ",
                List.of(new CompetenciaPuestoInput(1L, new BigDecimal("4")), new CompetenciaPuestoInput(2L, BigDecimal.ZERO)));

        assertEquals("PUE-QA-03", p.getCodigo());
        assertEquals("ANALISTA QA", p.getNombre());
        assertEquals("ACME", p.getEmpresa());
        assertEquals("PRUEBAS", p.getDescripcion());
        assertEquals(2, p.getCompetencias().size());
        PuestoCompetencia primera = p.getCompetencias().get(0);
        assertSame(p, primera.getPuesto());
        assertSame(gp, primera.getCompetencia());
        assertEquals(new BigDecimal("4"), primera.getPonderacionMinima());
    }

    @Test
    void crearPuesto_faltanDatos_listaTodosLosErrores() {
        assertEquals(List.of("El código del puesto es obligatorio", "El nombre del puesto es obligatorio", "La empresa es obligatoria",
                        "La descripción es obligatoria", "Debe asociar al menos una competencia"),
                error(() -> service.crearPuesto(null, "", " ", null, null)).getDetalles());
        assertEquals(1, error(() -> service.crearPuesto("C", "N", "E", "D", List.of())).getDetalles().size());
        verifyNoInteractions(puestoDao);
    }

    @Test
    void crearPuesto_codigoONombreExistente_noSePermite() {
        when(puestoDao.findByCodigoIgnoreCase("PUE-PM-01")).thenReturn(Optional.of(projectManager()));

        assertEquals("El código y/o nombre del puesto ya existen", error(() -> service.crearPuesto("pue-pm-01", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE)))).getMessage());
    }

    @Test
    void crearPuesto_competenciasInvalidas_cadaReglaTieneSuMensaje() {
        String repetida = "Cada competencia puede existir una sola vez para cada puesto";
        String fueraDeRango = "Las ponderaciones son valores entre 0 y 10";

        assertEquals(repetida, error(() -> service.crearPuesto("C", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(null, BigDecimal.ONE)))).getMessage());
        assertEquals(fueraDeRango, error(() -> service.crearPuesto("C", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, null)))).getMessage());
        assertEquals(fueraDeRango, error(() -> service.crearPuesto("C", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, new BigDecimal("-0.5"))))).getMessage());
        assertEquals(fueraDeRango, error(() -> service.crearPuesto("C", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, new BigDecimal("10.5"))))).getMessage());

        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        assertEquals(repetida, error(() -> service.crearPuesto("C", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE), new CompetenciaPuestoInput(1L, BigDecimal.TEN)))).getMessage());
        assertEquals("Competencia no encontrada: 9", error(() -> service.crearPuesto("C", "N", "E", "D",
                List.of(new CompetenciaPuestoInput(9L, BigDecimal.ONE)))).getMessage());
        verify(puestoDao, never()).save(any());
    }

    @Test
    void modificarPuesto_quitaActualizaYAgregaCompetencias() {
        Puesto pm = projectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(competenciaDao.findById(3L)).thenReturn(Optional.of(tl));
        when(puestoDao.save(pm)).thenReturn(pm);

        Puesto modificado = service.modificarPuesto(1L, " project manager sr ", " acme ", " lidera proyectos ",
                List.of(new CompetenciaPuestoInput(1L, new BigDecimal("5")), new CompetenciaPuestoInput(3L, new BigDecimal("2"))));

        assertEquals("PROJECT MANAGER SR", modificado.getNombre());
        assertEquals("ACME", modificado.getEmpresa());
        assertEquals(2, modificado.getCompetencias().size());
        assertSame(gp, modificado.getCompetencias().get(0).getCompetencia());
        assertEquals(new BigDecimal("5"), modificado.getCompetencias().get(0).getPonderacionMinima());
        assertSame(tl, modificado.getCompetencias().get(1).getCompetencia());
        verify(cuestionarioDao).existsByPuestoId(1L);
    }

    @Test
    void modificarPuesto_conCuestionariosGenerados_noSePuedenAgregarNiQuitarCompetencias() {
        Puesto pm = projectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.existsByPuestoId(1L)).thenReturn(true);

        BusinessException e = error(() -> service.modificarPuesto(1L, "PROJECT MANAGER TIC", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE))));

        assertEquals("No es posible agregar o quitar competencias porque ya existen cuestionarios generados para el puesto", e.getMessage());
        assertEquals(2, pm.getCompetencias().size());
    }

    @Test
    void modificarPuesto_mismasCompetencias_soloCambiaLasPonderacionesSinConsultarCuestionarios() {
        Puesto pm = projectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));

        service.modificarPuesto(1L, "PROJECT MANAGER TIC", "E", "D",
                List.of(new CompetenciaPuestoInput(2L, new BigDecimal("6")), new CompetenciaPuestoInput(1L, new BigDecimal("7"))));

        assertEquals(new BigDecimal("7"), pm.getCompetencias().get(0).getPonderacionMinima());
        assertEquals(new BigDecimal("6"), pm.getCompetencias().get(1).getPonderacionMinima());
        verify(cuestionarioDao, never()).existsByPuestoId(anyLong());
        verify(puestoDao).save(pm);
    }

    @Test
    void modificarPuesto_nombreDeOtroPuestoActivo_noSePermite() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(projectManager()));
        when(puestoDao.findByNombreIgnoreCase("ARQUITECTO DE SOFTWARE SR")).thenReturn(Optional.of(arquitecto()));

        assertEquals("El nombre del puesto ya existe", error(() -> service.modificarPuesto(1L, "arquitecto de software sr", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE)))).getMessage());
    }

    @Test
    void modificarPuesto_validaciones_cadaReglaTieneSuMensaje() {
        List<CompetenciaPuestoInput> una = List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE));
        assertEquals("Puesto no encontrado", error(() -> service.modificarPuesto(99L, "N", "E", "D", una)).getMessage());

        Puesto pm = projectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        assertEquals(4, error(() -> service.modificarPuesto(1L, " ", null, "", null)).getDetalles().size());
        assertEquals("Cada competencia puede existir una sola vez para cada puesto", error(() -> service.modificarPuesto(1L, "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE), new CompetenciaPuestoInput(1L, BigDecimal.ONE)))).getMessage());
        assertEquals("Cada competencia puede existir una sola vez para cada puesto", error(() -> service.modificarPuesto(1L, "N", "E", "D",
                List.of(new CompetenciaPuestoInput(null, BigDecimal.ONE)))).getMessage());
        assertEquals("Las ponderaciones son valores entre 0 y 10", error(() -> service.modificarPuesto(1L, "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, new BigDecimal("11"))))).getMessage());
        assertEquals("Competencia no encontrada: 9", error(() -> service.modificarPuesto(1L, "N", "E", "D",
                List.of(new CompetenciaPuestoInput(1L, BigDecimal.ONE), new CompetenciaPuestoInput(9L, BigDecimal.ONE)))).getMessage());
        verify(puestoDao, never()).save(any());
    }

    @Test
    void verificarUsoPuesto_conEvaluacionesEstaEnUso_sinEvaluacionesSePuedeEliminar() {
        when(puestoDao.findById(1L)).thenReturn(Optional.of(projectManager()));
        when(puestoDao.findById(2L)).thenReturn(Optional.of(arquitecto()));
        when(cuestionarioDao.existsByPuestoId(1L)).thenReturn(true);
        when(cuestionarioDao.existsByPuestoId(2L)).thenReturn(false);

        assertEquals(new EnUsoDTO(true, "El puesto PROJECT MANAGER TIC está siendo usado en la base de datos y no puede eliminarse"),
                service.verificarUsoPuesto(1L));
        assertEquals(new EnUsoDTO(false, "Los datos del puesto ARQUITECTO DE SOFTWARE SR serán eliminados del sistema"),
                service.verificarUsoPuesto(2L));
    }

    @Test
    void darBajaPuesto_sinEvaluaciones_loDesactivaYAudita() {
        assertEquals("Puesto no encontrado", error(() -> service.darBajaPuesto(99L, "MGOMEZ")).getMessage());
        Puesto arq = arquitecto();
        when(puestoDao.findById(2L)).thenReturn(Optional.of(arq));

        service.darBajaPuesto(2L, "MGOMEZ");

        assertFalse(arq.isActivo());
        verify(puestoDao).save(arq);
        AuditoriaEliminacion auditoria = auditoriaRegistrada();
        assertEquals("PUESTO", auditoria.getEntidad());
        assertEquals(2L, auditoria.getEntidadId());
        assertEquals("Baja lógica de puesto", auditoria.getDetalle());
    }

    @Test
    void darBajaPuesto_conEvaluaciones_noSePuedeEliminar() {
        Puesto pm = projectManager();
        when(puestoDao.findById(1L)).thenReturn(Optional.of(pm));
        when(cuestionarioDao.existsByPuestoId(1L)).thenReturn(true);

        assertTrue(error(() -> service.darBajaPuesto(1L, "MGOMEZ")).getMessage().startsWith("El puesto PROJECT MANAGER TIC está siendo usado"));
        assertTrue(pm.isActivo());
        verifyNoInteractions(auditoriaDao);
    }
}
