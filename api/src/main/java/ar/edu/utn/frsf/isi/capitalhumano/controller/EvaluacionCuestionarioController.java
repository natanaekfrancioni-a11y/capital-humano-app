package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService;
import ar.edu.utn.frsf.isi.capitalhumano.service.EvaluacionCuestionarioService.RespuestaItemInput;
import java.math.BigDecimal;
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

    private final EvaluacionCuestionarioService service;

    public EvaluacionCuestionarioController(EvaluacionCuestionarioService service) {
        this.service = service;
    }

    public record GenerarEvaluacionesRequest(List<Long> candidatoIds, Long puestoId) {}

    public record ResponderBloqueRequest(Integer nroBloque, List<RespuestaItemInput> respuestas) {}

    @GetMapping("/candidatos")
    public ResponseEntity<List<Candidato>> buscarCandidatos(
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nroCandidato) {
        return ResponseEntity.ok(service.buscarCandidatos(apellido, nombre, nroCandidato));
    }

    @PostMapping("/generar")
    public ResponseEntity<List<Cuestionario>> generarEvaluaciones(@RequestBody GenerarEvaluacionesRequest req) {
        List<Cuestionario> creados = service.generarEvaluaciones(req.candidatoIds(), req.puestoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    @PostMapping("/cuestionarios/{cuestionarioId}/iniciar-o-sortear")
    public ResponseEntity<Cuestionario> iniciarOSortear(@PathVariable Long cuestionarioId) {
        return ResponseEntity.ok(service.iniciarOSortearCuestionario(cuestionarioId));
    }

    @PostMapping("/cuestionarios/{cuestionarioId}/responder-bloque")
    public ResponseEntity<Cuestionario> responderBloque(
            @PathVariable Long cuestionarioId,
            @RequestBody ResponderBloqueRequest req) {
        return ResponseEntity.ok(service.responderBloque(cuestionarioId, req.nroBloque(), req.respuestas()));
    }

    @PostMapping("/cuestionarios/calcular-puntaje")
    public ResponseEntity<BigDecimal> calcularPuntajeTotal(@RequestBody Cuestionario cuestionario) {
        return ResponseEntity.ok(service.calcularPuntajeTotal(cuestionario));
    }

    @GetMapping("/puestos/{puestoId}/orden-merito")
    public ResponseEntity<Map<String, Object>> emitirOrdenDeMerito(@PathVariable Long puestoId) {
        return ResponseEntity.ok(service.emitirOrdenDeMerito(puestoId));
    }

    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionCuestionario> obtenerConfiguracion() {
        return ResponseEntity.ok(service.obtenerConfiguracion());
    }
}
