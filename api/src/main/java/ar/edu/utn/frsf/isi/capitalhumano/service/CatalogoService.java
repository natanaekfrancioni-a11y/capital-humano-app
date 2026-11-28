package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.*;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.*;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaLogica;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaPregunta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** GestorPreguntas: CU020 Gestionar preguntas, CU021 alta, CU022 modificación y CU023 baja */
@Service
public class CatalogoService {

    private final CompetenciaDao competenciaDao;
    private final FactorDao factorDao;
    private final OpcionRespuestaDao opcionRespuestaDao;
    private final PreguntaDao preguntaDao;
    private final PuestoDao puestoDao;

    // baja lógica con el patrón Template Method (BajaPregunta extiende BajaLogica)
    private final BajaLogica<Pregunta> bajaPregunta;

    public CatalogoService(
            CompetenciaDao competenciaDao,
            FactorDao factorDao,
            OpcionRespuestaDao opcionRespuestaDao,
            PreguntaDao preguntaDao,
            PuestoDao puestoDao,
            AuditoriaEliminacionDao auditoriaDao
    ) {
        this.competenciaDao = competenciaDao;
        this.factorDao = factorDao;
        this.opcionRespuestaDao = opcionRespuestaDao;
        this.preguntaDao = preguntaDao;
        this.puestoDao = puestoDao;
        this.bajaPregunta = new BajaPregunta(preguntaDao, auditoriaDao);
    }

    public List<Competencia> buscarCompetencias(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return competenciaDao.findByActivaTrueOrderByNombreAsc();
        }
        return competenciaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim());
    }

    public List<Factor> buscarFactores(Long competenciaId, String nombre) {
        List<Factor> baseList = competenciaId != null
                ? factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(competenciaId)
                : factorDao.findByActivoTrueOrderByNombreAsc();

        String filtro = nombre == null ? "" : nombre.trim().toUpperCase();
        return baseList.stream()
                .filter(f -> filtro.isEmpty() || f.getNombre().toUpperCase().contains(filtro))
                .sorted(Comparator.comparing(Factor::getNombre))
                .toList();
    }

    public List<OpcionRespuesta> buscarOpcionesRespuesta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return opcionRespuestaDao.findByActivaTrueOrderByNombreAsc();
        }
        return opcionRespuestaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim());
    }

    public List<Pregunta> buscarPreguntas(Long competenciaId, Long factorId, String nombre) {
        String filtroNom = nombre == null ? "" : nombre.trim().toUpperCase();
        return preguntaDao.findByActivaTrueOrderByIdAsc().stream()
                .filter(p -> factorId == null || p.getFactor().getId().equals(factorId))
                .filter(p -> competenciaId == null || p.getFactor().getCompetencia().getId().equals(competenciaId))
                .filter(p -> filtroNom.isEmpty() || p.getNombre().toUpperCase().contains(filtroNom))
                .toList();
    }

    @Transactional
    public Pregunta crearPregunta(
            Long factorId,
            Long opcionRespuestaId,
            String codigo,
            String nombre,
            String enunciado,
            String descripcion,
            Map<Long, BigDecimal> ponderaciones
    ) {
        List<String> errores = new ArrayList<>();
        String cod = normUpper(codigo);
        String nom = normUpper(nombre);
        String enun = normUpper(enunciado);
        String desc = descripcion == null ? null : normUpper(descripcion);

        if (factorId == null) errores.add("El factor es obligatorio");
        if (opcionRespuestaId == null) errores.add("La opción de respuesta es obligatoria");
        if (nom.isEmpty()) errores.add("El nombre de pregunta es obligatorio");
        if (enun.isEmpty()) errores.add("El enunciado de pregunta es obligatorio");
        if (cod.length() > 30) errores.add("El código excede la longitud máxima de 30 caracteres");
        if (nom.length() > 120) errores.add("El nombre excede la longitud máxima de 120 caracteres");
        if (desc != null && desc.length() > 255) errores.add("La descripción excede la longitud máxima de 255 caracteres");
        if (!errores.isEmpty()) {
            throw new BusinessException("Validaciones no superadas al crear pregunta", errores);
        }

        Factor factor = factorDao.findById(factorId)
                .filter(Factor::isActivo)
                .orElseThrow(() -> new BusinessException("Factor no encontrado"));

        OpcionRespuesta op = opcionRespuestaDao.findById(opcionRespuestaId)
                .filter(OpcionRespuesta::isActiva)
                .orElseThrow(() -> new BusinessException("Opción de respuesta no encontrada"));

        // CU021: la existencia se valida por nombre y por el texto de la pregunta
        if (preguntaDao.existsByFactorIdAndNombreIgnoreCaseAndActivaTrue(factorId, nom)) {
            throw new BusinessException("Ya se encuentra registrada una pregunta con el mismo nombre para la "
                    + "combinación de competencia y factor seleccionada.");
        }
        if (preguntaDao.existsByFactorIdAndEnunciadoIgnoreCaseAndActivaTrue(factorId, enun)) {
            throw new BusinessException("Ya se encuentra registrada una pregunta con el mismo texto para la "
                    + "combinación de competencia y factor seleccionada.");
        }
        if (cod.isEmpty()) {
            cod = generarCodigoPregunta(factor);
        } else if (preguntaDao.findByCodigoIgnoreCase(cod).isPresent()) {
            throw new BusinessException("El código de pregunta " + cod + " ya existe");
        }

        Map<Long, BigDecimal> pondFinal = normalizarYValidarPonderaciones(op, ponderaciones);

        Pregunta p = new Pregunta();
        p.setFactor(factor);
        p.setOpcionRespuesta(op);
        p.setCodigo(cod);
        p.setNombre(nom);
        p.setEnunciado(enun);
        p.setDescripcion(desc);
        p.setActiva(true);
        p.setPonderacionesPorDetalle(pondFinal);
        return preguntaDao.save(p);
    }

    @Transactional
    public Pregunta modificarPregunta(
            Long id,
            Long opcionRespuestaId,
            String nombre,
            String enunciado,
            String descripcion,
            Map<Long, BigDecimal> ponderaciones
    ) {
        Pregunta p = preguntaDao.findById(id)
                .filter(Pregunta::isActiva)
                .orElseThrow(() -> new BusinessException("Pregunta no encontrada"));

        List<String> errores = new ArrayList<>();
        String nom = normUpper(nombre);
        String enun = normUpper(enunciado);
        String desc = descripcion == null ? null : normUpper(descripcion);

        if (nom.isEmpty()) errores.add("El nombre de pregunta es obligatorio");
        if (enun.isEmpty()) errores.add("El enunciado de pregunta es obligatorio");
        if (nom.length() > 120) errores.add("El nombre excede la longitud máxima de 120 caracteres");
        if (desc != null && desc.length() > 255) errores.add("La descripción excede la longitud máxima de 255 caracteres");
        if (!errores.isEmpty()) {
            throw new BusinessException("Validaciones no superadas al modificar pregunta", errores);
        }

        preguntaDao.findByFactorIdAndActivaTrueOrderByIdAsc(p.getFactor().getId()).stream()
                .filter(otra -> !otra.getId().equals(id) && otra.getNombre().equalsIgnoreCase(nom))
                .findFirst()
                .ifPresent(otra -> {
                    throw new BusinessException("La pregunta ya existe definida para esa competencia y factor");
                });

        OpcionRespuesta op = opcionRespuestaId != null
                ? opcionRespuestaDao.findById(opcionRespuestaId).filter(OpcionRespuesta::isActiva)
                    .orElseThrow(() -> new BusinessException("Opción de respuesta no encontrada"))
                : p.getOpcionRespuesta();

        Map<Long, BigDecimal> pondFinal = normalizarYValidarPonderaciones(op, ponderaciones);

        p.setOpcionRespuesta(op);
        p.setNombre(nom);
        p.setEnunciado(enun);
        p.setDescripcion(desc);
        p.setPonderacionesPorDetalle(pondFinal);
        return preguntaDao.save(p);
    }

    @Transactional
    public void darBajaPregunta(Long id, String usuarioActor) {
        bajaPregunta.ejecutar(id, usuarioActor);
    }

    private Map<Long, BigDecimal> normalizarYValidarPonderaciones(OpcionRespuesta op, Map<Long, BigDecimal> ponderaciones) {
        if (op.getDetalles().isEmpty()) {
            throw new BusinessException("La opción de respuesta no posee ítems configurados");
        }
        if (ponderaciones == null || ponderaciones.isEmpty()) {
            throw new BusinessException("Las ponderaciones son obligatorias");
        }
        Set<Long> idsValidos = new HashSet<>();
        for (DetalleOpcionRespuesta det : op.getDetalles()) {
            idsValidos.add(det.getId());
        }
        if (!ponderaciones.keySet().equals(idsValidos)) {
            throw new BusinessException("Las ponderaciones deben corresponder exactamente a los ítems "
                    + "de la opción de respuesta elegida");
        }

        BigDecimal suma = BigDecimal.ZERO;
        Map<Long, BigDecimal> resultado = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : ponderaciones.entrySet()) {
            BigDecimal val = e.getValue() == null ? BigDecimal.ZERO : e.getValue();
            if (val.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Las ponderaciones de las opciones deben ser 0 o positivas");
            }
            if (val.stripTrailingZeros().scale() > 2) {
                throw new BusinessException("Las ponderaciones admiten hasta 2 decimales");
            }
            suma = suma.add(val);
            resultado.put(e.getKey(), val);
        }
        if (suma.compareTo(BigDecimal.TEN) != 0) {
            throw new BusinessException("Las ponderaciones de las opciones de respuesta deben sumar 10 puntos (actual: " + suma + ")");
        }
        return resultado;
    }

    public List<Puesto> buscarPuestos(String codigo, String nombre, String empresa) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase();
        String n = nombre == null ? "" : nombre.trim().toUpperCase();
        String e = empresa == null ? "" : empresa.trim().toUpperCase();
        return puestoDao.findByActivoTrueOrderByNombreAsc().stream()
                .filter(p -> c.isEmpty() || p.getCodigo().toUpperCase().contains(c))
                .filter(p -> n.isEmpty() || p.getNombre().toUpperCase().contains(n))
                .filter(p -> e.isEmpty() || p.getEmpresa().toUpperCase().contains(e))
                .toList();
    }

    /** el alta no pide código, se arma a partir del factor (PREG-GP-01-03) */
    private String generarCodigoPregunta(Factor factor) {
        String base = "PREG-" + factor.getCodigo().replaceFirst("^FAC-", "");
        if (base.length() > 26) {
            base = base.substring(0, 26);
        }
        long nro = preguntaDao.countByFactorId(factor.getId()) + 1;
        String codigo;
        do {
            codigo = base + "-" + String.format("%02d", nro++);
        } while (preguntaDao.findByCodigoIgnoreCase(codigo).isPresent());
        return codigo;
    }

    private String normUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}
