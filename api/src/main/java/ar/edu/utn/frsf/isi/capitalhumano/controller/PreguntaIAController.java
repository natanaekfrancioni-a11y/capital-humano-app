package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.service.GeneradorPreguntaIAService;
import ar.edu.utn.frsf.isi.capitalhumano.service.GeneradorPreguntaIAService.PreguntaGeneradaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU033 - Generar pregunta con Inteligencia Artificial. */
@RestController
@RequestMapping("/api/catalogo/preguntas")
@CrossOrigin(origins = "*")
public class PreguntaIAController {

    private final GeneradorPreguntaIAService generador;

    public PreguntaIAController(GeneradorPreguntaIAService generador) {
        this.generador = generador;
    }

    public record GenerarPreguntaRequest(Long competenciaId, Long factorId) {}

    /** Devuelve los datos de una pregunta nueva para completar el alta (CU021). No guarda nada. */
    @PostMapping("/generar-ia")
    public ResponseEntity<PreguntaGeneradaDTO> generar(@RequestBody GenerarPreguntaRequest req) {
        return ResponseEntity.ok(generador.generar(req.competenciaId(), req.factorId()));
    }
}
