package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.EvaluacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.RespuestaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Evaluacion;
import ar.edu.utn.frsf.isi.capitalhumano.model.PreguntaCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.PuestoCompetencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.RespuestaCuestionario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** GestorReportes: CU028 - Emitir orden de mérito */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    public static final String TODAS = "TODAS";

    private static final List<String> ORDEN_ESTADOS_RESTANTES =
            List.of("EN_PROCESO", "ACTIVO", "INCOMPLETO", "SIN_CONTESTAR");

    private final PuestoDao puestoDao;
    private final EvaluacionDao evaluacionDao;
    private final RespuestaCuestionarioDao respuestaDao;

    public ReporteService(
            PuestoDao puestoDao,
            EvaluacionDao evaluacionDao,
            RespuestaCuestionarioDao respuestaDao
    ) {
        this.puestoDao = puestoDao;
        this.evaluacionDao = evaluacionDao;
        this.respuestaDao = respuestaDao;
    }

    // DTOs

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

    /** evaluaciones elegidas y cómo se muestran en el reporte ("TODAS" o sus códigos) */
    private record Seleccion(List<Evaluacion> evaluaciones, String descripcion) {}

    // CU028

    /** puestos que coinciden con la búsqueda (contiene), con sus conteos */
    public List<PuestoResumenDTO> buscarPuestos(String codigo, String nombre, String empresa) {
        String c = normalizar(codigo);
        String n = normalizar(nombre);
        String e = normalizar(empresa);
        return puestoDao.findByActivoTrueOrderByNombreAsc().stream()
                .filter(p -> c.isEmpty() || p.getCodigo().toUpperCase().contains(c))
                .filter(p -> n.isEmpty() || p.getNombre().toUpperCase().contains(n))
                .filter(p -> e.isEmpty() || p.getEmpresa().toUpperCase().contains(e))
                .map(p -> {
                    List<Cuestionario> cuestionarios = cuestionariosDe(evaluacionDao.findByPuestoIdOrderByNroEvaluacionAsc(p.getId()));
                    return new PuestoResumenDTO(
                            p.getId(), p.getCodigo(), p.getNombre(), p.getEmpresa(),
                            contarCandidatos(cuestionarios), contarCompletos(cuestionarios));
                })
                .toList();
    }

    /** evaluaciones generadas para el puesto */
    public EvaluacionesPuestoDTO listarEvaluaciones(Long puestoId) {
        Puesto puesto = buscarPuesto(puestoId);
        List<Evaluacion> evaluaciones = evaluacionesDelPuesto(puesto);

        List<EvaluacionDTO> dtos = evaluaciones.stream()
                .map(ev -> new EvaluacionDTO(
                        ev.getCodigo(),
                        ev.getFechaCreacion(),
                        contarCandidatos(ev.getCuestionarios()),
                        contarCompletos(ev.getCuestionarios())))
                .toList();
        List<Cuestionario> todos = cuestionariosDe(evaluaciones);

        return new EvaluacionesPuestoDTO(
                puesto.getId(), puesto.getCodigo(), puesto.getNombre(),
                contarCandidatos(todos), contarCompletos(todos), dtos);
    }

    /** recibe una evaluación (EVA-001), varias separadas por coma (EVA-001,EVA-003) o TODAS */
    public ReporteOrdenMeritoDTO emitirOrdenDeMerito(Long puestoId, String evaluacion, String emitidoPor) {
        return emitirOrdenDeMerito(puestoId, evaluacion == null ? List.of() : List.of(evaluacion), emitidoPor);
    }

    /** orden de mérito: completos que alcanzan el mínimo de cada competencia, ordenados por puntaje */
    public ReporteOrdenMeritoDTO emitirOrdenDeMerito(Long puestoId, List<String> evaluaciones, String emitidoPor) {
        Puesto puesto = buscarPuesto(puestoId);
        Seleccion seleccion = seleccionarEvaluaciones(evaluacionesDelPuesto(puesto), evaluaciones);

        List<FilaOrdenMeritoDTO> cumplen = new ArrayList<>();
        List<FilaOrdenMeritoDTO> noCumplen = new ArrayList<>();
        List<FilaOrdenMeritoDTO> restantes = new ArrayList<>();

        for (Evaluacion ev : seleccion.evaluaciones()) {
            for (Cuestionario q : ev.getCuestionarios()) {
                if ("COMPLETO".equals(q.getEstado())) {
                    Map<Long, BigDecimal> porCompetencia = puntajesPorCompetencia(q);
                    BigDecimal puntaje = q.getPuntajeTotal() != null ? q.getPuntajeTotal() : promedio(porCompetencia);
                    FilaOrdenMeritoDTO fila = fila(null, q, puntaje);
                    if (alcanzaMinimos(puesto, ev, porCompetencia)) {
                        cumplen.add(fila);
                    } else {
                        noCumplen.add(fila);
                    }
                } else {
                    restantes.add(fila(null, q, null));
                }
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
                puesto.getCodigo(), puesto.getNombre(), puesto.getEmpresa(), seleccion.descripcion(),
                LocalDateTime.now(), emitidoPor == null || emitidoPor.isBlank() ? "-" : emitidoPor.trim(),
                competencias, ordenMerito, noCumplen, restantes);
    }

    // cálculo de puntajes

    /** puntaje del CU026: cada factor suma sus ponderaciones y divide por 2, la competencia promedia sus factores */
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

    /** controla las competencias que evaluó la evaluación contra el mínimo que pide el puesto */
    private boolean alcanzaMinimos(Puesto puesto, Evaluacion evaluacion, Map<Long, BigDecimal> porCompetencia) {
        for (Competencia competencia : evaluacion.getCompetenciasEvaluadas()) {
            Optional<BigDecimal> minimo = puesto.getCompetencias().stream()
                    .filter(pc -> pc.getCompetencia().getId().equals(competencia.getId()))
                    .map(PuestoCompetencia::getPonderacionMinima)
                    .findFirst();
            BigDecimal obtenido = porCompetencia.getOrDefault(competencia.getId(), BigDecimal.ZERO);
            if (minimo.isPresent() && obtenido.compareTo(minimo.get()) < 0) {
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

    // auxiliares

    /** evaluaciones del puesto en orden, si no tiene ninguna no se puede emitir el orden de mérito */
    private List<Evaluacion> evaluacionesDelPuesto(Puesto puesto) {
        List<Evaluacion> evaluaciones = evaluacionDao.findByPuestoIdOrderByNroEvaluacionAsc(puesto.getId());
        if (evaluaciones.isEmpty()) {
            throw new BusinessException("El puesto " + puesto.getNombre()
                    + " no posee evaluaciones registradas. No es posible emitir el orden de mérito.");
        }
        return evaluaciones;
    }

    /** evaluaciones elegidas por código o TODAS (también si no se elige ninguna) */
    private Seleccion seleccionarEvaluaciones(List<Evaluacion> delPuesto, List<String> pedidas) {
        List<String> codigos = pedidas == null ? List.of() : pedidas.stream()
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(this::normalizar)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (codigos.isEmpty() || codigos.contains(TODAS)) {
            return new Seleccion(delPuesto, TODAS);
        }
        for (String codigo : codigos) {
            if (delPuesto.stream().noneMatch(ev -> codigo.equals(ev.getCodigo()))) {
                throw new BusinessException("La evaluación " + codigo + " no existe para el puesto seleccionado");
            }
        }
        List<Evaluacion> elegidas = delPuesto.stream().filter(ev -> codigos.contains(ev.getCodigo())).toList();
        return new Seleccion(elegidas, elegidas.stream().map(Evaluacion::getCodigo).collect(Collectors.joining(", ")));
    }

    private List<Cuestionario> cuestionariosDe(List<Evaluacion> evaluaciones) {
        return evaluaciones.stream().flatMap(ev -> ev.getCuestionarios().stream()).toList();
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
