package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluación generada en el CU025 para un puesto: agrupa los cuestionarios que se crearon juntos
 * al presionar Finalizar y guarda el modelo con el que se arman todos ellos.
 *
 * <p>Requerimiento especial del CU025: "al momento de generarse la evaluación, se debe garantizar que
 * los cuestionarios generados para los candidatos se basen en el mismo modelo (competencias, factores,
 * preguntas, opciones de respuestas)". Por eso la evaluación guarda una copia de cada pregunta que se
 * puede sortear ({@link PreguntaEvaluacion}), con su factor, su texto y la ponderación de cada opción.
 * Si después se modifica el catálogo, los cuestionarios de la evaluación no cambian.
 *
 * <p>En el CU028 se elige por evaluación: se identifica por su número dentro del puesto (EVA-001, EVA-002...).
 */
@Entity
@Table(name = "evaluacion")
public class Evaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;

    /** Número correlativo dentro del puesto: 1, 2, 3... */
    @Column(name = "nro_evaluacion", nullable = false)
    private Integer nroEvaluacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Modelo congelado: las preguntas que se pueden sortear en los cuestionarios de esta evaluación. */
    @JsonIgnore
    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PreguntaEvaluacion> preguntas = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<Cuestionario> cuestionarios = new ArrayList<>();

    /** Código con el que se muestra la evaluación en el CU028: EVA-001, EVA-002, ... */
    public String getCodigo() {
        return nroEvaluacion == null ? null : String.format("EVA-%03d", nroEvaluacion);
    }

    public void agregarPregunta(PreguntaEvaluacion pregunta) {
        pregunta.setEvaluacion(this);
        preguntas.add(pregunta);
    }

    public void agregarCuestionario(Cuestionario cuestionario) {
        cuestionario.setEvaluacion(this);
        cuestionarios.add(cuestionario);
    }

    /**
     * Competencias que evalúa: las de los factores de las preguntas congeladas (cada pregunta pertenece a
     * un factor y cada factor a una competencia). Son las que tenía el puesto al generar la evaluación.
     */
    @JsonIgnore
    public List<Competencia> getCompetenciasEvaluadas() {
        Map<Long, Competencia> porId = new LinkedHashMap<>();
        for (PreguntaEvaluacion p : preguntas) {
            porId.putIfAbsent(p.getCompetencia().getId(), p.getCompetencia());
        }
        return new ArrayList<>(porId.values());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Puesto getPuesto() { return puesto; }
    public void setPuesto(Puesto puesto) { this.puesto = puesto; }
    public Integer getNroEvaluacion() { return nroEvaluacion; }
    public void setNroEvaluacion(Integer nroEvaluacion) { this.nroEvaluacion = nroEvaluacion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public List<PreguntaEvaluacion> getPreguntas() { return preguntas; }
    public void setPreguntas(List<PreguntaEvaluacion> preguntas) { this.preguntas = preguntas; }
    public List<Cuestionario> getCuestionarios() { return cuestionarios; }
    public void setCuestionarios(List<Cuestionario> cuestionarios) { this.cuestionarios = cuestionarios; }
}
