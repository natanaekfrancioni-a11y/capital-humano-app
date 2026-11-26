package ar.edu.utn.frsf.isi.capitalhumano.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cuestionario")
public class Cuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidato candidato;

    /** Evaluación (CU025) a la que pertenece: de ella toma el puesto y el modelo de preguntas. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;

    @Column(name = "clave_acceso", nullable = false, length = 8)
    private String claveAcceso;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";

    @Column(name = "bloque_actual", nullable = false)
    private Integer bloqueActual = 1;

    @Column(name = "cantidad_accesos", nullable = false)
    private Integer cantidadAccesos = 0;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "fecha_cambio_estado")
    private LocalDateTime fechaCambioEstado;

    @Column(name = "puntaje_total")
    private BigDecimal puntajeTotal;

    @OneToMany(mappedBy = "cuestionario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("ordenVisual ASC")
    private List<PreguntaCuestionario> preguntas = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Candidato getCandidato() { return candidato; }
    public void setCandidato(Candidato candidato) { this.candidato = candidato; }
    public Evaluacion getEvaluacion() { return evaluacion; }
    public void setEvaluacion(Evaluacion evaluacion) { this.evaluacion = evaluacion; }
    /** Puesto para el que se evalúa: es el de su evaluación. */
    public Puesto getPuesto() { return evaluacion == null ? null : evaluacion.getPuesto(); }
    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getBloqueActual() { return bloqueActual; }
    public void setBloqueActual(Integer bloqueActual) { this.bloqueActual = bloqueActual; }
    public Integer getCantidadAccesos() { return cantidadAccesos; }
    public void setCantidadAccesos(Integer cantidadAccesos) { this.cantidadAccesos = cantidadAccesos; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaUltimoAcceso() { return fechaUltimoAcceso; }
    public void setFechaUltimoAcceso(LocalDateTime fechaUltimoAcceso) { this.fechaUltimoAcceso = fechaUltimoAcceso; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public LocalDateTime getFechaCambioEstado() { return fechaCambioEstado; }
    public void setFechaCambioEstado(LocalDateTime fechaCambioEstado) { this.fechaCambioEstado = fechaCambioEstado; }
    public BigDecimal getPuntajeTotal() { return puntajeTotal; }
    public void setPuntajeTotal(BigDecimal puntajeTotal) { this.puntajeTotal = puntajeTotal; }
    public List<PreguntaCuestionario> getPreguntas() { return preguntas; }
    public void setPreguntas(List<PreguntaCuestionario> preguntas) { this.preguntas = preguntas; }
}
