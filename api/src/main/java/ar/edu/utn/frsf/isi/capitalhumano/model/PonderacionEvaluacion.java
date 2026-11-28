package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

/** ponderación de una opción de respuesta en la copia de la pregunta que guarda la evaluación */
@Entity
@Table(name = "ponderacion_evaluacion")
public class PonderacionEvaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_evaluacion_id", nullable = false)
    private PreguntaEvaluacion preguntaEvaluacion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "detalle_opcion_respuesta_id", nullable = false)
    private DetalleOpcionRespuesta detalleOpcionRespuesta;

    @Column(nullable = false)
    private BigDecimal ponderacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PreguntaEvaluacion getPreguntaEvaluacion() { return preguntaEvaluacion; }
    public void setPreguntaEvaluacion(PreguntaEvaluacion preguntaEvaluacion) { this.preguntaEvaluacion = preguntaEvaluacion; }
    public DetalleOpcionRespuesta getDetalleOpcionRespuesta() { return detalleOpcionRespuesta; }
    public void setDetalleOpcionRespuesta(DetalleOpcionRespuesta detalleOpcionRespuesta) { this.detalleOpcionRespuesta = detalleOpcionRespuesta; }
    public BigDecimal getPonderacion() { return ponderacion; }
    public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
}
