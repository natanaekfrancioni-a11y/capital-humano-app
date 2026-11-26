package ar.edu.utn.frsf.isi.capitalhumano.service;

/** Falla al comunicarse con el servicio de IA (CU033, flujo 4.A). El mensaje explica la causa en palabras simples. */
public class ServicioIAException extends RuntimeException {

    public ServicioIAException(String mensaje) {
        super(mensaje);
    }
}
