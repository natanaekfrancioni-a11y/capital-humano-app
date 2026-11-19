package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.*;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EvaluacionCuestionarioService {

    private static final String CARACTERES_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CandidatoDao candidatoDao;
    private final PuestoDao puestoDao;
    private final FactorDao factorDao;
    private final PreguntaDao preguntaDao;
    private final CuestionarioDao cuestionarioDao;
    private final PreguntaCuestionarioDao preguntaCuestionarioDao;
    private final RespuestaCuestionarioDao respuestaCuestionarioDao;
    private final ConfiguracionCuestionarioDao configuracionDao;

    public EvaluacionCuestionarioService(
            CandidatoDao candidatoDao,
            PuestoDao puestoDao,
            FactorDao factorDao,
            PreguntaDao preguntaDao,
            CuestionarioDao cuestionarioDao,
            PreguntaCuestionarioDao preguntaCuestionarioDao,
            RespuestaCuestionarioDao respuestaCuestionarioDao,
            ConfiguracionCuestionarioDao configuracionDao
    ) {
        this.candidatoDao = candidatoDao;
        this.puestoDao = puestoDao;
        this.factorDao = factorDao;
        this.preguntaDao = preguntaDao;
        this.cuestionarioDao = cuestionarioDao;
        this.preguntaCuestionarioDao = preguntaCuestionarioDao;
        this.respuestaCuestionarioDao = respuestaCuestionarioDao;
        this.configuracionDao = configuracionDao;
    }

    public List<Candidato> buscarCandidatos(String apellido, String nombre, String nroCandidato) {
        String ap = apellido == null ? "" : apellido.trim().toUpperCase();
        String no = nombre == null ? "" : nombre.trim().toUpperCase();
        String nr = nroCandidato == null ? "" : nroCandidato.trim().toUpperCase();
        return candidatoDao.findAll().stream()
                .filter(c -> !c.isEliminado())
                .filter(c -> ap.isEmpty() || c.getApellido().toUpperCase().contains(ap))
                .filter(c -> no.isEmpty() || c.getNombre().toUpperCase().contains(no))
                .filter(c -> nr.isEmpty() || c.getNroCandidato().toUpperCase().contains(nr))
                .toList();
    }

    @Transactional
    public List<Cuestionario> generarEvaluaciones(List<Long> candidatoIds, Long puestoId) {
        if (candidatoIds == null || candidatoIds.isEmpty()) {
            throw new BusinessException("Debe seleccionar uno o mas candidatos");
        }
        if (puestoId == null) {
            throw new BusinessException("Debe seleccionar una funcion o puesto");
        }

        List<String> bloqueados = new ArrayList<>();
        for (Long candId : candidatoIds) {
            List<Cuestionario> activos = cuestionarioDao.findByCandidatoIdAndEstadoIn(candId, List.of("ACTIVO", "EN_PROCESO"));
            if (!activos.isEmpty()) {
                Candidato c = candidatoDao.findById(candId).orElse(null);
                bloqueados.add(c != null ? c.getApellido() + ", " + c.getNombre() : "ID " + candId);
            }
        }
        if (!bloqueados.isEmpty()) {
            throw new BusinessException(
                    "Los siguientes candidatos poseen cuestionarios activos o en proceso y no pueden ser evaluados nuevamente: "
                            + String.join("; ", bloqueados)
            );
        }

        Puesto puesto = puestoDao.findById(puestoId)
                .filter(Puesto::isActivo)
                .orElseThrow(() -> new BusinessException("Puesto no encontrado"));

        List<String> competenciasNoEvaluables = new ArrayList<>();
        for (PuestoCompetencia pc : puesto.getCompetencias()) {
            List<Factor> factores = factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(pc.getCompetencia().getId());
            long factoresValidos = factores.stream()
                    .filter(f -> preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(f.getId()).size() >= 2)
                    .count();
            if (factoresValidos == 0) {
                competenciasNoEvaluables.add(pc.getCompetencia().getNombre());
            }
        }
        if (!competenciasNoEvaluables.isEmpty()) {
            throw new BusinessException(
                    "Las siguientes competencias no poseen factores con al menos 2 preguntas para ser evaluadas: "
                            + String.join(", ", competenciasNoEvaluables)
            );
        }

        List<Cuestionario> creados = new ArrayList<>();
        for (Long candId : candidatoIds) {
            Candidato cand = candidatoDao.findById(candId)
                    .orElseThrow(() -> new BusinessException("Candidato no encontrado: " + candId));
            Cuestionario q = new Cuestionario();
            q.setCandidato(cand);
            q.setPuesto(puesto);
            q.setClaveAcceso(generarClaveAlfanumerica(8));
            q.setEstado("ACTIVO");
            q.setBloqueActual(1);
            q.setCantidadAccesos(0);
            q.setFechaCreacion(LocalDateTime.now());
            creados.add(cuestionarioDao.save(q));
        }
        return creados;
    }

    @Transactional
    public Cuestionario iniciarOSortearCuestionario(Long cuestionarioId) {
        Cuestionario q = cuestionarioDao.findById(cuestionarioId)
                .orElseThrow(() -> new BusinessException("Cuestionario no encontrado"));

        ConfiguracionCuestionario cfg = obtenerConfiguracion();

        if ("ACTIVO".equals(q.getEstado())) {
            List<Pregunta> seleccionadas = new ArrayList<>();
            for (PuestoCompetencia pc : q.getPuesto().getCompetencias()) {
                List<Factor> factores = factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(pc.getCompetencia().getId());
                for (Factor f : factores) {
                    List<Pregunta> disponibles = new ArrayList<>(preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(f.getId()));
                    if (disponibles.size() >= 2) {
                        Collections.shuffle(disponibles, RANDOM);
                        seleccionadas.add(disponibles.get(0));
                        seleccionadas.add(disponibles.get(1));
                    }
                }
            }
            Collections.shuffle(seleccionadas, RANDOM);

            int tamBloque = Math.max(1, cfg.getPreguntasPorBloque());
            List<PreguntaCuestionario> items = new ArrayList<>();
            for (int i = 0; i < seleccionadas.size(); i++) {
                Pregunta p = seleccionadas.get(i);
                PreguntaCuestionario pq = new PreguntaCuestionario();
                pq.setCuestionario(q);
                pq.setPregunta(p);
                pq.setFactor(p.getFactor());
                pq.setCompetencia(p.getFactor().getCompetencia());
                pq.setOrdenVisual(i + 1);
                pq.setNroBloque((i / tamBloque) + 1);
                pq.setRespondida(false);
                items.add(pq);
            }
            q.getPreguntas().clear();
            q.getPreguntas().addAll(items);
            q.setEstado("EN_PROCESO");
            q.setBloqueActual(1);
            q.setFechaInicio(LocalDateTime.now());
        }

        q.setCantidadAccesos(q.getCantidadAccesos() + 1);
        q.setFechaUltimoAcceso(LocalDateTime.now());
        return cuestionarioDao.save(q);
    }

    public record RespuestaItemInput(Long preguntaCuestionarioId, Long detalleOpcionRespuestaId) {}

    @Transactional
    public Cuestionario responderBloque(Long cuestionarioId, Integer nroBloque, List<RespuestaItemInput> respuestas) {
        Cuestionario q = cuestionarioDao.findById(cuestionarioId)
                .orElseThrow(() -> new BusinessException("Cuestionario no encontrado"));

        if (!"EN_PROCESO".equals(q.getEstado())) {
            throw new BusinessException("El cuestionario no se encuentra en proceso");
        }
        if (!Objects.equals(nroBloque, q.getBloqueActual())) {
            throw new BusinessException("Solo puede responder el bloque activo actual (" + q.getBloqueActual() + ")");
        }

        List<PreguntaCuestionario> preguntasBloque = preguntaCuestionarioDao
                .findByCuestionarioIdAndNroBloqueOrderByOrdenVisualAsc(cuestionarioId, nroBloque);

        Map<Long, Long> seleccionMap = new HashMap<>();
        if (respuestas != null) {
            for (RespuestaItemInput r : respuestas) {
                if (r.preguntaCuestionarioId() != null && r.detalleOpcionRespuestaId() != null) {
                    seleccionMap.put(r.preguntaCuestionarioId(), r.detalleOpcionRespuestaId());
                }
            }
        }

        for (PreguntaCuestionario pq : preguntasBloque) {
            if (!seleccionMap.containsKey(pq.getId())) {
                throw new BusinessException("Existen preguntas sin responder en el bloque actual");
            }
        }

        for (PreguntaCuestionario pq : preguntasBloque) {
            Long detId = seleccionMap.get(pq.getId());
            DetalleOpcionRespuesta det = pq.getPregunta().getOpcionRespuesta().getDetalles().stream()
                    .filter(d -> d.getId().equals(detId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Opcion de respuesta invalida para la pregunta " + pq.getId()));

            BigDecimal pond = pq.getPregunta().getPonderacionesPorDetalle()
                    .getOrDefault(detId, BigDecimal.ZERO);

            RespuestaCuestionario resp = respuestaCuestionarioDao.findByPreguntaCuestionarioId(pq.getId())
                    .orElseGet(RespuestaCuestionario::new);
            resp.setPreguntaCuestionario(pq);
            resp.setDetalleOpcionRespuesta(det);
            resp.setPonderacionObtenida(pond);
            resp.setRespondidaEn(LocalDateTime.now());
            respuestaCuestionarioDao.save(resp);

            pq.setRespondida(true);
            preguntaCuestionarioDao.save(pq);
        }

        int maxBloque = q.getPreguntas().stream()
                .mapToInt(PreguntaCuestionario::getNroBloque)
                .max()
                .orElse(1);

        if (nroBloque < maxBloque) {
            q.setBloqueActual(nroBloque + 1);
            q.setFechaUltimoAcceso(LocalDateTime.now());
        } else {
            BigDecimal puntajeFinal = calcularPuntajeTotal(q);
            q.setPuntajeTotal(puntajeFinal);
            q.setEstado("COMPLETO");
            q.setFechaFin(LocalDateTime.now());
            q.setFechaCambioEstado(LocalDateTime.now());
        }
        return cuestionarioDao.save(q);
    }

public BigDecimal calcularPuntajeTotal(Cuestionario cuestionario) {
        Map<Long, List<BigDecimal>> puntajesPorFactor = new HashMap<>();
        Map<Long, Long> factorACompetencia = new HashMap<>();

        for (PreguntaCuestionario pq : cuestionario.getPreguntas()) {
            factorACompetencia.put(pq.getFactor().getId(), pq.getCompetencia().getId());
            BigDecimal pond = respuestaCuestionarioDao.findByPreguntaCuestionarioId(pq.getId())
                    .map(RespuestaCuestionario::getPonderacionObtenida)
                    .orElse(BigDecimal.ZERO);
            puntajesPorFactor.computeIfAbsent(pq.getFactor().getId(), k -> new ArrayList<>()).add(pond);
        }

        Map<Long, List<BigDecimal>> totalesPorCompetencia = new HashMap<>();
        for (Map.Entry<Long, List<BigDecimal>> entry : puntajesPorFactor.entrySet()) {
            BigDecimal sumaFactor = entry.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalFactor = sumaFactor.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            Long compId = factorACompetencia.get(entry.getKey());
            totalesPorCompetencia.computeIfAbsent(compId, k -> new ArrayList<>()).add(totalFactor);
        }

        if (totalesPorCompetencia.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sumaCompetencias = BigDecimal.ZERO;
        for (List<BigDecimal> factoresComp : totalesPorCompetencia.values()) {
            BigDecimal sumaFact = factoresComp.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promComp = sumaFact.divide(BigDecimal.valueOf(factoresComp.size()), 4, RoundingMode.HALF_UP);
            sumaCompetencias = sumaCompetencias.add(promComp);
        }

        return sumaCompetencias
                .divide(BigDecimal.valueOf(totalesPorCompetencia.size()), 2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> emitirOrdenDeMerito(Long puestoId) {
        Puesto puesto = puestoDao.findById(puestoId)
                .filter(Puesto::isActivo)
                .orElseThrow(() -> new BusinessException("Puesto no encontrado"));

        List<Cuestionario> todos = cuestionarioDao.findByPuestoIdOrderByIdAsc(puestoId);
        if (todos.isEmpty()) {
            throw new BusinessException("El puesto no posee evaluaciones");
        }

        var ordenMerito = new ArrayList<Map<String, Object>>();
        var noCumplenMinimos = new ArrayList<Map<String, Object>>();
        var porEstado = new LinkedHashMap<String, List<Map<String, Object>>>();

        for (Cuestionario q : todos) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("cuestionarioId", q.getId());
            fila.put("tipoDocumento", q.getCandidato().getTipoDocumento());
            fila.put("nroDocumento", q.getCandidato().getNroDocumento());
            fila.put("apellido", q.getCandidato().getApellido());
            fila.put("nombre", q.getCandidato().getNombre());
            fila.put("estado", q.getEstado());
            fila.put("fechaInicio", q.getFechaInicio());
            fila.put("fechaFin", q.getFechaFin());
            fila.put("fechaUltimoAcceso", q.getFechaUltimoAcceso());
            fila.put("cantidadAccesos", q.getCantidadAccesos());

            if ("COMPLETO".equals(q.getEstado())) {
                BigDecimal puntaje = q.getPuntajeTotal() != null ? q.getPuntajeTotal() : calcularPuntajeTotal(q);
                fila.put("puntajeObtenido", puntaje);

                boolean cumpleMinimos = puesto.getCompetencias().stream()
                        .allMatch(pc -> puntaje.compareTo(pc.getPonderacionMinima()) >= 0);

                if (cumpleMinimos) {
                    ordenMerito.add(fila);
                } else {
                    noCumplenMinimos.add(fila);
                }
            } else {
                fila.put("puntajeObtenido", null);
                porEstado.computeIfAbsent(q.getEstado(), k -> new ArrayList<>()).add(fila);
            }
        }

        ordenMerito.sort((a, b) -> ((BigDecimal) b.get("puntajeObtenido")).compareTo((BigDecimal) a.get("puntajeObtenido")));

        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("puestoCodigo", puesto.getCodigo());
        reporte.put("puestoNombre", puesto.getNombre());
        reporte.put("empresa", puesto.getEmpresa());
        reporte.put("ordenMerito", ordenMerito);
        reporte.put("noAlcanzanMinimo", noCumplenMinimos);
        reporte.put("porEstado", porEstado);
        return reporte;
    }

    public ConfiguracionCuestionario obtenerConfiguracion() {
        return configuracionDao.findAll().stream().findFirst().orElseGet(() -> {
            ConfiguracionCuestionario c = new ConfiguracionCuestionario();
            c.setPreguntasPorBloque(2);
            c.setMaxAccesos(5);
            c.setTiempoMaxMinutos(90);
            c.setDiasVencimientoActivo(30);
            c.setInstrucciones("LEA ATENTAMENTE CADA PREGUNTA Y SELECCIONE UNA OPCION.");
            return c;
        });
    }

    private String generarClaveAlfanumerica(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(CARACTERES_CLAVE.charAt(RANDOM.nextInt(CARACTERES_CLAVE.length())));
        }
        return sb.toString();
    }
}
