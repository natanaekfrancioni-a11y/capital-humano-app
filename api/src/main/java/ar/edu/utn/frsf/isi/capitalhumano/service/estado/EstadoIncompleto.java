package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

/** INCOMPLETO: se pasó del tiempo máximo o de los accesos sin terminar, es final */
public class EstadoIncompleto extends EstadoBase {

    @Override
    public String nombre() {
        return "INCOMPLETO";
    }
}
