package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService.ArchivoExportadoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService.CandidatoEvaluadoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ExportacionResultadosService.PuestoExportableDTO;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CU007 - Exportar resultados. Las fechas llegan en formato aaaa-mm-dd. */
@RestController
@RequestMapping("/api/exportacion")
@CrossOrigin(origins = "*")
public class ExportacionController {

    private final ExportacionResultadosService exportacionService;

    public ExportacionController(ExportacionResultadosService exportacionService) {
        this.exportacionService = exportacionService;
    }

    /** Pasos 2 y 3: búsqueda de funciones (al menos un filtro). */
    @GetMapping("/puestos")
    public ResponseEntity<List<PuestoExportableDTO>> buscarPuestos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(exportacionService.buscarPuestos(nombre, empresa, fecha(desde, "desde"), fecha(hasta, "hasta")));
    }

    /** Paso 5: candidatos evaluados en la función. */
    @GetMapping("/puestos/{puestoId}/candidatos")
    public ResponseEntity<List<CandidatoEvaluadoDTO>> listarEvaluados(
            @PathVariable Long puestoId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(exportacionService.listarEvaluados(puestoId, fecha(desde, "desde"), fecha(hasta, "hasta")));
    }

    /** Pasos 6 a 9: contenido y nombre del archivo CSV; la pantalla lo guarda en la carpeta elegida. */
    @GetMapping("/puestos/{puestoId}/csv")
    public ResponseEntity<ArchivoExportadoDTO> exportar(
            @PathVariable Long puestoId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(exportacionService.exportar(puestoId, fecha(desde, "desde"), fecha(hasta, "hasta")));
    }

    private static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("La fecha " + campo + " no es válida: use el formato aaaa-mm-dd");
        }
    }
}
