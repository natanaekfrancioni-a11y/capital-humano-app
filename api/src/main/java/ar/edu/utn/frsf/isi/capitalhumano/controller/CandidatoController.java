package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.service.CandidatoImportService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU006 - Importar candidatos desde el archivo CSV del sistema de gestión de candidatos y CV. */
@RestController
@RequestMapping("/api/candidatos")
@CrossOrigin(origins = "*")
public class CandidatoController {

    private final CandidatoImportService importService;

    public CandidatoController(CandidatoImportService importService) {
        this.importService = importService;
    }

    /** contenido: texto completo del CSV (separado por ';', con fila de encabezados). */
    public record ImportarCandidatosRequest(String nombreArchivo, String contenido) {}

    @PostMapping("/importar")
    public ResponseEntity<Map<String, Object>> importar(@RequestBody ImportarCandidatosRequest req) {
        return ResponseEntity.ok(importService.importarCsv(req != null ? req.contenido() : null));
    }
}
