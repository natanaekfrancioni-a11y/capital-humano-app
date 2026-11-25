package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.RespuestaItemInput;
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

    public EvaluacionCuestionarioController(EvaluacionCuestionarioService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    public record GenerarEvaluacionesRequest(List<Long> candidatoIds, Long puestoId) {}

    public record ResponderBloqueRequest(List<RespuestaItemInput> respuestas) {}

    @GetMapping("/candidatos")
    public ResponseEntity<List<Candidato>> buscarCandidatos(
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nroCandidato) {
        return ResponseEntity.ok(evaluacionService.buscarCandidatos(apellido, nombre, nroCandidato));
    }

    @PostMapping("/generar")
    public ResponseEntity<List<Cuestionario>> generarEvaluaciones(@RequestBody GenerarEvaluacionesRequest req) {
        List<Cuestionario> creados = evaluacionService.generarEvaluaciones(
                req != null ? req.candidatoIds() : null,
                req != null ? req.puestoId() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    @PostMapping("/cuestionarios/{id}/iniciar")
    public ResponseEntity<Cuestionario> iniciarOSortear(@PathVariable Long id) {
        return ResponseEntity.ok(evaluacionService.iniciarOSortearCuestionario(id));
    }

    @PostMapping("/cuestionarios/{id}/bloques/{nroBloque}/responder")
    public ResponseEntity<Cuestionario> responderBloque(
            @PathVariable Long id,
            @PathVariable Integer nroBloque,
            @RequestBody ResponderBloqueRequest req) {
        return ResponseEntity.ok(
                evaluacionService.responderBloque(id, nroBloque, req != null ? req.respuestas() : null)
        );
    }

    @GetMapping("/orden-merito")
    public ResponseEntity<Map<String, Object>> emitirOrdenDeMerito(@RequestParam Long puestoId) {
        return ResponseEntity.ok(evaluacionService.emitirOrdenDeMerito(puestoId));
    }
}
