package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

/** INCOMPLETO: se superó el tiempo máximo o la cantidad de accesos sin terminar. Es final. */
public class EstadoIncompleto extends EstadoBase {

    @Override
    public String nombre() {
        return "INCOMPLETO";
    }
}
