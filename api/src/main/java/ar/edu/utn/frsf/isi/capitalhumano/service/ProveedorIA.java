package ar.edu.utn.frsf.isi.capitalhumano.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** patrón Strategy: servicio externo de IA que usa el CU033 */
public interface ProveedorIA {

    /** manda el pedido y devuelve como texto el JSON que generó el modelo */
    String generarJson(String prompt, ObjectNode esquema);
}
