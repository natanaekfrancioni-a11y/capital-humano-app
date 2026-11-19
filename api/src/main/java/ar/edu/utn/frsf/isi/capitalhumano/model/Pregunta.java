package ar.edu.utn.frsf.isi.capitalhumano.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "pregunta")
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "factor_id", nullable = false)
    private Factor factor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "opcion_respuesta_id", nullable = false)
    private OpcionRespuesta opcionRespuesta;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ponderacion_opcion_pregunta", joinColumns = @JoinColumn(name = "pregunta_id"))
    @MapKeyColumn(name = "detalle_opcion_respuesta_id")
    @Column(name = "ponderacion", nullable = false)
    private Map<Long, BigDecimal> ponderacionesPorDetalle = new HashMap<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Factor getFactor() { return factor; }
    public void setFactor(Factor factor) { this.factor = factor; }
    public OpcionRespuesta getOpcionRespuesta() { return opcionRespuesta; }
    public void setOpcionRespuesta(OpcionRespuesta opcionRespuesta) { this.opcionRespuesta = opcionRespuesta; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public Map<Long, BigDecimal> getPonderacionesPorDetalle() { return ponderacionesPorDetalle; }
    public void setPonderacionesPorDetalle(Map<Long, BigDecimal> ponderacionesPorDetalle) { this.ponderacionesPorDetalle = ponderacionesPorDetalle; }
}
