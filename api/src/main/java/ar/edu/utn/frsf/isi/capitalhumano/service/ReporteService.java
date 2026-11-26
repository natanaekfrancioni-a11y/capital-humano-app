package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.RespuestaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.PuestoCompetencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * GestorReportes: CU028 (Emitir orden de mérito).
 *
 * Una "evaluación" es el conjunto de cuestionarios generados juntos para un puesto en el CU025:
 * se identifican por su fecha de creación (al segundo) y se numeran EVA-001, EVA-002, ...
 * en orden cronológico dentro de cada puesto.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    public static final String TODAS = "TODAS";

    private static final List<String> ORDEN_ESTADOS_RESTANTES =
            List.of("EN_PROCESO", "ACTIVO", "INCOMPLETO", "SIN_CONTESTAR");

    private final PuestoDao puestoDao;
    private final CuestionarioDao cuestionarioDao;
    private final RespuestaCuestionarioDao respuestaDao;

    public ReporteService(
            PuestoDao puestoDao,
            CuestionarioDao cuestionarioDao,
            RespuestaCuestionarioDao respuestaDao
    ) {
        this.puestoDao = puestoDao;
        this.cuestionarioDao = cuestionarioDao;
        this.respuestaDao = respuestaDao;
    }

    // ------------------------------------------------------------------ DTOs

    public record PuestoResumenDTO(
            Long id, String codigo, String nombre, String empresa,
            long candidatos, long evaluacionesCompletas) {}

    public record EvaluacionDTO(String codigo, LocalDateTime fechaGeneracion, long candidatos, long completos) {}

    public record EvaluacionesPuestoDTO(
            Long puestoId, String puestoCodigo, String puestoNombre,
            long totalCandidatos, long totalCompletos, List<EvaluacionDTO> evaluaciones) {}

    public record CompetenciaRequeridaDTO(String nombre, BigDecimal ponderacionMinima) {}

    public record FilaOrdenMeritoDTO(
            Integer posicion, Long cuestionarioId, String tipoDocumento, String nroDocumento,
            String apellido, String nombre, String estado, BigDecimal puntaje,
            LocalDateTime fechaInicio, LocalDateTime fechaFin, LocalDateTime fechaUltimoAcceso,
            Integer cantidadAccesos) {}

    public record ReporteOrdenMeritoDTO(
            String puestoCodigo, String puestoNombre, String empresa, String evaluacion,
            LocalDateTime fechaEmision, String emitidoPor, List<CompetenciaRequeridaDTO> competencias,
            List<FilaOrdenMeritoDTO> ordenMerito, List<FilaOrdenMeritoDTO> noAlcanzanMinimo,
            List<FilaOrdenMeritoDTO> restantes) {}

    // ------------------------------------------------------------------ CU028

    /** CU028 pasos 2 a 4: puestos que coinciden con los criterios ("contiene"), con sus conteos. */
    public List<PuestoResumenDTO> buscarPuestos(String codigo, String nombre, String empresa) {
        String c = normalizar(codigo);
        String n = normalizar(nombre);
        String e = normalizar(empresa);
        return puestoDao.findByActivoTrueOrderByNombreAsc().stream()
                .filter(p -> c.isEmpty() || p.getCodigo().toUpperCase().contains(c))
                .filter(p -> n.isEmpty() || p.getNombre().toUpperCase().contains(n))
                .filter(p -> e.isEmpty() || p.getEmpresa().toUpperCase().contains(e))
                .map(p -> {
                    List<Cuestionario> cuestionarios = cuestionarioDao.findByPuestoIdOrderByIdAsc(p.getId());
                    return new PuestoResumenDTO(
                            p.getId(), p.getCodigo(), p.getNombre(), p.getEmpresa(),
                            contarCandidatos(cuestionarios), contarCompletos(cuestionarios));
                })
                .toList();
    }

    /** CU028 paso 6: evaluaciones generadas para el puesto (6.A si no tiene ninguna). */
    public EvaluacionesPuestoDTO listarEvaluaciones(Long puestoId) {
        Puesto puesto = buscarPuesto(puestoId);
        List<Cuestionario> todos = cuestionarioDao.findByPuestoIdOrderByIdAsc(puestoId);
        if (todos.isEmpty()) {
            throw new BusinessException("El puesto " + puesto.getNombre()
                    + " no posee evaluaciones registradas. No es posible emitir el orden de mérito.");
        }

        List<EvaluacionDTO> evaluaciones = new ArrayList<>();
        agruparPorEvaluacion(todos).forEach((codigo, grupo) -> evaluaciones.add(new EvaluacionDTO(
                codigo,
                grupo.get(0).getFechaCreacion(),
                contarCandidatos(grupo),
                contarCompletos(grupo))));

        return new EvaluacionesPuestoDTO(
                puesto.getId(), puesto.getCodigo(), puesto.getNombre(),
                contarCandidatos(todos), contarCompletos(todos), evaluaciones);
    }

    /**
     * CU028 paso 7. El orden de mérito lo forman los candidatos con cuestionario completo que
     * alcanzaron la ponderación mínima de CADA competencia del puesto, ordenados por puntaje.
     * Se muestran aparte los completos que no alcanzaron los mínimos y el resto agrupado por estado.
     */
    public ReporteOrdenMeritoDTO emitirOrdenDeMerito(Long puestoId, String evaluacion, String emitidoPor) {
        Puesto puesto = buscarPuesto(puestoId);
        List<Cuestionario> todos = cuestionarioDao.findByPuestoIdOrderByIdAsc(puestoId);
        if (todos.isEmpty()) {
            throw new BusinessException("El puesto " + puesto.getNombre()
                    + " no posee evaluaciones registradas. No es posible emitir el orden de mérito.");
        }

        String evaluacionElegida = evaluacion == null || evaluacion.isBlank()
                ? TODAS
                : evaluacion.trim().toUpperCase();
        List<Cuestionario> seleccionados;
        if (TODAS.equals(evaluacionElegida)) {
            seleccionados = todos;
        } else {
            seleccionados = agruparPorEvaluacion(todos).get(evaluacionElegida);
            if (seleccionados == null) {
                throw new BusinessException("La evaluación " + evaluacionElegida + " no existe para el puesto seleccionado");
            }
        }

        List<FilaOrdenMeritoDTO> cumplen = new ArrayList<>();
        List<FilaOrdenMeritoDTO> noCumplen = new ArrayList<>();
        List<FilaOrdenMeritoDTO> restantes = new ArrayList<>();

        for (Cuestionario q : seleccionados) {
            if ("COMPLETO".equals(q.getEstado())) {
                Map<Long, BigDecimal> porCompetencia = puntajesPorCompetencia(q);
                BigDecimal puntaje = q.getPuntajeTotal() != null ? q.getPuntajeTotal() : promedio(porCompetencia);
                FilaOrdenMeritoDTO fila = fila(null, q, puntaje);
                if (alcanzaMinimos(puesto, porCompetencia)) {
                    cumplen.add(fila);
                } else {
                    noCumplen.add(fila);
                }
            } else {
                restantes.add(fila(null, q, null));
            }
        }

        Comparator<FilaOrdenMeritoDTO> porPuntajeDesc = Comparator
                .comparing(FilaOrdenMeritoDTO::puntaje, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FilaOrdenMeritoDTO::apellido)
                .thenComparing(FilaOrdenMeritoDTO::nombre);
        cumplen.sort(porPuntajeDesc);
        noCumplen.sort(porPuntajeDesc);
        restantes.sort(Comparator
                .comparing((FilaOrdenMeritoDTO f) -> ordenEstado(f.estado()))
                .thenComparing(FilaOrdenMeritoDTO::apellido)
                .thenComparing(FilaOrdenMeritoDTO::nombre));

        List<FilaOrdenMeritoDTO> ordenMerito = new ArrayList<>();
        for (int i = 0; i < cumplen.size(); i++) {
            FilaOrdenMeritoDTO f = cumplen.get(i);
            ordenMerito.add(new FilaOrdenMeritoDTO(
                    i + 1, f.cuestionarioId(), f.tipoDocumento(), f.nroDocumento(), f.apellido(), f.nombre(),
                    f.estado(), f.puntaje(), f.fechaInicio(), f.fechaFin(), f.fechaUltimoAcceso(),
                    f.cantidadAccesos()));
        }

        List<CompetenciaRequeridaDTO> competencias = puesto.getCompetencias().stream()
                .sorted(Comparator.comparing(pc -> pc.getCompetencia().getNombre()))
                .map(pc -> new CompetenciaRequeridaDTO(pc.getCompetencia().getNombre(), pc.getPonderacionMinima()))
                .toList();

        return new ReporteOrdenMeritoDTO(
                puesto.getCodigo(), puesto.getNombre(), puesto.getEmpresa(), evaluacionElegida,
                LocalDateTime.now(), emitidoPor == null || emitidoPor.isBlank() ? "-" : emitidoPor.trim(),
                competencias, ordenMerito, noCumplen, restantes);
    }

    // ------------------------------------------------------------------ cálculo de puntajes

    /**
     * Algoritmo (3) del CU026: por cada factor se suman las ponderaciones obtenidas y se dividen
     * por 2; el puntaje de la competencia es el promedio de sus factores evaluados.
     */
    public Map<Long, BigDecimal> puntajesPorCompetencia(Cuestionario q) {
        Map<Long, BigDecimal> sumaPorFactor = new HashMap<>();
        Map<Long, Long> competenciaDelFactor = new HashMap<>();
        for (PreguntaCuestionario pq : q.getPreguntas()) {
            BigDecimal obtenida = respuestaDao.findByPreguntaCuestionarioId(pq.getId())
                    .map(RespuestaCuestionario::getPonderacionObtenida)
                    .orElse(BigDecimal.ZERO);
            sumaPorFactor.merge(pq.getFactor().getId(), obtenida, BigDecimal::add);
            competenciaDelFactor.put(pq.getFactor().getId(), pq.getCompetencia().getId());
        }

        Map<Long, List<BigDecimal>> factoresPorCompetencia = new HashMap<>();
        sumaPorFactor.forEach((factorId, suma) -> factoresPorCompetencia
                .computeIfAbsent(competenciaDelFactor.get(factorId), k -> new ArrayList<>())
                .add(suma.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)));

        Map<Long, BigDecimal> resultado = new HashMap<>();
        factoresPorCompetencia.forEach((competenciaId, totales) -> resultado.put(
                competenciaId,
                totales.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(totales.size()), 2, RoundingMode.HALF_UP)));
        return resultado;
    }

    private boolean alcanzaMinimos(Puesto puesto, Map<Long, BigDecimal> porCompetencia) {
        for (PuestoCompetencia pc : puesto.getCompetencias()) {
            BigDecimal obtenido = porCompetencia.getOrDefault(pc.getCompetencia().getId(), BigDecimal.ZERO);
            if (obtenido.compareTo(pc.getPonderacionMinima()) < 0) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal promedio(Map<Long, BigDecimal> porCompetencia) {
        if (porCompetencia.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return porCompetencia.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(porCompetencia.size()), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ auxiliares

    /** Agrupa los cuestionarios por evaluación (fecha de creación al segundo), en orden cronológico. */
    private Map<String, List<Cuestionario>> agruparPorEvaluacion(List<Cuestionario> cuestionarios) {
        TreeMap<LocalDateTime, List<Cuestionario>> porFecha = new TreeMap<>();
        for (Cuestionario q : cuestionarios) {
            porFecha.computeIfAbsent(q.getFechaCreacion().truncatedTo(ChronoUnit.SECONDS), k -> new ArrayList<>())
                    .add(q);
        }
        Map<String, List<Cuestionario>> resultado = new LinkedHashMap<>();
        int nro = 1;
        for (List<Cuestionario> grupo : porFecha.values()) {
            resultado.put(String.format("EVA-%03d", nro++), grupo);
        }
        return resultado;
    }

    private FilaOrdenMeritoDTO fila(Integer posicion, Cuestionario q, BigDecimal puntaje) {
        Candidato c = q.getCandidato();
        return new FilaOrdenMeritoDTO(
                posicion, q.getId(), c.getTipoDocumento(), c.getNroDocumento(), c.getApellido(), c.getNombre(),
                q.getEstado(), puntaje, q.getFechaInicio(), q.getFechaFin(), q.getFechaUltimoAcceso(),
                q.getCantidadAccesos());
    }

    private long contarCandidatos(List<Cuestionario> cuestionarios) {
        return cuestionarios.stream().map(q -> q.getCandidato().getId()).filter(Objects::nonNull).distinct().count();
    }

    private long contarCompletos(List<Cuestionario> cuestionarios) {
        return cuestionarios.stream().filter(q -> "COMPLETO".equals(q.getEstado())).count();
    }

    private int ordenEstado(String estado) {
        int idx = ORDEN_ESTADOS_RESTANTES.indexOf(estado);
        return idx < 0 ? ORDEN_ESTADOS_RESTANTES.size() : idx;
    }

    private Puesto buscarPuesto(Long puestoId) {
        if (puestoId == null) {
            throw new BusinessException("Debe seleccionar un puesto");
        }
        return puestoDao.findById(puestoId)
                .orElseThrow(() -> new BusinessException("Puesto no encontrado"));
    }

    private String normalizar(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}
