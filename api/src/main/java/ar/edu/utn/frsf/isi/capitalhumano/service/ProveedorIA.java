package ar.edu.utn.frsf.isi.capitalhumano.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Patrón Strategy: el servicio externo de IA que usa CU033.
 *
 * {@link GeneradorPreguntaIAService} (el contexto) arma el pedido y valida la respuesta, pero no sabe
 * qué proveedor la genera: solo conoce esta interfaz. Hoy la estrategia concreta es {@link ClienteGemini};
 * usar otro proveedor (OpenAI, Claude, un modelo local) es agregar otra clase que la implemente, sin tocar
 * el servicio. En los tests se usa una estrategia falsa, así no hace falta conexión ni clave.
 */
public interface ProveedorIA {

    /**
     * Envía el pedido y devuelve, como texto, el JSON que generó el modelo.
     *
     * @param prompt  instrucciones para el modelo
     * @param esquema esquema JSON que debe cumplir la respuesta
     * @throws ServicioIAException si no se pudo obtener una respuesta; el mensaje explica la causa
     */
    String generarJson(String prompt, ObjectNode esquema);
}
