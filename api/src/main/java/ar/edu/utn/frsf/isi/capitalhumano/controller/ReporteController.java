package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.CandidatoEvaluadoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.EvaluacionesPuestoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.PuestoResumenDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.ReporteComparativoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.ReporteOrdenMeritoDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CU027 - Emitir reporte comparativo y CU028 - Emitir orden de mérito. */
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** CU028 paso 4: puestos con cantidad de candidatos y de evaluaciones completas. */
    @GetMapping("/puestos")
    public ResponseEntity<List<PuestoResumenDTO>> buscarPuestos(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String empresa) {
        return ResponseEntity.ok(reporteService.buscarPuestos(codigo, nombre, empresa));
    }

    /** CU028 paso 6: evaluaciones del puesto (EVA-001, EVA-002, ...). */
    @GetMapping("/puestos/{puestoId}/evaluaciones")
    public ResponseEntity<EvaluacionesPuestoDTO> evaluaciones(@PathVariable Long puestoId) {
        return ResponseEntity.ok(reporteService.listarEvaluaciones(puestoId));
    }

    /** CU028 paso 7: evaluacion = TODAS o el código de una evaluación (EVA-001). */
    @GetMapping("/orden-merito")
    public ResponseEntity<ReporteOrdenMeritoDTO> ordenDeMerito(
            @RequestParam Long puestoId,
            @RequestParam(defaultValue = ReporteService.TODAS) String evaluacion,
            @RequestParam(required = false) String emitidoPor) {
        return ResponseEntity.ok(reporteService.emitirOrdenDeMerito(puestoId, evaluacion, emitidoPor));
    }

    /** CU027 paso 5: candidatos con cuestionario completo para el puesto. */
    @GetMapping("/comparativo/candidatos")
    public ResponseEntity<List<CandidatoEvaluadoDTO>> candidatosEvaluados(@RequestParam Long puestoId) {
        return ResponseEntity.ok(reporteService.listarCandidatosEvaluados(puestoId));
    }

    /** CU027 paso 7: requerido vs obtenido por competencia. */
    @GetMapping("/comparativo")
    public ResponseEntity<ReporteComparativoDTO> comparativo(@RequestParam Long cuestionarioId) {
        return ResponseEntity.ok(reporteService.generarReporteComparativo(cuestionarioId));
    }
}
