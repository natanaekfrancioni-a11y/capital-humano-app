package ar.edu.utn.frsf.isi.capitalhumano.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_cuestionario")
public class ConfiguracionCuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preguntas_por_bloque", nullable = false)
    private Integer preguntasPorBloque = 2;

    @Column(name = "max_accesos", nullable = false)
    private Integer maxAccesos = 5;

    @Column(name = "tiempo_max_minutos", nullable = false)
    private Integer tiempoMaxMinutos = 90;

    @Column(name = "dias_vencimiento_activo", nullable = false)
    private Integer diasVencimientoActivo = 30;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instrucciones;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPreguntasPorBloque() { return preguntasPorBloque; }
    public void setPreguntasPorBloque(Integer preguntasPorBloque) { this.preguntasPorBloque = preguntasPorBloque; }
    public Integer getMaxAccesos() { return maxAccesos; }
    public void setMaxAccesos(Integer maxAccesos) { this.maxAccesos = maxAccesos; }
    public Integer getTiempoMaxMinutos() { return tiempoMaxMinutos; }
    public void setTiempoMaxMinutos(Integer tiempoMaxMinutos) { this.tiempoMaxMinutos = tiempoMaxMinutos; }
    public Integer getDiasVencimientoActivo() { return diasVencimientoActivo; }
    public void setDiasVencimientoActivo(Integer diasVencimientoActivo) { this.diasVencimientoActivo = diasVencimientoActivo; }
    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
