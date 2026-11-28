package ar.edu.utn.frsf.isi.capitalhumano.service;

/** error al comunicarse con el servicio de IA (CU033), el mensaje explica la causa */
public class ServicioIAException extends RuntimeException {

    public ServicioIAException(String mensaje) {
        super(mensaje);
    }
}
