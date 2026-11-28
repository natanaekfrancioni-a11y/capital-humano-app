package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CompetenciaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.OpcionRespuestaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.service.GeneradorPreguntaIAService.PreguntaGeneradaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CU033 - Generar pregunta con IA, el proveedor de IA se reemplaza por un mock (sin internet ni clave) */
@ExtendWith(MockitoExtension.class)
class GeneradorPreguntaIAServiceTest {

    @Mock
    private CompetenciaDao competenciaDao;

    @Mock
    private FactorDao factorDao;

    @Mock
    private PreguntaDao preguntaDao;

    @Mock
    private OpcionRespuestaDao opcionRespuestaDao;

    @Mock
    private ProveedorIA proveedorIA;

    private GeneradorPreguntaIAService service;

    private final Competencia gp = Datos.competencia(1L, "COMP-GP", "GESTION DE PROYECTOS");
    private final Factor planificacion = Datos.factor(11L, gp, "FAC-GP-01", "PLANIFICACION", 1);
    private final OpcionRespuesta vf = Datos.opcion(1L, "VERDADERO-FALSO", "VERDADERO", "FALSO");
    private final OpcionRespuesta frecuencia = Datos.opcion(2L, "FRECUENCIA-3", "NUNCA", "A VECES", "SIEMPRE");

    @BeforeEach
    void crearServicio() {
        service = new GeneradorPreguntaIAService(competenciaDao, factorDao, preguntaDao, opcionRespuestaDao, proveedorIA, new ObjectMapper());
    }

    /** competencia, factor y opciones válidos, el factor ya tiene una pregunta */
    private void datosValidos() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(frecuencia, vf));
        when(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(11L))
                .thenReturn(List.of(Datos.pregunta(5L, planificacion, vf, "RUTA CRITICA", "10", "0")));
    }

    private void iaResponde(String texto) {
        when(proveedorIA.generarJson(anyString(), any(ObjectNode.class))).thenReturn(texto);
    }

    @Test
    void generar_completaLosDatosEnMayusculasYLasPonderacionesPorOpcion() {
        datosValidos();
        iaResponde("""
                ```json
                {"nombre": "  ¿alcance   del proyecto? ", "pregunta": "¿El alcance se define al inicio?",
                 "descripcion": "Evalúa la planificación", "opcionRespuesta": "verdadero-falso",
                 "ponderaciones": [{"orden": 1, "ponderacion": 10}, {"orden": 2, "ponderacion": "0"}]}
                ```""");

        PreguntaGeneradaDTO generada = service.generar(1L, 11L);

        assertEquals(1L, generada.competenciaId());
        assertEquals(11L, generada.factorId());
        assertEquals("¿ALCANCE DEL PROYECTO?", generada.nombre());
        assertEquals("¿EL ALCANCE SE DEFINE AL INICIO?", generada.enunciado());
        assertEquals("EVALÚA LA PLANIFICACIÓN", generada.descripcion());
        assertEquals(1L, generada.opcionRespuestaId());
        assertEquals(Map.of(11L, new BigDecimal("10.00"), 12L, new BigDecimal("0.00")), generada.ponderaciones());
    }

    @Test
    void generar_elPedidoIncluyeElContextoYElEsquemaLimitaLasOpciones() {
        datosValidos();
        iaResponde("{\"nombre\": \"N\", \"pregunta\": \"P\", \"opcionRespuesta\": \"FRECUENCIA-3\", \"ponderaciones\": []}");

        service.generar(1L, 11L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ObjectNode> esquema = ArgumentCaptor.forClass(ObjectNode.class);
        verify(proveedorIA).generarJson(prompt.capture(), esquema.capture());
        assertTrue(prompt.getValue().contains("Competencia: GESTION DE PROYECTOS (técnica): DESCRIPCION DE GESTION DE PROYECTOS"));
        assertTrue(prompt.getValue().contains("Factor a evaluar: PLANIFICACION: DESCRIPCION DE PLANIFICACION"));
        assertTrue(prompt.getValue().contains("- RUTA CRITICA: ENUNCIADO DE RUTA CRITICA"));
        assertTrue(prompt.getValue().contains("- FRECUENCIA-3 (DESCRIPCION DE FRECUENCIA-3). Opciones: 1) NUNCA; 2) A VECES; 3) SIEMPRE"));
        assertEquals("[\"FRECUENCIA-3\",\"VERDADERO-FALSO\"]",
                esquema.getValue().path("properties").path("opcionRespuesta").path("enum").toString());
    }

    @Test
    void armarPrompt_competenciaActitudinalSinDescripcionYFactorSinPreguntas() {
        Competencia actitudinal = Datos.competencia(2L, "COMP-LE", "LEALTAD");
        actitudinal.setTipo("ACTITUDINAL");
        actitudinal.setDescripcion(" ");
        Factor factor = Datos.factor(21L, actitudinal, "FAC-LE-01", "COMUNICACION", 1);
        factor.setDescripcion(null);

        String prompt = service.armarPrompt(actitudinal, factor, List.of(), List.of(vf));

        assertTrue(prompt.contains("Competencia: LEALTAD (actitudinal)\n"));
        assertTrue(prompt.contains("Factor a evaluar: COMUNICACION\n"));
        assertTrue(prompt.contains("- Todavía no hay preguntas para este factor."));
    }

    @Test
    void generar_ponderacionesQueNoSuman10_quedanParaQueLasCargueElConsultor() {
        datosValidos();
        iaResponde("{\"nombre\": \"N\", \"pregunta\": \"P\", \"descripcion\": \"\", \"opcionRespuesta\": \"FRECUENCIA-3\","
                + " \"ponderaciones\": [{\"orden\": 1, \"ponderacion\": 2}, {\"orden\": 2, \"ponderacion\": 3}, {\"orden\": 3, \"ponderacion\": 4}]}");

        PreguntaGeneradaDTO generada = service.generar(1L, 11L);

        assertEquals(2L, generada.opcionRespuestaId());
        assertTrue(generada.ponderaciones().isEmpty());
        assertNull(generada.descripcion(), "una descripción vacía no se completa");
    }

    @Test
    void generar_ponderacionesConComaDecimalSeAceptan_negativasOFaltantesNo() {
        datosValidos();
        when(proveedorIA.generarJson(anyString(), any(ObjectNode.class))).thenReturn(
                "{\"nombre\": \"N\", \"pregunta\": \"P\", \"opcionRespuesta\": \"FRECUENCIA-3\", \"ponderaciones\": ["
                        + "{\"orden\": 1, \"ponderacion\": \"0\"}, {\"orden\": 2, \"ponderacion\": \"4,5\"}, {\"orden\": 3, \"ponderacion\": 5.5}]}",
                "{\"nombre\": \"N\", \"pregunta\": \"P\", \"opcionRespuesta\": \"FRECUENCIA-3\", \"ponderaciones\": ["
                        + "{\"orden\": 1, \"ponderacion\": -1}, {\"orden\": 2, \"ponderacion\": 5}, {\"orden\": 3, \"ponderacion\": 6}]}",
                "{\"nombre\": \"N\", \"pregunta\": \"P\", \"opcionRespuesta\": \"FRECUENCIA-3\", \"ponderaciones\": ["
                        + "{\"orden\": 1, \"ponderacion\": \"diez\"}, {\"orden\": 0, \"ponderacion\": 5}, {\"orden\": 3, \"ponderacion\": 5}]}");

        assertEquals(Map.of(21L, new BigDecimal("0.00"), 22L, new BigDecimal("4.50"), 23L, new BigDecimal("5.50")),
                service.generar(1L, 11L).ponderaciones());
        assertTrue(service.generar(1L, 11L).ponderaciones().isEmpty(), "una ponderación negativa invalida todas");
        assertTrue(service.generar(1L, 11L).ponderaciones().isEmpty(), "faltan las opciones 1 y 2");
    }

    @Test
    void generar_textosMuyLargos_seRecortanAlMaximoDeLaPantalla() {
        datosValidos();
        iaResponde("{\"nombre\": \"" + "a".repeat(150) + "\", \"pregunta\": \"P\", \"opcionRespuesta\": \"VERDADERO-FALSO\"}");

        PreguntaGeneradaDTO generada = service.generar(1L, 11L);

        assertEquals(100, generada.nombre().length());
        assertTrue(generada.ponderaciones().isEmpty());
    }

    // datos obligatorios y válidos

    @Test
    void generar_sinCompetenciaNiFactor_listaLoQueFalta() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.generar(null, null));

        assertEquals("Faltan datos para generar la pregunta", e.getMessage());
        assertEquals(List.of("La competencia es obligatoria", "El factor es obligatorio"), e.getDetalles());
        verifyNoInteractions(competenciaDao, proveedorIA);
    }

    @Test
    void generar_competenciaInexistenteOInactiva_informa() {
        Competencia inactiva = Datos.competencia(2L, "COMP-X", "INACTIVA");
        inactiva.setActiva(false);
        when(competenciaDao.findById(2L)).thenReturn(Optional.of(inactiva));

        assertEquals("La competencia seleccionada no existe",
                assertThrows(BusinessException.class, () -> service.generar(2L, 11L)).getMessage());
        assertEquals("La competencia seleccionada no existe",
                assertThrows(BusinessException.class, () -> service.generar(9L, 11L)).getMessage());
        verifyNoInteractions(proveedorIA);
    }

    @Test
    void generar_factorInexistenteODeOtraCompetencia_informa() {
        Factor deOtra = Datos.factor(21L, Datos.competencia(2L, "COMP-LE", "LEALTAD"), "FAC-LE-01", "COMUNICACION", 1);
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.findById(21L)).thenReturn(Optional.of(deOtra));

        assertEquals("El factor no pertenece a la competencia seleccionada",
                assertThrows(BusinessException.class, () -> service.generar(1L, 21L)).getMessage());
        assertEquals("El factor seleccionado no existe",
                assertThrows(BusinessException.class, () -> service.generar(1L, 99L)).getMessage());
        verifyNoInteractions(proveedorIA);
    }

    @Test
    void generar_sinOpcionesDeRespuestaCargadas_informa() {
        when(competenciaDao.findById(1L)).thenReturn(Optional.of(gp));
        when(factorDao.findById(11L)).thenReturn(Optional.of(planificacion));
        when(opcionRespuestaDao.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class, () -> service.generar(1L, 11L));

        assertTrue(e.getMessage().startsWith("No hay opciones de respuesta cargadas"));
        verifyNoInteractions(proveedorIA);
    }

    // errores del servicio de IA

    @Test
    void generar_elProveedorFalla_informaElMensajeDelCasoDeUsoConLaCausa() {
        datosValidos();
        when(proveedorIA.generarJson(anyString(), any(ObjectNode.class)))
                .thenThrow(new ServicioIAException("Se alcanzó el límite de consultas del plan gratuito de Gemini."));

        BusinessException e = assertThrows(BusinessException.class, () -> service.generar(1L, 11L));

        assertEquals(GeneradorPreguntaIAService.MENSAJE_ERROR_IA, e.getMessage());
        assertEquals(List.of("Se alcanzó el límite de consultas del plan gratuito de Gemini."), e.getDetalles());
    }

    @Test
    void generar_respuestaInvalidaDeLaIA_informaLaCausa() {
        datosValidos();
        when(proveedorIA.generarJson(anyString(), any(ObjectNode.class))).thenReturn(
                "No puedo ayudarte con eso",
                "{\"nombre\": \"\", \"pregunta\": \"P\", \"opcionRespuesta\": \"VERDADERO-FALSO\"}",
                "{\"nombre\": \"N\", \"pregunta\": \"P\", \"opcionRespuesta\": \"ESCALA-INVENTADA\"}");

        assertEquals(List.of("La respuesta del servicio de IA no tiene el formato esperado."),
                assertThrows(BusinessException.class, () -> service.generar(1L, 11L)).getDetalles());
        assertEquals(List.of("El servicio de IA no devolvió el nombre o el texto de la pregunta."),
                assertThrows(BusinessException.class, () -> service.generar(1L, 11L)).getDetalles());
        BusinessException e = assertThrows(BusinessException.class, () -> service.generar(1L, 11L));
        assertEquals(GeneradorPreguntaIAService.MENSAJE_ERROR_IA, e.getMessage());
        assertFalse(e.getDetalles().isEmpty());
        assertTrue(e.getDetalles().get(0).contains("ESCALA-INVENTADA"));
    }
}
