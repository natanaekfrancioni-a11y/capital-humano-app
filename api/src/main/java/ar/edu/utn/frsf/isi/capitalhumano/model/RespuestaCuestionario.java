package ar.edu.utn.frsf.isi.capitalhumano.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "respuesta_cuestionario")
public class RespuestaCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_cuestionario_id", nullable = false, unique = true)
    private PreguntaCuestionario preguntaCuestionario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "detalle_opcion_respuesta_id", nullable = false)
    private DetalleOpcionRespuesta detalleOpcionRespuesta;

    @Column(name = "ponderacion_obtenida", nullable = false)
    private BigDecimal ponderacionObtenida;

    @Column(name = "respondida_en", nullable = false)
    private LocalDateTime respondidaEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PreguntaCuestionario getPreguntaCuestionario() { return preguntaCuestionario; }
    public void setPreguntaCuestionario(PreguntaCuestionario preguntaCuestionario) { this.preguntaCuestionario = preguntaCuestionario; }
    public DetalleOpcionRespuesta getDetalleOpcionRespuesta() { return detalleOpcionRespuesta; }
    public void setDetalleOpcionRespuesta(DetalleOpcionRespuesta detalleOpcionRespuesta) { this.detalleOpcionRespuesta = detalleOpcionRespuesta; }
    public BigDecimal getPonderacionObtenida() { return ponderacionObtenida; }
    public void setPonderacionObtenida(BigDecimal ponderacionObtenida) { this.ponderacionObtenida = ponderacionObtenida; }
    public LocalDateTime getRespondidaEn() { return respondidaEn; }
    public void setRespondidaEn(LocalDateTime respondidaEn) { this.respondidaEn = respondidaEn; }
}
