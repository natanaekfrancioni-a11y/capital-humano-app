package ar.edu.utn.frsf.isi.capitalhumano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "detalle_opcion_respuesta")
public class DetalleOpcionRespuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcion_respuesta_id", nullable = false)
    private OpcionRespuesta opcionRespuesta;

    @Column(name = "nro_orden", nullable = false)
    private Integer nroOrden;

    @Column(nullable = false, length = 120)
    private String descripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OpcionRespuesta getOpcionRespuesta() { return opcionRespuesta; }
    public void setOpcionRespuesta(OpcionRespuesta opcionRespuesta) { this.opcionRespuesta = opcionRespuesta; }
    public Integer getNroOrden() { return nroOrden; }
    public void setNroOrden(Integer nroOrden) { this.nroOrden = nroOrden; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
