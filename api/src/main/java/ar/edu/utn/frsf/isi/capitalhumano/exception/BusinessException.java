package ar.edu.utn.frsf.isi.capitalhumano.exception;

import java.util.Collections;
import java.util.List;

public class BusinessException extends RuntimeException {

    private final List<String> detalles;

    public BusinessException(String message) {
        super(message);
        this.detalles = List.of(message);
    }

    public BusinessException(String message, List<String> detalles) {
        super(message);
        this.detalles = (detalles == null || detalles.isEmpty()) ? List.of(message) : detalles;
    }

    public List<String> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }
}
