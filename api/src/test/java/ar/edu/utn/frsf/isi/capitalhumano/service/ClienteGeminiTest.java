package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Estrategia concreta de CU033 (Gemini). En lugar de internet se usa un servidor HTTP local que hace
 * de API de Gemini: así se prueba el pedido que se arma y cada respuesta posible, sin clave real.
 */
class ClienteGeminiTest {

    private record Respuesta(int status, String cuerpo, long demoraMs) {}

    private record Pedido(String ruta, String clave, JsonNode cuerpo) {}

    private final ObjectMapper mapper = new ObjectMapper();
    private final Queue<Respuesta> aResponder = new ConcurrentLinkedQueue<>();
    private final List<Pedido> recibidos = new CopyOnWriteArrayList<>();
    private HttpServer servidor;
    private ExecutorService hilos;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        hilos = Executors.newCachedThreadPool();
        servidor.setExecutor(hilos);
        servidor.createContext("/", intercambio -> {
            byte[] entrada = intercambio.getRequestBody().readAllBytes();
            recibidos.add(new Pedido(intercambio.getRequestURI().getPath(),
                    intercambio.getRequestHeaders().getFirst("x-goog-api-key"), mapper.readTree(entrada)));
            Respuesta r = aResponder.isEmpty() ? new Respuesta(500, "", 0) : aResponder.poll();
            if (r.demoraMs() > 0) {
                try {
                    Thread.sleep(r.demoraMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] salida = r.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(r.status(), salida.length == 0 ? -1 : salida.length);
            try (OutputStream os = intercambio.getResponseBody()) {
                os.write(salida);
            }
        });
        servidor.start();
    }

    @AfterEach
    void bajarServidor() {
        servidor.stop(0);
        hilos.shutdownNow();
    }

    private ClienteGemini cliente(String clave, int segundosEspera) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta/";
        return new ClienteGemini(mapper, clave, "modelo-prueba", url, segundosEspera);
    }

    private ClienteGemini cliente() {
        return cliente("clave-de-prueba", 5);
    }

    private void responder(int status, String cuerpo) {
        aResponder.add(new Respuesta(status, cuerpo, 0));
    }

    private ObjectNode esquema() {
        ObjectNode e = mapper.createObjectNode();
        e.put("type", "object");
        return e;
    }

    private static String textoGenerado(String... partes) {
        StringBuilder sb = new StringBuilder("{\"candidates\":[{\"content\":{\"parts\":[");
        for (int i = 0; i < partes.length; i++) {
            sb.append(i == 0 ? "" : ",").append(partes[i]);
        }
        return sb.append("]}}]}").toString();
    }

    private String error(int status, String mensaje) {
        responder(status, "{\"error\":{\"message\":\"" + mensaje + "\"}}");
        return assertThrows(ServicioIAException.class, () -> cliente().generarJson("prompt", esquema())).getMessage();
    }

    @Test
    void generarJson_armaElPedidoConElEsquemaYDevuelveElTextoSinElRazonamiento() {
        responder(200, textoGenerado("{\"text\":\"pensando...\",\"thought\":true}", "{\"text\":\"{\\\"nombre\\\": \"}", "{\"text\":\"\\\"X\\\"}\"}"));

        String json = cliente().generarJson("Generá una pregunta", esquema());

        assertEquals("{\"nombre\": \"X\"}", json);
        Pedido pedido = recibidos.get(0);
        assertEquals("/v1beta/models/modelo-prueba:generateContent", pedido.ruta());
        assertEquals("clave-de-prueba", pedido.clave());
        assertEquals("Generá una pregunta", pedido.cuerpo().path("contents").path(0).path("parts").path(0).path("text").asText());
        JsonNode formato = pedido.cuerpo().path("generationConfig").path("responseFormat").path("text");
        assertEquals("application/json", formato.path("mimeType").asText());
        assertEquals("object", formato.path("schema").path("type").asText());
    }

    @Test
    void generarJson_siRechazaLaSalidaEstructurada_reintentaUnaVezSinElEsquema() {
        responder(400, "{\"error\":{\"message\":\"responseFormat no soportado\"}}");
        responder(200, textoGenerado("{\"text\":\"{}\"}"));

        assertEquals("{}", cliente().generarJson("prompt", esquema()));

        assertEquals(2, recibidos.size());
        assertTrue(recibidos.get(0).cuerpo().has("generationConfig"));
        assertFalse(recibidos.get(1).cuerpo().has("generationConfig"));
    }

    @Test
    void generarJson_sinClaveConfigurada_noLlamaAlServicio() {
        ServicioIAException e = assertThrows(ServicioIAException.class, () -> cliente("  ", 5).generarJson("p", esquema()));

        assertTrue(e.getMessage().startsWith("Falta configurar la clave de la API de Gemini"));
        assertTrue(recibidos.isEmpty());
        assertThrows(ServicioIAException.class, () -> cliente(null, 5).generarJson("p", esquema()));
    }

    @Test
    void generarJson_explicaCadaErrorDeLaApiEnPalabrasSimples() {
        assertEquals("La clave de la API de Gemini no es válida. Revise gemini.api.key en application.properties.",
                error(400, "API key not valid. Please pass a valid API key. API_KEY_INVALID"));
        assertEquals(1, recibidos.size(), "con la clave inválida no se reintenta");
        assertEquals("La clave de la API de Gemini no es válida. Revise gemini.api.key en application.properties.",
                error(401, "Unauthorized"));
        assertEquals("La clave no tiene permiso para usar la API de Gemini (Permission denied).", error(403, "Permission denied"));
        assertTrue(error(404, "not found").startsWith("El modelo modelo-prueba no existe"));
        assertTrue(error(429, "quota").startsWith("Se alcanzó el límite de consultas"));
        assertEquals("El servicio de Gemini no está disponible en este momento (código 503).", error(503, "overloaded"));
        assertEquals("El servicio de IA rechazó el pedido (código 418: sin detalle).", error(418, ""));
        String largo = error(409, "x".repeat(250));
        assertTrue(largo.endsWith("…).") && largo.length() < 300, largo);
    }

    @Test
    void generarJson_respuestaSinTexto_informaElMotivo() {
        responder(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        responder(200, "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[]}}]}");
        responder(200, "esto no es JSON");

        assertEquals("El servicio de IA no devolvió una pregunta (motivo: SAFETY).",
                assertThrows(ServicioIAException.class, () -> cliente().generarJson("p", esquema())).getMessage());
        assertEquals("El servicio de IA no devolvió una pregunta (motivo: MAX_TOKENS).",
                assertThrows(ServicioIAException.class, () -> cliente().generarJson("p", esquema())).getMessage());
        assertEquals("El servicio de IA no devolvió una pregunta (motivo: SIN CONTENIDO).",
                assertThrows(ServicioIAException.class, () -> cliente().generarJson("p", esquema())).getMessage());
    }

    @Test
    void generarJson_servicioQueNoContestaATiempo_informa() {
        aResponder.add(new Respuesta(200, textoGenerado("{\"text\":\"tarde\"}"), 2_500));

        ServicioIAException e = assertThrows(ServicioIAException.class, () -> cliente("clave", 1).generarJson("p", esquema()));

        assertEquals("El servicio de IA no respondió a tiempo.", e.getMessage());
    }

    @Test
    void generarJson_sinConexion_informa() {
        ClienteGemini sinServidor = cliente();
        servidor.stop(0);

        ServicioIAException e = assertThrows(ServicioIAException.class, () -> sinServidor.generarJson("p", esquema()));

        assertEquals("No se pudo conectar con el servicio de IA. Revise la conexión a internet.", e.getMessage());
    }

    @Test
    void generarJson_hiloInterrumpido_informaYConservaLaInterrupcion() {
        Thread.currentThread().interrupt();
        try {
            ServicioIAException e = assertThrows(ServicioIAException.class, () -> cliente().generarJson("p", esquema()));
            assertEquals("Se interrumpió la comunicación con el servicio de IA.", e.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void urlInvalida_informaQueNoSePudoArmarElPedido() {
        ClienteGemini mal = new ClienteGemini(mapper, "clave", "modelo", "http://url con espacios", 5);

        ServicioIAException e = assertThrows(ServicioIAException.class, () -> mal.generarJson("p", esquema()));

        assertTrue(e.getMessage().startsWith("No se pudo armar el pedido al servicio de IA"));
    }

    @Test
    void modelo_porDefectoSiNoSeConfigura() {
        assertEquals("gemini-3.5-flash-lite", new ClienteGemini(mapper, "c", " ", "http://localhost", 5).getModelo());
        assertEquals("gemini-3.5-flash-lite", new ClienteGemini(mapper, "c", null, "http://localhost", 5).getModelo());
        assertEquals("otro-modelo", new ClienteGemini(mapper, "c", " otro-modelo ", "http://localhost/", 5).getModelo());
    }
}
