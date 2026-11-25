package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.service.AuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU001 - Autenticar usuario: consultor (usuario y contraseña) o candidato (documento y clave). */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginConsultorRequest(String username, String password) {}

    public record LoginCandidatoRequest(String tipoDocumento, String nroDocumento, String clave) {}

    @PostMapping("/consultor")
    public ResponseEntity<Map<String, Object>> loginConsultor(@RequestBody LoginConsultorRequest req) {
        return ResponseEntity.ok(authService.autenticarConsultor(
                req != null ? req.username() : null,
                req != null ? req.password() : null));
    }

    @PostMapping("/candidato")
    public ResponseEntity<Map<String, Object>> loginCandidato(@RequestBody LoginCandidatoRequest req) {
        return ResponseEntity.ok(authService.autenticarCandidatoCuestionario(
                req != null ? req.tipoDocumento() : null,
                req != null ? req.nroDocumento() : null,
                req != null ? req.clave() : null));
    }
}
