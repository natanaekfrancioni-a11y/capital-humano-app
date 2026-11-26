package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.DetalleOpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Evaluacion;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.PonderacionEvaluacion;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaEvaluacion;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.PuestoCompetencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de prueba para los tests de la capa de servicio. Arma las entidades en memoria (sin base de
 * datos) con lo mínimo que usa cada caso de uso. No es un test: lo usan las clases *Test.
 */
public final class Datos {

    private Datos() {
    }

    public static Candidato candidato(long id, String nroCandidato, String apellido, String nombre) {
        Candidato c = new Candidato();
        c.setId(id);
        c.setNroCandidato(nroCandidato);
        c.setApellido(apellido);
        c.setNombre(nombre);
        c.setTipoDocumento("DNI");
        c.setNroDocumento(String.valueOf(30_000_000L + id));
        return c;
    }

    public static Competencia competencia(long id, String codigo, String nombre) {
        Competencia c = new Competencia();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setDescripcion("DESCRIPCION DE " + nombre);
        return c;
    }

    public static Factor factor(long id, Competencia competencia, String codigo, String nombre, int nroOrden) {
        Factor f = new Factor();
        f.setId(id);
        f.setCompetencia(competencia);
        f.setCodigo(codigo);
        f.setNombre(nombre);
        f.setDescripcion("DESCRIPCION DE " + nombre);
        f.setNroOrden(nroOrden);
        return f;
    }

    /** Opción de respuesta con sus opciones en orden 1, 2, 3...; el id de cada opción es id * 10 + orden. */
    public static OpcionRespuesta opcion(long id, String nombre, String... descripciones) {
        OpcionRespuesta op = new OpcionRespuesta();
        op.setId(id);
        op.setNombre(nombre);
        op.setDescripcion("DESCRIPCION DE " + nombre);
        for (int i = 0; i < descripciones.length; i++) {
            DetalleOpcionRespuesta d = new DetalleOpcionRespuesta();
            d.setId(id * 10 + i + 1);
            d.setOpcionRespuesta(op);
            d.setNroOrden(i + 1);
            d.setDescripcion(descripciones[i]);
            op.getDetalles().add(d);
        }
        return op;
    }

    /** Pregunta con una ponderación por cada opción, en el mismo orden que las opciones. */
    public static Pregunta pregunta(long id, Factor factor, OpcionRespuesta opcion, String nombre, String... ponderaciones) {
        Pregunta p = new Pregunta();
        p.setId(id);
        p.setFactor(factor);
        p.setOpcionRespuesta(opcion);
        p.setCodigo("PREG-" + id);
        p.setNombre(nombre);
        p.setEnunciado("ENUNCIADO DE " + nombre);
        for (int i = 0; i < ponderaciones.length; i++) {
            p.getPonderacionesPorDetalle().put(opcion.getDetalles().get(i).getId(), new BigDecimal(ponderaciones[i]));
        }
        return p;
    }

    public static Puesto puesto(long id, String codigo, String nombre, String empresa) {
        Puesto p = new Puesto();
        p.setId(id);
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setEmpresa(empresa);
        p.setDescripcion("DESCRIPCION DE " + nombre);
        return p;
    }

    /** Agrega al puesto la competencia con su ponderación mínima. */
    public static PuestoCompetencia exige(Puesto puesto, Competencia competencia, String ponderacionMinima) {
        PuestoCompetencia pc = new PuestoCompetencia();
        pc.setId(puesto.getId() * 100 + competencia.getId());
        pc.setPuesto(puesto);
        pc.setCompetencia(competencia);
        pc.setPonderacionMinima(new BigDecimal(ponderacionMinima));
        puesto.getCompetencias().add(pc);
        return pc;
    }

    /** Evaluación del CU025 para el puesto, todavía sin preguntas ni cuestionarios. */
    public static Evaluacion evaluacion(long id, Puesto puesto, int nroEvaluacion, LocalDateTime fechaCreacion) {
        Evaluacion ev = new Evaluacion();
        ev.setId(id);
        ev.setPuesto(puesto);
        ev.setNroEvaluacion(nroEvaluacion);
        ev.setFechaCreacion(fechaCreacion);
        return ev;
    }

    /**
     * Cuestionario de una evaluación nueva del puesto (EVA-001). Con puesto null queda sin evaluación,
     * para los casos que no la usan.
     */
    public static Cuestionario cuestionario(long id, Candidato candidato, Puesto puesto, String estado) {
        Evaluacion ev = puesto == null ? null : evaluacion(id, puesto, 1, LocalDateTime.of(2026, 11, 20, 9, 0, 0));
        return cuestionarioDe(id, candidato, ev, estado);
    }

    /** Cuestionario de la evaluación indicada (se agrega a sus cuestionarios). */
    public static Cuestionario cuestionarioDe(long id, Candidato candidato, Evaluacion evaluacion, String estado) {
        Cuestionario q = new Cuestionario();
        q.setId(id);
        q.setCandidato(candidato);
        q.setEstado(estado);
        q.setClaveAcceso("A8K3M9P2");
        q.setFechaCreacion(evaluacion == null ? LocalDateTime.of(2026, 11, 20, 9, 0, 0) : evaluacion.getFechaCreacion());
        if (evaluacion != null) {
            evaluacion.agregarCuestionario(q);
        }
        return q;
    }

    /**
     * Copia congelada de la pregunta (la que guarda la evaluación al generarse). Usa el id de la pregunta;
     * si se indica la evaluación, la agrega a su modelo.
     */
    public static PreguntaEvaluacion copia(Pregunta pregunta, Evaluacion evaluacion) {
        PreguntaEvaluacion pe = PreguntaEvaluacion.copiaDe(pregunta);
        pe.setId(pregunta.getId());
        long nro = 1;
        for (PonderacionEvaluacion p : pe.getPonderaciones()) {
            p.setId(pregunta.getId() * 100 + nro++);
        }
        if (evaluacion != null) {
            evaluacion.agregarPregunta(pe);
        }
        return pe;
    }

    /** Agrega al cuestionario la pregunta (copiada en el modelo de su evaluación), en el bloque indicado. */
    public static PreguntaCuestionario item(long id, Cuestionario cuestionario, Pregunta pregunta, int nroBloque) {
        return item(id, cuestionario, copia(pregunta, cuestionario.getEvaluacion()), nroBloque);
    }

    /** Agrega al cuestionario una pregunta del modelo congelado, en el bloque indicado. */
    public static PreguntaCuestionario item(long id, Cuestionario cuestionario, PreguntaEvaluacion pregunta, int nroBloque) {
        PreguntaCuestionario pq = new PreguntaCuestionario();
        pq.setId(id);
        pq.setCuestionario(cuestionario);
        pq.setPreguntaEvaluacion(pregunta);
        pq.setNroBloque(nroBloque);
        pq.setOrdenVisual(cuestionario.getPreguntas().size() + 1);
        cuestionario.getPreguntas().add(pq);
        return pq;
    }

    public static RespuestaCuestionario respuesta(PreguntaCuestionario pq, String ponderacion) {
        RespuestaCuestionario r = new RespuestaCuestionario();
        r.setId(pq.getId());
        r.setPreguntaCuestionario(pq);
        r.setPonderacionObtenida(new BigDecimal(ponderacion));
        return r;
    }

    public static ConfiguracionCuestionario configuracion(int preguntasPorBloque, int maxAccesos, int tiempoMaxMinutos, int diasVencimiento) {
        ConfiguracionCuestionario c = new ConfiguracionCuestionario();
        c.setId(1L);
        c.setPreguntasPorBloque(preguntasPorBloque);
        c.setMaxAccesos(maxAccesos);
        c.setTiempoMaxMinutos(tiempoMaxMinutos);
        c.setDiasVencimientoActivo(diasVencimiento);
        c.setInstrucciones("RESPONDA CADA PREGUNTA");
        return c;
    }
}
