package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CandidatoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.UsuarioConsultorDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.UsuarioConsultor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UsuarioConsultorDao usuarioConsultorDao;
    private final CandidatoDao candidatoDao;
    private final CuestionarioDao cuestionarioDao;

    public AuthService(
            UsuarioConsultorDao usuarioConsultorDao,
            CandidatoDao candidatoDao,
            CuestionarioDao cuestionarioDao
    ) {
        this.usuarioConsultorDao = usuarioConsultorDao;
        this.candidatoDao = candidatoDao;
        this.cuestionarioDao = cuestionarioDao;
    }

    public Map<String, Object> autenticarConsultor(String username, String password) {
        String userNorm = username == null ? "" : username.trim().toUpperCase();
        String pass = password == null ? "" : password;

        if (userNorm.isEmpty() || pass.isEmpty()) {
            throw new BusinessException("El usuario o la contraseña no son válidos");
        }
        if (userNorm.length() > 15 || pass.length() > 20) {
            throw new BusinessException("El usuario o la contraseña no son válidos");
        }

        UsuarioConsultor consultor = usuarioConsultorDao.findByUsernameIgnoreCase(userNorm)
                .filter(UsuarioConsultor::isActivo)
                .orElseThrow(() -> new BusinessException("El usuario o la contraseña no son válidos"));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("perfil", "CONSULTOR");
        res.put("username", consultor.getUsername());
        res.put("nombreCompleto", consultor.getNombreCompleto());
        return res;
    }

    public Map<String, Object> autenticarCandidatoCuestionario(String tipoDocumento, String nroDocumento, String clave) {
        String tipoNorm = tipoDocumento == null ? "" : tipoDocumento.trim().toUpperCase();
        String docNorm = nroDocumento == null ? "" : nroDocumento.trim().toUpperCase();
        String claveNorm = clave == null ? "" : clave.trim().toUpperCase();

        if (tipoNorm.isEmpty() || docNorm.isEmpty() || claveNorm.isEmpty()) {
            throw new BusinessException("Los datos ingresados no son válidos o no existe un cuestionario para el Candidato");
        }

        Candidato candidato = candidatoDao.findByTipoDocumentoIgnoreCaseAndNroDocumento(tipoNorm, docNorm)
                .filter(c -> !c.isEliminado())
                .orElseThrow(() -> new BusinessException("Los datos ingresados no son válidos o no existe un cuestionario para el Candidato"));

        Cuestionario cuestionario = cuestionarioDao
                .findFirstByCandidatoIdAndEstadoInOrderByIdDesc(candidato.getId(), List.of("ACTIVO", "EN_PROCESO"))
                .filter(q -> q.getClaveAcceso().equalsIgnoreCase(claveNorm))
                .orElseThrow(() -> new BusinessException("Los datos ingresados no son válidos o no existe un cuestionario para el Candidato"));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("perfil", "CANDIDATO");
        res.put("candidatoId", candidato.getId());
        res.put("candidatoNombre", candidato.getApellido() + ", " + candidato.getNombre());
        res.put("cuestionarioId", cuestionario.getId());
        res.put("estado", cuestionario.getEstado());
        return res;
    }
}
