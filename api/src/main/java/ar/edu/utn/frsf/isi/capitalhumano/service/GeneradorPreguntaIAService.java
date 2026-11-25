package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CompetenciaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.OpcionRespuestaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.DetalleOpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * CU033 - Generar pregunta con Inteligencia Artificial (extiende a CU021 - Dar de alta pregunta).
 *
 * Arma el pedido con la competencia, el factor, las preguntas que ya tiene el factor y las opciones
 * de respuesta disponibles; lo envía al servicio de IA y devuelve los datos para completar el alta.
 * No guarda nada: el consultor revisa los datos y la pregunta se registra con el "Aceptar" de CU021,
 * que hace todas las validaciones de siempre (nombre y texto repetidos, ponderaciones que suman 10).
 */
@Service
public class GeneradorPreguntaIAService {

    /** Mensaje del flujo alternativo 4.A de CU033. */
    public static final String MENSAJE_ERROR_IA =
            "Ocurrió un error al comunicarse con el servicio de IA. Intente nuevamente más tarde";

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    // Los mismos máximos que la pantalla de alta de pregunta.
    private static final int MAX_NOMBRE = 100;
    private static final int MAX_PREGUNTA = 500;
    private static final int MAX_DESCRIPCION = 255;

    /**
     * Datos que completa la IA (paso 4). Las ponderaciones van por id de opción y solo vienen si
     * son válidas (0 o positivas y suman 10); si no, quedan vacías y las carga el consultor.
     */
    public record PreguntaGeneradaDTO(
            Long competenciaId,
            Long factorId,
            String nombre,
            String enunciado,
            String descripcion,
            Long opcionRespuestaId,
            Map<Long, BigDecimal> ponderaciones) {}

    private final CompetenciaDao competenciaDao;
    private final FactorDao factorDao;
    private final PreguntaDao preguntaDao;
    private final OpcionRespuestaDao opcionRespuestaDao;
    private final ClienteGemini clienteGemini;
    private final ObjectMapper mapper;

    public GeneradorPreguntaIAService(
            CompetenciaDao competenciaDao,
            FactorDao factorDao,
            PreguntaDao preguntaDao,
            OpcionRespuestaDao opcionRespuestaDao,
            ClienteGemini clienteGemini,
            ObjectMapper mapper) {
        this.competenciaDao = competenciaDao;
        this.factorDao = factorDao;
        this.preguntaDao = preguntaDao;
        this.opcionRespuestaDao = opcionRespuestaDao;
        this.clienteGemini = clienteGemini;
        this.mapper = mapper;
    }

    public PreguntaGeneradaDTO generar(Long competenciaId, Long factorId) {
        // Paso 2 y 2.A: competencia y factor son obligatorios.
        List<String> faltan = new ArrayList<>();
        if (competenciaId == null) faltan.add("La competencia es obligatoria");
        if (factorId == null) faltan.add("El factor es obligatorio");
        if (!faltan.isEmpty()) {
            throw new BusinessException("Faltan datos para generar la pregunta", faltan);
        }

        Competencia competencia = competenciaDao.findById(competenciaId)
                .filter(Competencia::isActiva)
                .orElseThrow(() -> new BusinessException("La competencia seleccionada no existe"));
        Factor factor = factorDao.findById(factorId)
                .filter(Factor::isActivo)
                .orElseThrow(() -> new BusinessException("El factor seleccionado no existe"));
        if (!factor.getCompetencia().getId().equals(competencia.getId())) {
            throw new BusinessException("El factor no pertenece a la competencia seleccionada");
        }
        List<OpcionRespuesta> opciones = opcionRespuestaDao.findByActivaTrueOrderByNombreAsc();
        if (opciones.isEmpty()) {
            throw new BusinessException("No hay opciones de respuesta cargadas: cree al menos una antes de generar preguntas");
        }
        List<Pregunta> existentes = preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(factor.getId());

        // Paso 3: se invoca el servicio externo de IA. 4.A: si falla, se informa con el mensaje del CU.
        String respuesta;
        try {
            respuesta = clienteGemini.generarJson(armarPrompt(competencia, factor, existentes, opciones), armarEsquema(opciones));
        } catch (ClienteGemini.ServicioIAException e) {
            throw new BusinessException(MENSAJE_ERROR_IA, List.of(e.getMessage()));
        }
        // Paso 4: datos que se cargan automáticamente en la pantalla de alta.
        return interpretar(respuesta, competencia, factor, opciones);
    }

    String armarPrompt(Competencia competencia, Factor factor, List<Pregunta> existentes, List<OpcionRespuesta> opciones) {
        StringBuilder p = new StringBuilder();
        p.append("Sos especialista en selección de personal de la consultora de recursos humanos \"Capit@l Humano\", ")
                .append("enfocada en el mercado TIC de Argentina. Escribís preguntas para cuestionarios que evalúan ")
                .append("competencias de candidatos a un puesto.\n\n")
                .append("Reglas del cuestionario:\n")
                .append("- Las preguntas son cerradas y tienen una única respuesta (multiple choice de una sola opción).\n")
                .append("- Cada pregunta usa una \"opción de respuesta\": un conjunto fijo de opciones. La pregunta tiene ")
                .append("que poder responderse con esas opciones exactas.\n")
                .append("- Cada opción recibe una ponderación de 0 o más puntos y las ponderaciones de una pregunta suman exactamente 10.\n")
                .append("- En una pregunta de conocimiento con una sola opción correcta, la correcta vale 10 y las demás 0. ")
                .append("En una pregunta de actitud o de autoevaluación, repartí los 10 puntos premiando las respuestas ")
                .append("que mejor muestran la competencia.\n\n");

        p.append("Competencia: ").append(competencia.getNombre())
                .append(" (").append("ACTITUDINAL".equalsIgnoreCase(competencia.getTipo()) ? "actitudinal" : "técnica").append(")");
        if (competencia.getDescripcion() != null && !competencia.getDescripcion().isBlank()) {
            p.append(": ").append(competencia.getDescripcion());
        }
        p.append("\nFactor a evaluar: ").append(factor.getNombre());
        if (factor.getDescripcion() != null && !factor.getDescripcion().isBlank()) {
            p.append(": ").append(factor.getDescripcion());
        }

        p.append("\n\nPreguntas que ya existen para este factor (no repitas ni parafrasees ninguna):\n");
        if (existentes.isEmpty()) {
            p.append("- Todavía no hay preguntas para este factor.\n");
        }
        for (Pregunta pregunta : existentes) {
            p.append("- ").append(pregunta.getNombre()).append(": ").append(pregunta.getEnunciado()).append('\n');
        }

        p.append("\nOpciones de respuesta disponibles (elegí exactamente una, por su nombre):\n");
        for (OpcionRespuesta opcion : opciones) {
            p.append("- ").append(opcion.getNombre()).append(" (").append(opcion.getDescripcion()).append("). Opciones: ");
            List<DetalleOpcionRespuesta> detalles = new ArrayList<>(opcion.getDetalles());
            detalles.sort(Comparator.comparing(DetalleOpcionRespuesta::getNroOrden));
            for (int i = 0; i < detalles.size(); i++) {
                if (i > 0) p.append("; ");
                p.append(detalles.get(i).getNroOrden()).append(") ").append(detalles.get(i).getDescripcion());
            }
            p.append('\n');
        }

        p.append("\nGenerá UNA pregunta nueva que evalúe este factor, en español, con tildes y signos de pregunta. ")
                .append("Si ninguna opción de respuesta es específica para el tema, elegí una genérica (por ejemplo, ")
                .append("verdadero o falso, frecuencia o valoración) y redactá la pregunta para que encaje con ella.\n\n")
                .append("Respondé solo con un objeto JSON con estas claves:\n")
                .append("- \"nombre\": nombre corto de la pregunta, de 2 a 6 palabras.\n")
                .append("- \"pregunta\": el texto que lee el candidato, de hasta 300 caracteres.\n")
                .append("- \"descripcion\": qué evalúa la pregunta, en hasta 150 caracteres.\n")
                .append("- \"opcionRespuesta\": el nombre exacto de la opción de respuesta elegida.\n")
                .append("- \"ponderaciones\": una entrada por cada opción de la opción de respuesta elegida, con \"orden\" ")
                .append("(el número de la opción) y \"ponderacion\" (un número). Tienen que sumar 10.\n");
        return p.toString();
    }

    ObjectNode armarEsquema(List<OpcionRespuesta> opciones) {
        ObjectNode esquema = mapper.createObjectNode();
        esquema.put("type", "object");
        ObjectNode propiedades = esquema.putObject("properties");
        propiedades.putObject("nombre").put("type", "string").put("description", "Nombre corto de la pregunta, de 2 a 6 palabras.");
        propiedades.putObject("pregunta").put("type", "string").put("description", "Texto que lee el candidato, de hasta 300 caracteres.");
        propiedades.putObject("descripcion").put("type", "string").put("description", "Qué evalúa la pregunta, en hasta 150 caracteres.");
        ObjectNode opcion = propiedades.putObject("opcionRespuesta");
        opcion.put("type", "string").put("description", "Nombre exacto de la opción de respuesta elegida.");
        ArrayNode nombres = opcion.putArray("enum");
        for (OpcionRespuesta o : opciones) {
            nombres.add(o.getNombre());
        }
        ObjectNode ponderaciones = propiedades.putObject("ponderaciones");
        ponderaciones.put("type", "array")
                .put("description", "Una entrada por cada opción de la opción de respuesta elegida. Las ponderaciones suman 10.");
        ObjectNode item = ponderaciones.putObject("items");
        item.put("type", "object");
        ObjectNode propiedadesItem = item.putObject("properties");
        propiedadesItem.putObject("orden").put("type", "integer");
        propiedadesItem.putObject("ponderacion").put("type", "number").put("minimum", 0).put("maximum", 10);
        item.putArray("required").add("orden").add("ponderacion");
        esquema.putArray("required").add("nombre").add("pregunta").add("descripcion").add("opcionRespuesta").add("ponderaciones");
        return esquema;
    }

    PreguntaGeneradaDTO interpretar(String texto, Competencia competencia, Factor factor, List<OpcionRespuesta> opciones) {
        JsonNode json;
        try {
            json = mapper.readTree(recortarJson(texto));
        } catch (Exception e) {
            throw errorIA("La respuesta del servicio de IA no tiene el formato esperado.");
        }
        String nombre = limpiar(json.path("nombre").asText(""), MAX_NOMBRE);
        String enunciado = limpiar(json.path("pregunta").asText(""), MAX_PREGUNTA);
        String descripcion = limpiar(json.path("descripcion").asText(""), MAX_DESCRIPCION);
        String nombreOpcion = json.path("opcionRespuesta").asText("").trim();
        if (nombre.isEmpty() || enunciado.isEmpty()) {
            throw errorIA("El servicio de IA no devolvió el nombre o el texto de la pregunta.");
        }
        OpcionRespuesta opcion = opciones.stream()
                .filter(o -> o.getNombre().equalsIgnoreCase(nombreOpcion))
                .findFirst()
                .orElseThrow(() -> errorIA("El servicio de IA eligió una opción de respuesta que no existe (" + nombreOpcion + ")."));
        return new PreguntaGeneradaDTO(
                competencia.getId(),
                factor.getId(),
                nombre,
                enunciado,
                descripcion.isEmpty() ? null : descripcion,
                opcion.getId(),
                ponderaciones(json.path("ponderaciones"), opcion));
    }

    /** Ponderaciones por id de opción. Si faltan o no suman 10, se devuelven vacías para que las cargue el consultor. */
    private Map<Long, BigDecimal> ponderaciones(JsonNode lista, OpcionRespuesta opcion) {
        Map<Integer, BigDecimal> porOrden = new HashMap<>();
        for (JsonNode item : lista) {
            BigDecimal valor = aNumero(item.path("ponderacion"));
            int orden = item.path("orden").asInt(-1);
            if (valor != null && orden > 0) {
                porOrden.put(orden, valor.setScale(2, RoundingMode.HALF_UP));
            }
        }
        Map<Long, BigDecimal> resultado = new LinkedHashMap<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleOpcionRespuesta detalle : opcion.getDetalles()) {
            BigDecimal valor = porOrden.get(detalle.getNroOrden());
            if (valor == null || valor.signum() < 0) {
                return Map.of();
            }
            resultado.put(detalle.getId(), valor);
            suma = suma.add(valor);
        }
        return suma.compareTo(BigDecimal.TEN) == 0 ? resultado : Map.of();
    }

    private static BigDecimal aNumero(JsonNode nodo) {
        if (nodo.isNumber()) return nodo.decimalValue();
        try {
            return new BigDecimal(nodo.asText("").trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Si el modelo envolvió el JSON en texto o en un bloque de código, se queda con el objeto. */
    private static String recortarJson(String texto) {
        int desde = texto.indexOf('{');
        int hasta = texto.lastIndexOf('}');
        return desde >= 0 && hasta > desde ? texto.substring(desde, hasta + 1) : texto;
    }

    /** El sistema guarda los textos siempre en mayúsculas. */
    private static String limpiar(String texto, int maximo) {
        String limpio = texto.replaceAll("\\s+", " ").trim().toUpperCase(ES_AR);
        return limpio.length() > maximo ? limpio.substring(0, maximo).trim() : limpio;
    }

    private static BusinessException errorIA(String causa) {
        return new BusinessException(MENSAJE_ERROR_IA, List.of(causa));
    }
}
