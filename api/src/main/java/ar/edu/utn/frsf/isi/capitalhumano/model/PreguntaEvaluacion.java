package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "pregunta_evaluacion")
public class PreguntaEvaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;

    /** factor de la pregunta (y por el factor, la competencia) */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "factor_id", nullable = false)
    private Factor factor;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    /** una ponderación por cada opción de respuesta, como estaba al generar la evaluación */
    @JsonIgnore
    @OneToMany(mappedBy = "preguntaEvaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PonderacionEvaluacion> ponderaciones = new ArrayList<>();

    /** copia la pregunta como está en el catálogo: factor, nombre, texto y ponderación de cada opción */
    public static PreguntaEvaluacion copiaDe(Pregunta pregunta) {
        PreguntaEvaluacion copia = new PreguntaEvaluacion();
        copia.setFactor(pregunta.getFactor());
        copia.setNombre(pregunta.getNombre());
        copia.setEnunciado(pregunta.getEnunciado());
        for (DetalleOpcionRespuesta opcion : pregunta.getOpcionRespuesta().getDetalles()) {
            PonderacionEvaluacion p = new PonderacionEvaluacion();
            p.setDetalleOpcionRespuesta(opcion);
            p.setPonderacion(pregunta.getPonderacionesPorDetalle().getOrDefault(opcion.getId(), BigDecimal.ZERO));
            copia.agregarPonderacion(p);
        }
        return copia;
    }

    public void agregarPonderacion(PonderacionEvaluacion ponderacion) {
        ponderacion.setPreguntaEvaluacion(this);
        ponderaciones.add(ponderacion);
    }

    /** la opción elegida, si es una de las opciones de esta pregunta */
    public Optional<PonderacionEvaluacion> buscarOpcion(Long detalleOpcionRespuestaId) {
        return ponderaciones.stream()
                .filter(p -> p.getDetalleOpcionRespuesta().getId().equals(detalleOpcionRespuestaId))
                .findFirst();
    }

    @JsonIgnore
    public Competencia getCompetencia() {
        return factor.getCompetencia();
    }

    /** lo que ve el candidato en el CU026: las opciones en orden y sin ponderaciones */
    @JsonProperty("opcionRespuesta")
    public OpcionesParaResponder getOpcionesParaResponder() {
        return new OpcionesParaResponder(ponderaciones.stream()
                .map(PonderacionEvaluacion::getDetalleOpcionRespuesta)
                .sorted(Comparator.comparing(DetalleOpcionRespuesta::getNroOrden))
                .toList());
    }

    public record OpcionesParaResponder(List<DetalleOpcionRespuesta> detalles) {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Evaluacion getEvaluacion() { return evaluacion; }
    public void setEvaluacion(Evaluacion evaluacion) { this.evaluacion = evaluacion; }
    public Factor getFactor() { return factor; }
    public void setFactor(Factor factor) { this.factor = factor; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public List<PonderacionEvaluacion> getPonderaciones() { return ponderaciones; }
    public void setPonderaciones(List<PonderacionEvaluacion> ponderaciones) { this.ponderaciones = ponderaciones; }
}
