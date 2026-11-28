package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.CandidatoClaveDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.CompetenciaPuestoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.RespuestaItemInput;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService;
import ar.edu.utn.frsf.isi.capitalhumano.service.ReporteService.ReporteOrdenMeritoDTO;
import ar.edu.utn.frsf.isi.capitalhumano.service.VigenciaCuestionarioService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluaciones")
@CrossOrigin(origins = "*")
public class EvaluacionCuestionarioController {

    private final EvaluacionCuestionarioService evaluacionService;
    private final VigenciaCuestionarioService vigenciaService;
    private final ReporteService reporteService;

    public EvaluacionCuestionarioController(
            EvaluacionCuestionarioService evaluacionService,
            VigenciaCuestionarioService vigenciaService,
            ReporteService reporteService
    ) {
        this.evaluacionService = evaluacionService;
        this.vigenciaService = vigenciaService;
        this.reporteService = reporteService;
    }

    /** claves: las claves de ingreso que se mostraron en el CU025 (candidatoId -> clave) */
    public record GenerarEvaluacionesRequest(List<Long> candidatoIds, Long puestoId, Map<Long, String> claves) {}

    public record CandidatosRequest(List<Long> candidatoIds) {}

    public record ClavesRequest(List<Long> candidatoIds, Long puestoId) {}

    public record ResponderBloqueRequest(List<RespuestaItemInput> respuestas) {}

    // CU025 - Evaluar a candidatos

    @GetMapping("/candidatos")
    public ResponseEntity<List<Candidato>> buscarCandidatos(
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nroCandidato) {
        return ResponseEntity.ok(evaluacionService.buscarCandidatos(apellido, nombre, nroCandidato));
    }

    /** devuelve los candidatos que ya tienen cuestionarios activos o en proceso (vacío = todo OK) */
    @PostMapping("/validar-candidatos")
    public ResponseEntity<List<Candidato>> validarCandidatos(@RequestBody CandidatosRequest req) {
        return ResponseEntity.ok(evaluacionService.validarCuestionariosActivos(req != null ? req.candidatoIds() : null));
    }

    /** competencias del puesto con su ponderación requerida y si se pueden evaluar */
    @GetMapping("/puestos/{puestoId}/competencias")
    public ResponseEntity<List<CompetenciaPuestoDTO>> competenciasPuesto(@PathVariable Long puestoId) {
        return ResponseEntity.ok(evaluacionService.obtenerCompetenciasPuesto(puestoId));
    }

    /** genera (sin guardar) las claves de ingreso de cada candidato */
    @PostMapping("/claves")
    public ResponseEntity<List<CandidatoClaveDTO>> generarClaves(@RequestBody ClavesRequest req) {
        return ResponseEntity.ok(evaluacionService.generarClaves(
                req != null ? req.candidatoIds() : null,
                req != null ? req.puestoId() : null));
    }

    /** guarda la evaluación (un cuestionario ACTIVO por candidato) */
    @PostMapping("/generar")
    public ResponseEntity<List<Cuestionario>> generarEvaluaciones(@RequestBody GenerarEvaluacionesRequest req) {
        List<Cuestionario> creados = evaluacionService.generarEvaluaciones(
                req != null ? req.candidatoIds() : null,
                req != null ? req.puestoId() : null,
                req != null ? req.claves() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    // CU026 - Completar cuestionario

    /** instrucciones y parámetros del cuestionario (repositorio de configuraciones) */
    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionCuestionario> obtenerConfiguracion() {
        return ResponseEntity.ok(evaluacionService.obtenerConfiguracion());
    }

    @PostMapping("/cuestionarios/{id}/iniciar")
    public ResponseEntity<Cuestionario> iniciarOSortear(@PathVariable Long id) {
        vigenciaService.verificarVigencia(id, true);
        return ResponseEntity.ok(evaluacionService.iniciarOSortearCuestionario(id));
    }

    @PostMapping("/cuestionarios/{id}/bloques/{nroBloque}/responder")
    public ResponseEntity<Cuestionario> responderBloque(
            @PathVariable Long id,
            @PathVariable Integer nroBloque,
            @RequestBody ResponderBloqueRequest req) {
        vigenciaService.verificarVigencia(id, false);
        return ResponseEntity.ok(
                evaluacionService.responderBloque(id, nroBloque, req != null ? req.respuestas() : null)
        );
    }

    // CU028 (compatibilidad)

    /** se deja por compatibilidad, el reporte completo está en /api/reportes/orden-merito */
    @GetMapping("/orden-merito")
    public ResponseEntity<ReporteOrdenMeritoDTO> emitirOrdenDeMerito(@RequestParam Long puestoId) {
        return ResponseEntity.ok(reporteService.emitirOrdenDeMerito(puestoId, ReporteService.TODAS, null));
    }
}
