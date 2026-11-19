package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

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

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "factor_id", nullable = false)
    private Factor factor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    @Column(name = "nro_bloque", nullable = false)
    private Integer nroBloque;

    @Column(name = "orden_visual", nullable = false)
    private Integer ordenVisual;

    @Column(nullable = false)
    private boolean respondida = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cuestionario getCuestionario() { return cuestionario; }
    public void setCuestionario(Cuestionario cuestionario) { this.cuestionario = cuestionario; }
    public Pregunta getPregunta() { return pregunta; }
    public void setPregunta(Pregunta pregunta) { this.pregunta = pregunta; }
    public Factor getFactor() { return factor; }
    public void setFactor(Factor factor) { this.factor = factor; }
    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { this.competencia = competencia; }
    public Integer getNroBloque() { return nroBloque; }
    public void setNroBloque(Integer nroBloque) { this.nroBloque = nroBloque; }
    public Integer getOrdenVisual() { return ordenVisual; }
    public void setOrdenVisual(Integer ordenVisual) { this.ordenVisual = ordenVisual; }
    public boolean isRespondida() { return respondida; }
    public void setRespondida(boolean respondida) { this.respondida = respondida; }
}
