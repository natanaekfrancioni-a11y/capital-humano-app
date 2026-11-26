package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.*;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.*;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaCompetencia;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaFactor;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaLogica;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaOpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaPregunta;
import ar.edu.utn.frsf.isi.capitalhumano.service.baja.BajaPuesto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogoService {

    private final CompetenciaDao competenciaDao;
    private final FactorDao factorDao;
    private final OpcionRespuestaDao opcionRespuestaDao;
    private final PreguntaDao preguntaDao;
    private final PuestoDao puestoDao;
    private final CuestionarioDao cuestionarioDao;

    // Bajas lógicas (patrón Template Method): cada una completa los pasos variables de BajaLogica.
    private final BajaLogica<Competencia> bajaCompetencia;
    private final BajaLogica<Factor> bajaFactor;
    private final BajaLogica<Pregunta> bajaPregunta;
    private final BajaLogica<OpcionRespuesta> bajaOpcionRespuesta;
    private final BajaLogica<Puesto> bajaPuesto;

    public CatalogoService(
            CompetenciaDao competenciaDao,
            FactorDao factorDao,
            OpcionRespuestaDao opcionRespuestaDao,
            PreguntaDao preguntaDao,
            PuestoDao puestoDao,
            AuditoriaEliminacionDao auditoriaDao,
            CuestionarioDao cuestionarioDao
    ) {
        this.competenciaDao = competenciaDao;
        this.factorDao = factorDao;
        this.opcionRespuestaDao = opcionRespuestaDao;
        this.preguntaDao = preguntaDao;
        this.puestoDao = puestoDao;
        this.cuestionarioDao = cuestionarioDao;
        this.bajaCompetencia = new BajaCompetencia(competenciaDao, factorDao, puestoDao, auditoriaDao);
        this.bajaFactor = new BajaFactor(factorDao, preguntaDao, auditoriaDao);
        this.bajaPregunta = new BajaPregunta(preguntaDao, auditoriaDao);
        this.bajaOpcionRespuesta = new BajaOpcionRespuesta(opcionRespuestaDao, preguntaDao, auditoriaDao);
        this.bajaPuesto = new BajaPuesto(puestoDao, cuestionarioDao, auditoriaDao);
    }

    /** Resultado de la verificación previa a una baja (CU011 y CU015, paso 2). */
    public record EnUsoDTO(boolean enUso, String mensaje) {}

    public List<Competencia> buscarCompetencias(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return competenciaDao.findByActivaTrueOrderByNombreAsc();
        }
        return competenciaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim());
    }

    @Transactional
    public Competencia crearCompetencia(String codigo, String nombre, String descripcion, String tipo) {
        List<String> errores = new ArrayList<>();
        String cod = normUpper(codigo);
        String nom = normUpper(nombre);
        String desc = normUpper(descripcion);
        String tip = tipo == null || tipo.isBlank() ? "TECNICA" : normUpper(tipo);

        if (cod.isEmpty()) errores.add("El código es obligatorio");
        if (nom.isEmpty()) errores.add("El nombre es obligatorio");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos para crear competencia", errores);
        }

        if (competenciaDao.findByCodigoIgnoreCase(cod).isPresent() || competenciaDao.findByNombreIgnoreCase(nom).isPresent()) {
            throw new BusinessException("El código y/o nombre de competencia ya existe");
        }

        Competencia c = new Competencia();
        c.setCodigo(cod);
        c.setNombre(nom);
        c.setDescripcion(desc);
        c.setTipo(tip);
        c.setActiva(true);
        return competenciaDao.save(c);
    }

    @Transactional
    public Competencia modificarCompetencia(Long id, String nombre, String descripcion) {
        Competencia c = competenciaDao.findById(id)
                .filter(Competencia::isActiva)
                .orElseThrow(() -> new BusinessException("Competencia no encontrada"));

        List<String> errores = new ArrayList<>();
        String nom = normUpper(nombre);
        String desc = normUpper(descripcion);
        if (nom.isEmpty()) errores.add("El nombre es obligatorio");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos al modificar competencia", errores);
        }

        competenciaDao.findByNombreIgnoreCase(nom)
                .filter(otra -> !otra.getId().equals(id) && otra.isActiva())
                .ifPresent(otra -> {
                    throw new BusinessException("El nombre de competencia ya existe");
                });

        c.setNombre(nom);
        c.setDescripcion(desc);
        return competenciaDao.save(c);
    }

    @Transactional
    public void darBajaCompetencia(Long id, String usuarioActor) {
        bajaCompetencia.ejecutar(id, usuarioActor);
    }

    /** CU015 paso 2: la competencia está en uso si tiene factores o está asociada a un puesto. */
    public EnUsoDTO verificarUsoCompetencia(Long id) {
        Competencia c = bajaCompetencia.buscarActiva(id);
        return bajaCompetencia.motivoPorElQueNoSePuedeEliminar(c)
                .map(motivo -> new EnUsoDTO(true, motivo))
                .orElseGet(() -> new EnUsoDTO(false, "Los datos de la competencia " + c.getNombre() + " serán eliminados del sistema"));
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

    @Transactional
    public Factor crearFactor(Long competenciaId, String codigo, String nombre, String descripcion, Integer nroOrden) {
        List<String> errores = new ArrayList<>();
        String cod = normUpper(codigo);
        String nom = normUpper(nombre);
        String desc = normUpper(descripcion);

        if (competenciaId == null) errores.add("La competencia es obligatoria");
        if (cod.isEmpty()) errores.add("El código es obligatorio");
        if (nom.isEmpty()) errores.add("El nombre es obligatorio");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (nroOrden == null || nroOrden <= 0) errores.add("El número de orden debe ser mayor a 0");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos para crear factor", errores);
        }

        Competencia comp = competenciaDao.findById(competenciaId)
                .filter(Competencia::isActiva)
                .orElseThrow(() -> new BusinessException("Competencia no encontrada"));

        if (factorDao.findByCodigoIgnoreCase(cod).isPresent()
                || factorDao.existsByCompetenciaIdAndNombreIgnoreCaseAndActivoTrue(competenciaId, nom)) {
            throw new BusinessException("El código y/o nombre de factor ya existe");
        }

        if (factorDao.existsByCompetenciaIdAndNroOrdenAndActivoTrue(competenciaId, nroOrden)) {
            throw new BusinessException("El número de orden ya existe para la competencia seleccionada");
        }

        Factor f = new Factor();
        f.setCompetencia(comp);
        f.setCodigo(cod);
        f.setNombre(nom);
        f.setDescripcion(desc);
        f.setNroOrden(nroOrden);
        f.setActivo(true);
        return factorDao.save(f);
    }

    @Transactional
    public void darBajaFactor(Long id, String usuarioActor) {
        bajaFactor.ejecutar(id, usuarioActor);
    }

    public List<OpcionRespuesta> buscarOpcionesRespuesta(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return opcionRespuestaDao.findByActivaTrueOrderByNombreAsc();
        }
        return opcionRespuestaDao.findByActivaTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim());
    }

    /** Ítem de la lista de opciones. En el alta no lleva id; al modificar (CU030) identifica la opción existente. */
    public record ItemOpcionInput(Long id, Integer nroOrden, String descripcion) {}

    @Transactional
    public OpcionRespuesta crearOpcionRespuesta(String nombre, String descripcion, List<ItemOpcionInput> items) {
        List<String> errores = new ArrayList<>();
        String nom = normUpper(nombre);
        String desc = normUpper(descripcion);
        if (nom.isEmpty()) errores.add("El nombre es obligatorio");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (items == null || items.isEmpty()) errores.add("Debe ingresar al menos una opción");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos para crear opción de respuesta", errores);
        }

        if (opcionRespuestaDao.findByNombreIgnoreCase(nom).isPresent()) {
            throw new BusinessException("El nombre de la opción de respuesta ya existe");
        }

        validarItemsConsecutivosYSinDuplicados(items);

        OpcionRespuesta op = new OpcionRespuesta();
        op.setNombre(nom);
        op.setDescripcion(desc);
        op.setActiva(true);

        List<DetalleOpcionRespuesta> detalles = new ArrayList<>();
        for (ItemOpcionInput item : items) {
            DetalleOpcionRespuesta det = new DetalleOpcionRespuesta();
            det.setOpcionRespuesta(op);
            det.setNroOrden(item.nroOrden());
            det.setDescripcion(normUpper(item.descripcion()));
            detalles.add(det);
        }
        op.setDetalles(detalles);
        return opcionRespuestaDao.save(op);
    }

    @Transactional
    public OpcionRespuesta modificarOpcionRespuesta(Long id, String descripcion, List<ItemOpcionInput> items) {
        OpcionRespuesta op = opcionRespuestaDao.findById(id)
                .filter(OpcionRespuesta::isActiva)
                .orElseThrow(() -> new BusinessException("Opción de respuesta no encontrada"));

        // 2.A: datos blanqueados
        List<String> errores = new ArrayList<>();
        String desc = normUpper(descripcion);
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (items != null && items.stream().anyMatch(it -> normUpper(it.descripcion()).isEmpty())) {
            errores.add("La descripción de cada opción es obligatoria");
        }
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos para modificar la opción de respuesta", errores);
        }

        if (items != null && !items.isEmpty()) {
            // Paso 1: se pueden cambiar el orden y la descripción de cada opción, pero no agregar ni quitar opciones.
            Map<Long, DetalleOpcionRespuesta> actuales = new HashMap<>();
            for (DetalleOpcionRespuesta det : op.getDetalles()) {
                actuales.put(det.getId(), det);
            }
            Set<Long> recibidos = new HashSet<>();
            for (ItemOpcionInput it : items) {
                if (it.id() != null) recibidos.add(it.id());
            }
            if (items.size() != actuales.size() || !recibidos.equals(actuales.keySet())) {
                throw new BusinessException("No se pueden agregar ni quitar opciones de una opción de respuesta existente");
            }
            // 2.B (orden no consecutivo) y 2.C (descripción repetida)
            validarItemsConsecutivosYSinDuplicados(items);

            boolean hayCambios = items.stream().anyMatch(it -> {
                DetalleOpcionRespuesta det = actuales.get(it.id());
                return !det.getNroOrden().equals(it.nroOrden()) || !det.getDescripcion().equals(normUpper(it.descripcion()));
            });
            if (hayCambios) {
                // La base no admite dos opciones con el mismo orden ni con la misma descripción. Para poder
                // intercambiarlos, primero se pasan todas a valores provisorios y después a los definitivos.
                int provisorio = 1000;
                for (DetalleOpcionRespuesta det : op.getDetalles()) {
                    det.setNroOrden(provisorio++);
                    det.setDescripcion("~" + det.getId());
                }
                opcionRespuestaDao.saveAndFlush(op);
                for (ItemOpcionInput it : items) {
                    DetalleOpcionRespuesta det = actuales.get(it.id());
                    det.setNroOrden(it.nroOrden());
                    det.setDescripcion(normUpper(it.descripcion()));
                }
            }
        }
        op.setDescripcion(desc);
        return opcionRespuestaDao.saveAndFlush(op);
    }

    private void validarItemsConsecutivosYSinDuplicados(List<ItemOpcionInput> items) {
        Set<String> descripcionesVistas = new HashSet<>();
        List<Integer> ordenes = new ArrayList<>();
        for (ItemOpcionInput it : items) {
            String d = normUpper(it.descripcion());
            if (d.isEmpty()) {
                throw new BusinessException("La descripción de cada ítem es obligatoria");
            }
            if (!descripcionesVistas.add(d)) {
                throw new BusinessException("La descripción de opción está más de una vez en la Opción de Respuesta");
            }
            if (it.nroOrden() == null || it.nroOrden() <= 0) {
                throw new BusinessException("El orden de visualización debe ser positivo");
            }
            ordenes.add(it.nroOrden());
        }
        Collections.sort(ordenes);
        for (int i = 0; i < ordenes.size(); i++) {
            if (ordenes.get(i) != i + 1) {
                throw new BusinessException("El orden de visualización no es consecutivo");
            }
        }
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

        // CU021 2.C: la existencia se valida por nombre y por el texto de la pregunta
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
            BigDecimal equitativa = BigDecimal.valueOf(10.0)
                    .divide(BigDecimal.valueOf(op.getDetalles().size()), 2, java.math.RoundingMode.HALF_UP);
            Map<Long, BigDecimal> mapa = new LinkedHashMap<>();
            BigDecimal acumulado = BigDecimal.ZERO;
            for (int i = 0; i < op.getDetalles().size(); i++) {
                DetalleOpcionRespuesta det = op.getDetalles().get(i);
                if (i == op.getDetalles().size() - 1) {
                    mapa.put(det.getId(), BigDecimal.valueOf(10.00).subtract(acumulado));
                } else {
                    mapa.put(det.getId(), equitativa);
                    acumulado = acumulado.add(equitativa);
                }
            }
            return mapa;
        }

        BigDecimal suma = BigDecimal.ZERO;
        Map<Long, BigDecimal> resultado = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : ponderaciones.entrySet()) {
            BigDecimal val = e.getValue() == null ? BigDecimal.ZERO : e.getValue();
            if (val.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Las ponderaciones de las opciones deben ser 0 o positivas");
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

    public record CompetenciaPuestoInput(Long competenciaId, BigDecimal ponderacionMinima) {}

    @Transactional
    public Puesto crearPuesto(
            String codigo,
            String nombre,
            String empresa,
            String descripcion,
            List<CompetenciaPuestoInput> competencias
    ) {
        List<String> errores = new ArrayList<>();
        String cod = normUpper(codigo);
        String nom = normUpper(nombre);
        String emp = normUpper(empresa);
        String desc = normUpper(descripcion);

        if (cod.isEmpty()) errores.add("El código del puesto es obligatorio");
        if (nom.isEmpty()) errores.add("El nombre del puesto es obligatorio");
        if (emp.isEmpty()) errores.add("La empresa es obligatoria");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (competencias == null || competencias.isEmpty()) errores.add("Debe asociar al menos una competencia");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos para crear puesto", errores);
        }

        if (puestoDao.findByCodigoIgnoreCase(cod).isPresent() || puestoDao.findByNombreIgnoreCase(nom).isPresent()) {
            throw new BusinessException("El código y/o nombre del puesto ya existen");
        }

        Set<Long> vistas = new HashSet<>();
        Puesto puesto = new Puesto();
        puesto.setCodigo(cod);
        puesto.setNombre(nom);
        puesto.setEmpresa(emp);
        puesto.setDescripcion(desc);
        puesto.setActivo(true);

        List<PuestoCompetencia> lista = new ArrayList<>();
        for (CompetenciaPuestoInput cp : competencias) {
            if (cp.competenciaId() == null || !vistas.add(cp.competenciaId())) {
                throw new BusinessException("Cada competencia puede existir una sola vez para cada puesto");
            }
            if (cp.ponderacionMinima() == null
                    || cp.ponderacionMinima().compareTo(BigDecimal.ZERO) < 0
                    || cp.ponderacionMinima().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessException("Las ponderaciones son valores entre 0 y 10");
            }
            Competencia comp = competenciaDao.findById(cp.competenciaId())
                    .filter(Competencia::isActiva)
                    .orElseThrow(() -> new BusinessException("Competencia no encontrada: " + cp.competenciaId()));

            PuestoCompetencia pc = new PuestoCompetencia();
            pc.setPuesto(puesto);
            pc.setCompetencia(comp);
            pc.setPonderacionMinima(cp.ponderacionMinima());
            lista.add(pc);
        }
        puesto.setCompetencias(lista);
        return puestoDao.save(puesto);
    }

    // ------------------------------------------------------------------ Factores (CU018)

    @Transactional
    public Factor modificarFactor(Long id, String nombre, String descripcion, Integer nroOrden) {
        Factor f = factorDao.findById(id)
                .filter(Factor::isActivo)
                .orElseThrow(() -> new BusinessException("Factor no encontrado"));

        List<String> errores = new ArrayList<>();
        String nom = normUpper(nombre);
        String desc = normUpper(descripcion);
        if (nom.isEmpty()) errores.add("El nombre es obligatorio");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (nroOrden == null || nroOrden <= 0) errores.add("El número de orden debe ser mayor a 0");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos al modificar factor", errores);
        }

        Long competenciaId = f.getCompetencia().getId();
        boolean nombreRepetido = factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(competenciaId).stream()
                .anyMatch(otro -> !otro.getId().equals(id) && otro.getNombre().equalsIgnoreCase(nom));
        if (nombreRepetido) {
            throw new BusinessException("El nombre de factor ya existe para la competencia seleccionada");
        }
        boolean ordenRepetido = factorDao.findByCompetenciaIdAndActivoTrueOrderByNroOrdenAsc(competenciaId).stream()
                .anyMatch(otro -> !otro.getId().equals(id) && otro.getNroOrden().equals(nroOrden));
        if (ordenRepetido) {
            throw new BusinessException("El número de orden ya existe para la competencia seleccionada");
        }

        f.setNombre(nom);
        f.setDescripcion(desc);
        f.setNroOrden(nroOrden);
        return factorDao.save(f);
    }

    // ------------------------------------------------------------------ Opciones de respuesta (CU031)

    @Transactional
    public void darBajaOpcionRespuesta(Long id, String usuarioActor) {
        bajaOpcionRespuesta.ejecutar(id, usuarioActor);
    }

    // ------------------------------------------------------------------ Puestos (CU010 y CU011)

    @Transactional
    public Puesto modificarPuesto(
            Long id,
            String nombre,
            String empresa,
            String descripcion,
            List<CompetenciaPuestoInput> competencias
    ) {
        Puesto puesto = puestoDao.findById(id)
                .filter(Puesto::isActivo)
                .orElseThrow(() -> new BusinessException("Puesto no encontrado"));

        List<String> errores = new ArrayList<>();
        String nom = normUpper(nombre);
        String emp = normUpper(empresa);
        String desc = normUpper(descripcion);
        if (nom.isEmpty()) errores.add("El nombre del puesto es obligatorio");
        if (emp.isEmpty()) errores.add("La empresa es obligatoria");
        if (desc.isEmpty()) errores.add("La descripción es obligatoria");
        if (competencias == null || competencias.isEmpty()) errores.add("Debe asociar al menos una competencia");
        if (!errores.isEmpty()) {
            throw new BusinessException("Datos incompletos al modificar puesto", errores);
        }

        puestoDao.findByNombreIgnoreCase(nom)
                .filter(otro -> !otro.getId().equals(id) && otro.isActivo())
                .ifPresent(otro -> {
                    throw new BusinessException("El nombre del puesto ya existe");
                });

        Map<Long, BigDecimal> nuevas = new LinkedHashMap<>();
        for (CompetenciaPuestoInput cp : competencias) {
            if (cp.competenciaId() == null || nuevas.containsKey(cp.competenciaId())) {
                throw new BusinessException("Cada competencia puede existir una sola vez para cada puesto");
            }
            if (cp.ponderacionMinima() == null
                    || cp.ponderacionMinima().compareTo(BigDecimal.ZERO) < 0
                    || cp.ponderacionMinima().compareTo(BigDecimal.TEN) > 0) {
                throw new BusinessException("Las ponderaciones son valores entre 0 y 10");
            }
            nuevas.put(cp.competenciaId(), cp.ponderacionMinima());
        }

        Set<Long> actuales = new HashSet<>();
        for (PuestoCompetencia pc : puesto.getCompetencias()) {
            actuales.add(pc.getCompetencia().getId());
        }
        if (!actuales.equals(nuevas.keySet()) && cuestionarioDao.existsByPuestoId(id)) {
            throw new BusinessException("No es posible agregar o quitar competencias porque ya existen "
                    + "cuestionarios generados para el puesto");
        }

        // Se modifica la misma colección (orphanRemoval): se quitan, actualizan y agregan solo las diferencias.
        puesto.getCompetencias().removeIf(pc -> !nuevas.containsKey(pc.getCompetencia().getId()));
        for (PuestoCompetencia pc : puesto.getCompetencias()) {
            pc.setPonderacionMinima(nuevas.get(pc.getCompetencia().getId()));
        }
        for (Map.Entry<Long, BigDecimal> e : nuevas.entrySet()) {
            if (!actuales.contains(e.getKey())) {
                Competencia comp = competenciaDao.findById(e.getKey())
                        .filter(Competencia::isActiva)
                        .orElseThrow(() -> new BusinessException("Competencia no encontrada: " + e.getKey()));
                PuestoCompetencia pc = new PuestoCompetencia();
                pc.setPuesto(puesto);
                pc.setCompetencia(comp);
                pc.setPonderacionMinima(e.getValue());
                puesto.getCompetencias().add(pc);
            }
        }

        puesto.setNombre(nom);
        puesto.setEmpresa(emp);
        puesto.setDescripcion(desc);
        return puestoDao.save(puesto);
    }

    /** CU011 paso 2: el puesto está en uso si tiene evaluaciones (cuestionarios) asociadas. */
    public EnUsoDTO verificarUsoPuesto(Long id) {
        Puesto puesto = bajaPuesto.buscarActiva(id);
        return bajaPuesto.motivoPorElQueNoSePuedeEliminar(puesto)
                .map(motivo -> new EnUsoDTO(true, motivo))
                .orElseGet(() -> new EnUsoDTO(false, "Los datos del puesto " + puesto.getNombre() + " serán eliminados del sistema"));
    }

    @Transactional
    public void darBajaPuesto(Long id, String usuarioActor) {
        bajaPuesto.ejecutar(id, usuarioActor);
    }

    /** El alta de pregunta del enunciado no pide código: se arma a partir del factor (PREG-GP-01-03). */
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
