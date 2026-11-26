package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.*;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.*;
import ar.edu.utn.frsf.isi.capitalhumano.service.estado.EstadoCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.estado.EstadosCuestionario;
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

    private static final List<String> ESTADOS_VIGENTES =
            List.of(EstadosCuestionario.ACTIVO.nombre(), EstadosCuestionario.EN_PROCESO.nombre());

    /** CU025 paso 6: candidatos que ya poseen cuestionarios activos o en proceso (6.A). */
    public List<Candidato> validarCuestionariosActivos(List<Long> candidatoIds) {
        if (candidatoIds == null || candidatoIds.isEmpty()) {
            throw new BusinessException("Se debe seleccionar uno o más candidatos");
        }
        List<Candidato> conCuestionarioVigente = new ArrayList<>();
        for (Long candId : new LinkedHashSet<>(candidatoIds)) {
            if (!cuestionarioDao.findByCandidatoIdAndEstadoIn(candId, ESTADOS_VIGENTES).isEmpty()) {
                candidatoDao.findById(candId).ifPresent(conCuestionarioVigente::add);
            }
        }
        return conCuestionarioVigente;
    }

    public record CompetenciaPuestoDTO(
            Long competenciaId, String codigo, String nombre, BigDecimal ponderacionMinima, boolean evaluable) {}

    /**
     * CU025 paso 9: competencias del puesto con su ponderación requerida. Una competencia es
     * evaluable si tiene al menos un factor activo con 2 o más preguntas activas (9.A).
     */
    public List<CompetenciaPuestoDTO> obtenerCompetenciasPuesto(Long puestoId) {
        Puesto puesto = buscarPuestoActivo(puestoId);
        return puesto.getCompetencias().stream()
                .sorted(Comparator.comparing(pc -> pc.getCompetencia().getNombre()))
                .map(pc -> new CompetenciaPuestoDTO(
                        pc.getCompetencia().getId(),
                        pc.getCompetencia().getCodigo(),
                        pc.getCompetencia().getNombre(),
                        pc.getPonderacionMinima(),
                        esCompetenciaEvaluable(pc.getCompetencia().getId())))
                .toList();
    }

    public record CandidatoClaveDTO(
            Long candidatoId, String nroCandidato, String apellido, String nombre,
            String tipoDocumento, String nroDocumento, String claveIngreso) {}

    /**
     * CU025 pasos 10-11: valida candidatos y puesto, y genera las claves de ingreso que se muestran
     * antes de confirmar. No guarda nada: los cuestionarios se crean recién al presionar Finalizar.
     */
    public List<CandidatoClaveDTO> generarClaves(List<Long> candidatoIds, Long puestoId) {
        validarSeleccion(candidatoIds, puestoId);
        validarSinCuestionariosVigentes(candidatoIds);
        validarCompetenciasEvaluables(buscarPuestoActivo(puestoId));

        List<CandidatoClaveDTO> resultado = new ArrayList<>();
        Set<String> usadas = new HashSet<>();
        for (Long candId : new LinkedHashSet<>(candidatoIds)) {
            Candidato c = candidatoDao.findById(candId)
                    .orElseThrow(() -> new BusinessException("Candidato no encontrado: " + candId));
            String clave;
            do {
                clave = generarClaveAlfanumerica(8);
            } while (!usadas.add(clave));
            resultado.add(new CandidatoClaveDTO(
                    c.getId(), c.getNroCandidato(), c.getApellido(), c.getNombre(),
                    c.getTipoDocumento(), c.getNroDocumento(), clave));
        }
        return resultado;
    }

    @Transactional
    public List<Cuestionario> generarEvaluaciones(List<Long> candidatoIds, Long puestoId) {
        return generarEvaluaciones(candidatoIds, puestoId, null);
    }

    /**
     * CU025 paso 13: crea un cuestionario en estado ACTIVO por candidato. Si se reciben las claves
     * mostradas en el paso 11 se usan esas; si no, se generan. Todos los cuestionarios de la
     * evaluación comparten la misma fecha de creación (así se identifica la evaluación en el CU028).
     */
    @Transactional
    public List<Cuestionario> generarEvaluaciones(List<Long> candidatoIds, Long puestoId, Map<Long, String> claves) {
        validarSeleccion(candidatoIds, puestoId);
        validarSinCuestionariosVigentes(candidatoIds);
        Puesto puesto = buscarPuestoActivo(puestoId);
        validarCompetenciasEvaluables(puesto);

        LocalDateTime ahora = LocalDateTime.now();
        List<Cuestionario> creados = new ArrayList<>();
        for (Long candId : new LinkedHashSet<>(candidatoIds)) {
            Candidato cand = candidatoDao.findById(candId)
                    .orElseThrow(() -> new BusinessException("Candidato no encontrado: " + candId));
            String claveRecibida = claves == null ? null : claves.get(candId);
            Cuestionario q = new Cuestionario();
            q.setCandidato(cand);
            q.setPuesto(puesto);
            q.setClaveAcceso(esClaveValida(claveRecibida) ? claveRecibida.trim().toUpperCase() : generarClaveAlfanumerica(8));
            q.setEstado(EstadosCuestionario.ACTIVO.nombre());
            q.setBloqueActual(1);
            q.setCantidadAccesos(0);
            q.setFechaCreacion(ahora);
            creados.add(cuestionarioDao.save(q));
        }
        return creados;
    }

    private void validarSeleccion(List<Long> candidatoIds, Long puestoId) {
        if (candidatoIds == null || candidatoIds.isEmpty()) {
            throw new BusinessException("Se debe seleccionar uno o más candidatos");
        }
        if (puestoId == null) {
            throw new BusinessException("Se debe seleccionar una función o puesto para realizar la evaluación.");
        }
    }

    private void validarSinCuestionariosVigentes(List<Long> candidatoIds) {
        List<String> bloqueados = validarCuestionariosActivos(candidatoIds).stream()
                .map(c -> c.getApellido() + ", " + c.getNombre())
                .toList();
        if (!bloqueados.isEmpty()) {
            throw new BusinessException(
                    "Los siguientes candidatos poseen cuestionarios activos o en proceso y no pueden ser evaluados: "
                            + String.join("; ", bloqueados) + ".",
                    bloqueados);
        }
    }

    private void validarCompetenciasEvaluables(Puesto puesto) {
        List<String> competenciasNoEvaluables = puesto.getCompetencias().stream()
                .filter(pc -> !esCompetenciaEvaluable(pc.getCompetencia().getId()))
                .map(pc -> pc.getCompetencia().getNombre())
                .toList();
        if (!competenciasNoEvaluables.isEmpty()) {
            throw new BusinessException(
                    "Las siguientes competencias no poseen factores que cumplan las condiciones para ser evaluadas: "
                            + String.join(", ", competenciasNoEvaluables) + ".",
                    competenciasNoEvaluables);
        }
    }

    private boolean esCompetenciaEvaluable(Long competenciaId) {
        return factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(competenciaId).stream()
                .anyMatch(f -> preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(f.getId()).size() >= 2);
    }

    private Puesto buscarPuestoActivo(Long puestoId) {
        if (puestoId == null) {
            throw new BusinessException("Se debe seleccionar una función o puesto para realizar la evaluación.");
        }
        return puestoDao.findById(puestoId)
                .filter(Puesto::isActivo)
                .orElseThrow(() -> new BusinessException("Puesto no encontrado"));
    }

    private boolean esClaveValida(String clave) {
        if (clave == null) {
            return false;
        }
        String c = clave.trim().toUpperCase();
        return c.matches("[A-Z0-9]{8}") && c.matches(".*[A-Z].*") && c.matches(".*[0-9].*");
    }

    @Transactional
    public Cuestionario iniciarOSortearCuestionario(Long cuestionarioId) {
        Cuestionario q = cuestionarioDao.findById(cuestionarioId)
                .orElseThrow(() -> new BusinessException("Cuestionario no encontrado"));

        ConfiguracionCuestionario cfg = obtenerConfiguracion();
        // Patrón State: el estado decide si hay que sortear las preguntas y cómo se registra el ingreso.
        EstadoCuestionario estado = EstadosCuestionario.de(q);

        if (estado.requiereSortearPreguntas()) {
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
        }

        estado.registrarIngreso(q, LocalDateTime.now());
        return cuestionarioDao.save(q);
    }

    public record RespuestaItemInput(Long preguntaCuestionarioId, Long detalleOpcionRespuestaId) {}

    @Transactional
    public Cuestionario responderBloque(Long cuestionarioId, Integer nroBloque, List<RespuestaItemInput> respuestas) {
        Cuestionario q = cuestionarioDao.findById(cuestionarioId)
                .orElseThrow(() -> new BusinessException("Cuestionario no encontrado"));

        EstadoCuestionario estado = EstadosCuestionario.de(q);
        estado.validarQueAdmiteRespuestas(q);
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
            estado.completar(q, calcularPuntajeTotal(q), LocalDateTime.now());
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

    /** Requerimiento especial CU025: clave aleatoria de 8 caracteres con letras y dígitos. */
    private String generarClaveAlfanumerica(int longitud) {
        String clave;
        do {
            StringBuilder sb = new StringBuilder(longitud);
            for (int i = 0; i < longitud; i++) {
                sb.append(CARACTERES_CLAVE.charAt(RANDOM.nextInt(CARACTERES_CLAVE.length())));
            }
            clave = sb.toString();
        } while (!clave.matches(".*[A-Z].*") || !clave.matches(".*[0-9].*"));
        return clave;
    }
}
