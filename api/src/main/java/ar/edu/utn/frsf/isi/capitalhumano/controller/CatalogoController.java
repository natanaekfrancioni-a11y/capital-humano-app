package ar.edu.utn.frsf.isi.capitalhumano.controller;

import ar.edu.utn.frsf.isi.capitalhumano.model.Competencia;
import ar.edu.utn.frsf.isi.capitalhumano.model.Factor;
import ar.edu.utn.frsf.isi.capitalhumano.model.OpcionRespuesta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Pregunta;
import ar.edu.utn.frsf.isi.capitalhumano.model.Puesto;
import ar.edu.utn.frsf.isi.capitalhumano.service.CatalogoService;
import ar.edu.utn.frsf.isi.capitalhumano.service.CatalogoService.CompetenciaPuestoInput;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogo")
@CrossOrigin(origins = "*")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    public record CrearPuestoRequest(
            String codigo,
            String nombre,
            String empresa,
            String descripcion,
            List<CompetenciaPuestoInput> competencias
    ) {}

    public record CrearPreguntaRequest(
            Long factorId,
            Long opcionRespuestaId,
            String codigo,
            String nombre,
            String enunciado,
            String descripcion,
            Map<Long, BigDecimal> ponderaciones
    ) {}

    public record ModificarPreguntaRequest(
            Long opcionRespuestaId,
            String nombre,
            String enunciado,
            String descripcion,
            Map<Long, BigDecimal> ponderaciones
    ) {}

    @GetMapping("/competencias")
    public ResponseEntity<List<Competencia>> buscarCompetencias(
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(catalogoService.buscarCompetencias(nombre));
    }

    @GetMapping("/factores")
    public ResponseEntity<List<Factor>> buscarFactores(
            @RequestParam(required = false) Long competenciaId,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(catalogoService.buscarFactores(competenciaId, nombre));
    }

    @GetMapping("/opciones-respuesta")
    public ResponseEntity<List<OpcionRespuesta>> buscarOpcionesRespuesta(
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(catalogoService.buscarOpcionesRespuesta(nombre));
    }

    @GetMapping("/puestos")
    public ResponseEntity<List<Puesto>> buscarPuestos(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String empresa) {
        return ResponseEntity.ok(catalogoService.buscarPuestos(codigo, nombre, empresa));
    }

    @PostMapping("/puestos")
    public ResponseEntity<Puesto> crearPuesto(@RequestBody CrearPuestoRequest req) {
        Puesto creado = catalogoService.crearPuesto(
                req.codigo(),
                req.nombre(),
                req.empresa(),
                req.descripcion(),
                req.competencias()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/preguntas")
    public ResponseEntity<List<Pregunta>> buscarPreguntas(
            @RequestParam(required = false) Long competenciaId,
            @RequestParam(required = false) Long factorId,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(catalogoService.buscarPreguntas(competenciaId, factorId, nombre));
    }

    @PostMapping("/preguntas")
    public ResponseEntity<Pregunta> crearPregunta(@RequestBody CrearPreguntaRequest req) {
        Pregunta creada = catalogoService.crearPregunta(
                req.factorId(),
                req.opcionRespuestaId(),
                req.codigo(),
                req.nombre(),
                req.enunciado(),
                req.descripcion(),
                req.ponderaciones()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/preguntas/{id}")
    public ResponseEntity<Pregunta> modificarPregunta(
            @PathVariable Long id,
            @RequestBody ModificarPreguntaRequest req) {
        Pregunta modificada = catalogoService.modificarPregunta(
                id,
                req.opcionRespuestaId(),
                req.nombre(),
                req.enunciado(),
                req.descripcion(),
                req.ponderaciones()
        );
        return ResponseEntity.ok(modificada);
    }

    @DeleteMapping("/preguntas/{id}")
    public ResponseEntity<Void> darBajaPregunta(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CONSULTOR") String usuario) {
        catalogoService.darBajaPregunta(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
