package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.model.PuestoCompetencia;
import ar.edu.utn.frsf.isi.capitalhumano.service.estado.EstadosCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.exportacion.ArchivoCsv;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU007 - Exportar resultados: genera un CSV con los resultados de los candidatos evaluados en una función.
 *
 * Un candidato está evaluado cuando su cuestionario está Completo. El archivo tiene una fila por
 * candidato y competencia, con las columnas y el nombre (aaaa-mm-dd-hh-mm-Empresa-NombreFunción.csv)
 * que pide el enunciado. El archivo lo arma {@link ArchivoCsv} (patrón Builder); el guardado en la
 * carpeta que elige el consultor (pasos 7 a 9) lo hace la pantalla.
 */
@Service
@Transactional(readOnly = true)
public class ExportacionResultadosService {

    /** Columnas del archivo, en el orden del enunciado. */
    public static final List<String> COLUMNAS = List.of(
            "Nro candidato", "Apellido", "Nombre", "Tipo documento", "Nro documento",
            "Código Competencia", "Resultado", "Nombre puesto", "Código puesto", "Fecha finalización cuestionario");

    private static final DateTimeFormatter FORMATO_NOMBRE_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

    public record PuestoExportableDTO(Long id, String codigo, String nombre, String empresa, long evaluados) {}

    public record CandidatoEvaluadoDTO(
            Long cuestionarioId, String nroCandidato, String apellido, String nombre,
            String tipoDocumento, String nroDocumento, LocalDateTime fechaFin) {}

    public record ArchivoExportadoDTO(String nombreArchivo, String contenido, int filas) {}

    private final PuestoDao puestoDao;
    private final CuestionarioDao cuestionarioDao;
    private final ReporteService reporteService;

    public ExportacionResultadosService(PuestoDao puestoDao, CuestionarioDao cuestionarioDao, ReporteService reporteService) {
        this.puestoDao = puestoDao;
        this.cuestionarioDao = cuestionarioDao;
        this.reporteService = reporteService;
    }

    /** Pasos 2 y 3: funciones que cumplen los filtros. Hay que completar al menos uno. */
    public List<PuestoExportableDTO> buscarPuestos(String nombre, String empresa, LocalDate desde, LocalDate hasta) {
        String nombreBuscado = normalizar(nombre);
        String empresaBuscada = normalizar(empresa);
        if (nombreBuscado.isEmpty() && empresaBuscada.isEmpty() && desde == null && hasta == null) {
            throw new BusinessException("Debe completar al menos un filtro para buscar funciones");
        }
        validarRango(desde, hasta);
        boolean filtraPorFecha = desde != null || hasta != null;
        return puestoDao.findByActivoTrueOrderByNombreAsc().stream()
                .filter(p -> nombreBuscado.isEmpty() || normalizar(p.getNombre()).contains(nombreBuscado))
                .filter(p -> empresaBuscada.isEmpty() || normalizar(p.getEmpresa()).contains(empresaBuscada))
                .map(p -> new PuestoExportableDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getEmpresa(),
                        evaluados(p.getId(), desde, hasta).size()))
                // Con fechas, solo interesan las funciones que tienen candidatos evaluados en ese período.
                .filter(dto -> !filtraPorFecha || dto.evaluados() > 0)
                .toList();
    }

    /** Paso 5: candidatos evaluados en la función, dentro del período (si se indicó). */
    public List<CandidatoEvaluadoDTO> listarEvaluados(Long puestoId, LocalDate desde, LocalDate hasta) {
        Puesto puesto = buscarPuesto(puestoId);
        validarRango(desde, hasta);
        return evaluados(puesto.getId(), desde, hasta).stream()
                .map(q -> {
                    Candidato c = q.getCandidato();
                    return new CandidatoEvaluadoDTO(q.getId(), c.getNroCandidato(), c.getApellido(), c.getNombre(),
                            c.getTipoDocumento(), c.getNroDocumento(), q.getFechaFin());
                })
                .toList();
    }

    public ArchivoExportadoDTO exportar(Long puestoId, LocalDate desde, LocalDate hasta) {
        return exportar(puestoId, desde, hasta, LocalDateTime.now());
    }

    /** Pasos 6 a 9: arma el CSV. 6.A: sin función seleccionada no se puede exportar. */
    public ArchivoExportadoDTO exportar(Long puestoId, LocalDate desde, LocalDate hasta, LocalDateTime ahora) {
        Puesto puesto = buscarPuesto(puestoId);
        validarRango(desde, hasta);
        List<Cuestionario> cuestionarios = evaluados(puesto.getId(), desde, hasta);
        if (cuestionarios.isEmpty()) {
            throw new BusinessException("La función " + puesto.getNombre() + " no tiene candidatos evaluados para exportar");
        }

        List<PuestoCompetencia> competencias = puesto.getCompetencias().stream()
                .sorted(Comparator.comparing(pc -> pc.getCompetencia().getCodigo()))
                .toList();
        ArchivoCsv.Builder archivo = ArchivoCsv.builder()
                .nombre(nombreArchivo(puesto, ahora))
                .columnas(COLUMNAS.toArray(String[]::new));
        for (Cuestionario q : cuestionarios) {
            Candidato c = q.getCandidato();
            Map<Long, BigDecimal> porCompetencia = reporteService.puntajesPorCompetencia(q);
            for (PuestoCompetencia pc : competencias) {
                BigDecimal resultado = porCompetencia.getOrDefault(pc.getCompetencia().getId(), BigDecimal.ZERO);
                archivo.fila(c.getNroCandidato(), c.getApellido(), c.getNombre(), c.getTipoDocumento(),
                        c.getNroDocumento(), pc.getCompetencia().getCodigo(), formatearResultado(resultado),
                        puesto.getNombre(), puesto.getCodigo(), q.getFechaFin().format(FORMATO_FECHA));
            }
        }
        ArchivoCsv csv = archivo.construir();
        return new ArchivoExportadoDTO(csv.getNombre(), csv.getContenido(), csv.getCantidadFilas());
    }

    /** aaaa-mm-dd-hh-mm-Empresa-NombreFunción.csv, sin los caracteres que Windows no admite en un nombre de archivo. */
    String nombreArchivo(Puesto puesto, LocalDateTime ahora) {
        return ahora.format(FORMATO_NOMBRE_ARCHIVO) + "-" + paraNombreDeArchivo(puesto.getEmpresa()) + "-"
                + paraNombreDeArchivo(puesto.getNombre()) + ".csv";
    }

    /** Cuestionarios completos de la función, con fecha de finalización dentro del período, ordenados por candidato. */
    private List<Cuestionario> evaluados(Long puestoId, LocalDate desde, LocalDate hasta) {
        return cuestionarioDao.findByPuestoIdOrderByIdAsc(puestoId).stream()
                .filter(q -> EstadosCuestionario.COMPLETO.nombre().equals(q.getEstado()))
                .filter(q -> q.getFechaFin() != null)
                .filter(q -> desde == null || !q.getFechaFin().toLocalDate().isBefore(desde))
                .filter(q -> hasta == null || !q.getFechaFin().toLocalDate().isAfter(hasta))
                .sorted(Comparator.comparing((Cuestionario q) -> q.getCandidato().getApellido())
                        .thenComparing(q -> q.getCandidato().getNombre()))
                .toList();
    }

    private Puesto buscarPuesto(Long puestoId) {
        if (puestoId == null) { // 6.A
            throw new BusinessException("Debe seleccionar una función");
        }
        return puestoDao.findById(puestoId)
                .orElseThrow(() -> new BusinessException("Función no encontrada"));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }

    /** Dos decimales con coma, como los muestra el sistema y los lee Excel en español. */
    private static String formatearResultado(BigDecimal resultado) {
        return resultado.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String paraNombreDeArchivo(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ").trim();
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toUpperCase(ES_AR);
    }
}
