package ar.edu.utn.frsf.isi.capitalhumano.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** estrategia concreta de ProveedorIA (patrón Strategy) con la API de Google Gemini */
@Component
public class ClienteGemini implements ProveedorIA {

    private static final Logger log = LoggerFactory.getLogger(ClienteGemini.class);
    private static final String MODELO_POR_DEFECTO = "gemini-3.5-flash-lite";

    private record Respuesta(int status, JsonNode json) {
        String mensajeError() {
            return json.path("error").path("message").asText("");
        }
    }

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String clave;
    private final String modelo;
    private final String urlBase;
    private final Duration espera;

    public ClienteGemini(
            ObjectMapper mapper,
            @Value("${gemini.api.key:}") String clave,
            @Value("${gemini.model:" + MODELO_POR_DEFECTO + "}") String modelo,
            @Value("${gemini.url:https://generativelanguage.googleapis.com/v1beta}") String urlBase,
            @Value("${gemini.timeout-segundos:60}") int segundosEspera) {
        this.mapper = mapper;
        this.clave = clave == null ? "" : clave.trim();
        this.modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim();
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.espera = Duration.ofSeconds(segundosEspera);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String getModelo() {
        return modelo;
    }

    /** pide la respuesta en JSON, si la API da error 400 reintenta una vez sin la salida estructurada */
    @Override
    public String generarJson(String prompt, ObjectNode esquema) {
        if (clave.isEmpty()) {
            throw new ServicioIAException(
                    "Falta configurar la clave de la API de Gemini (gemini.api.key en application.properties).");
        }
        Respuesta respuesta = enviar(armarCuerpo(prompt, esquema));
        if (respuesta.status() == 400 && !esClaveInvalida(respuesta)) {
            log.warn("Gemini rechazó el pedido con salida estructurada ({}). Se reintenta sin ella.", respuesta.mensajeError());
            respuesta = enviar(armarCuerpo(prompt, null));
        }
        if (respuesta.status() != 200) {
            throw new ServicioIAException(explicarError(respuesta));
        }
        return extraerTexto(respuesta.json());
    }

    private ObjectNode armarCuerpo(String prompt, ObjectNode esquema) {
        ObjectNode cuerpo = mapper.createObjectNode();
        ObjectNode contenido = cuerpo.putArray("contents").addObject();
        contenido.put("role", "user");
        contenido.putArray("parts").addObject().put("text", prompt);
        if (esquema != null) {
            ObjectNode formato = cuerpo.putObject("generationConfig").putObject("responseFormat").putObject("text");
            formato.put("mimeType", "application/json");
            formato.set("schema", esquema);
        }
        return cuerpo;
    }

    private Respuesta enviar(ObjectNode cuerpo) {
        HttpRequest pedido;
        try {
            pedido = HttpRequest.newBuilder(URI.create(urlBase + "/models/" + modelo + ":generateContent"))
                    .timeout(espera)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", clave)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(cuerpo), StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new ServicioIAException("No se pudo armar el pedido al servicio de IA: " + e.getMessage());
        }
        try {
            HttpResponse<String> res = http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Respuesta(res.statusCode(), leer(res.body()));
        } catch (HttpTimeoutException e) {
            throw new ServicioIAException("El servicio de IA no respondió a tiempo.");
        } catch (IOException e) {
            log.warn("No se pudo conectar con Gemini: {}", e.toString());
            throw new ServicioIAException("No se pudo conectar con el servicio de IA. Revise la conexión a internet.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioIAException("Se interrumpió la comunicación con el servicio de IA.");
        }
    }

    private JsonNode leer(String cuerpo) {
        try {
            return mapper.readTree(cuerpo == null ? "" : cuerpo);
        } catch (JsonProcessingException e) {
            return mapper.createObjectNode();
        }
    }

    private boolean esClaveInvalida(Respuesta r) {
        return r.json().toString().contains("API_KEY_INVALID") || r.mensajeError().toLowerCase().contains("api key");
    }

    private String explicarError(Respuesta r) {
        int status = r.status();
        String detalle = r.mensajeError();
        log.warn("Gemini respondió {}: {}", status, detalle);
        if (status == 401 || esClaveInvalida(r)) {
            return "La clave de la API de Gemini no es válida. Revise gemini.api.key en application.properties.";
        }
        if (status == 403) {
            return "La clave no tiene permiso para usar la API de Gemini (" + resumir(detalle) + ").";
        }
        if (status == 404) {
            return "El modelo " + modelo + " no existe o no está disponible para esta clave. "
                    + "Cambie gemini.model en application.properties.";
        }
        if (status == 429) {
            return "Se alcanzó el límite de consultas del plan gratuito de Gemini. Espere un minuto y vuelva a intentar; "
                    + "si se agotó el cupo del día, se renueva al día siguiente.";
        }
        if (status >= 500) {
            return "El servicio de Gemini no está disponible en este momento (código " + status + ").";
        }
        return "El servicio de IA rechazó el pedido (código " + status + ": " + resumir(detalle) + ").";
    }

    /** junta el texto de la primera respuesta del modelo sin las partes de razonamiento */
    private String extraerTexto(JsonNode json) {
        JsonNode candidato = json.path("candidates").path(0);
        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : candidato.path("content").path("parts")) {
            if (!parte.path("thought").asBoolean(false)) {
                texto.append(parte.path("text").asText(""));
            }
        }
        if (texto.toString().isBlank()) {
            String motivo = json.path("promptFeedback").path("blockReason").asText(candidato.path("finishReason").asText("SIN CONTENIDO"));
            throw new ServicioIAException("El servicio de IA no devolvió una pregunta (motivo: " + motivo + ").");
        }
        return texto.toString();
    }

    private static String resumir(String texto) {
        if (texto == null || texto.isBlank()) return "sin detalle";
        return texto.length() > 200 ? texto.substring(0, 200) + "…" : texto;
    }
}
