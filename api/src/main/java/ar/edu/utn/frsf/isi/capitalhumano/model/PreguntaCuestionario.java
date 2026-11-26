package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/** Pregunta sorteada para un cuestionario (CU026), con su bloque y su orden en pantalla. */
@Entity
@Table(name = "pregunta_cuestionario")
public class PreguntaCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuestionario_id", nullable = false)
    private Cuestionario cuestionario;

    /**
     * Pregunta del modelo congelado de la evaluación (no la del catálogo): el texto y las opciones que
     * ve el candidato, y las ponderaciones con las que se calcula el puntaje, son las del momento en que
     * se generó la evaluación. Se publica como "pregunta" para la pantalla del cuestionario.
     */
    @JsonProperty("pregunta")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pregunta_evaluacion_id", nullable = false)
    private PreguntaEvaluacion preguntaEvaluacion;

    @Column(name = "nro_bloque", nullable = false)
    private Integer nroBloque;

    @Column(name = "orden_visual", nullable = false)
    private Integer ordenVisual;

    @Column(nullable = false)
    private boolean respondida = false;

    @JsonIgnore
    public Factor getFactor() { return preguntaEvaluacion.getFactor(); }

    @JsonIgnore
    public Competencia getCompetencia() { return preguntaEvaluacion.getCompetencia(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cuestionario getCuestionario() { return cuestionario; }
    public void setCuestionario(Cuestionario cuestionario) { this.cuestionario = cuestionario; }
    public PreguntaEvaluacion getPreguntaEvaluacion() { return preguntaEvaluacion; }
    public void setPreguntaEvaluacion(PreguntaEvaluacion preguntaEvaluacion) { this.preguntaEvaluacion = preguntaEvaluacion; }
    public Integer getNroBloque() { return nroBloque; }
    public void setNroBloque(Integer nroBloque) { this.nroBloque = nroBloque; }
    public Integer getOrdenVisual() { return ordenVisual; }
    public void setOrdenVisual(Integer ordenVisual) { this.ordenVisual = ordenVisual; }
    public boolean isRespondida() { return respondida; }
    public void setRespondida(boolean respondida) { this.respondida = respondida; }
}
