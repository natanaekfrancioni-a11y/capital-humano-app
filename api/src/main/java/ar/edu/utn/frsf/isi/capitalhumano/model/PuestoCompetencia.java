package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "puesto_competencia")
public class PuestoCompetencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "puesto_id", nullable = false)
    private Puesto puesto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    @Column(name = "ponderacion_minima", nullable = false)
    private BigDecimal ponderacionMinima;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Puesto getPuesto() { return puesto; }
    public void setPuesto(Puesto puesto) { this.puesto = puesto; }
    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { this.competencia = competencia; }
    public BigDecimal getPonderacionMinima() { return ponderacionMinima; }
    public void setPonderacionMinima(BigDecimal ponderacionMinima) { this.ponderacionMinima = ponderacionMinima; }
}
