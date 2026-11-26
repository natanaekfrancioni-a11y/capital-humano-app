package ar.edu.utn.frsf.isi.capitalhumano.service.estado;

/** SIN_CONTESTAR: venció el plazo para empezarlo. Es final. */
public class EstadoSinContestar extends EstadoBase {

    @Override
    public String nombre() {
        return "SIN_CONTESTAR";
    }
}
