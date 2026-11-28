package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CandidatoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.UsuarioConsultorDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.Cuestionario;
import ar.edu.utn.frsf.isi.capitalhumano.model.UsuarioConsultor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CU001 - Autenticar usuario (consultor y candidato) */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CONSULTOR_INVALIDO = "El usuario o la contraseña no son válidos";
    private static final String CANDIDATO_INVALIDO =
            "Los datos ingresados no son válidos o no existe un cuestionario para el Candidato";
    private static final List<String> VIGENTES = List.of("ACTIVO", "EN_PROCESO");

    @Mock
    private UsuarioConsultorDao usuarioConsultorDao;

    @Mock
    private CandidatoDao candidatoDao;

    @Mock
    private CuestionarioDao cuestionarioDao;

    @InjectMocks
    private AuthService service;

    private static UsuarioConsultor consultor(boolean activo) {
        UsuarioConsultor u = new UsuarioConsultor();
        u.setId(1L);
        u.setUsername("CONSULTOR1");
        u.setNombreCompleto("MARIANA GOMEZ");
        u.setActivo(activo);
        return u;
    }

    // consultor

    @Test
    void autenticarConsultor_usuarioActivo_devuelvePerfilYNombre() {
        when(usuarioConsultorDao.findByUsernameIgnoreCase("CONSULTOR1")).thenReturn(Optional.of(consultor(true)));

        Map<String, Object> sesion = service.autenticarConsultor("  consultor1 ", "secreto");

        assertEquals("CONSULTOR", sesion.get("perfil"));
        assertEquals("CONSULTOR1", sesion.get("username"));
        assertEquals("MARIANA GOMEZ", sesion.get("nombreCompleto"));
    }

    @Test
    void autenticarConsultor_sinUsuarioOContrasena_rechazaSinConsultarLaBase() {
        assertEquals(CONSULTOR_INVALIDO, assertThrows(BusinessException.class,
                () -> service.autenticarConsultor("   ", "secreto")).getMessage());
        assertEquals(CONSULTOR_INVALIDO, assertThrows(BusinessException.class,
                () -> service.autenticarConsultor("CONSULTOR1", null)).getMessage());
        assertEquals(CONSULTOR_INVALIDO, assertThrows(BusinessException.class,
                () -> service.autenticarConsultor(null, "")).getMessage());
        verifyNoInteractions(usuarioConsultorDao);
    }

    @Test
    void autenticarConsultor_superaLongitudesMaximas_rechaza() {
        assertThrows(BusinessException.class, () -> service.autenticarConsultor("USUARIODEMASIADOLARGO", "secreto"));
        assertThrows(BusinessException.class, () -> service.autenticarConsultor("CONSULTOR1", "x".repeat(21)));
        verifyNoInteractions(usuarioConsultorDao);
    }

    @Test
    void autenticarConsultor_usuarioInactivo_rechaza() {
        when(usuarioConsultorDao.findByUsernameIgnoreCase("CONSULTOR1")).thenReturn(Optional.of(consultor(false)));

        BusinessException e = assertThrows(BusinessException.class, () -> service.autenticarConsultor("CONSULTOR1", "secreto"));

        assertEquals(CONSULTOR_INVALIDO, e.getMessage());
    }

    @Test
    void autenticarConsultor_usuarioInexistente_rechaza() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.autenticarConsultor("NADIE", "secreto"));

        assertEquals(CONSULTOR_INVALIDO, e.getMessage());
    }

    // candidato

    @Test
    void autenticarCandidato_conClaveDeCuestionarioVigente_devuelveCuestionario() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        Cuestionario q = Datos.cuestionario(7L, sosa, null, "ACTIVO");
        when(candidatoDao.findByTipoDocumentoIgnoreCaseAndNroDocumento("DNI", "30000002")).thenReturn(Optional.of(sosa));
        when(cuestionarioDao.findFirstByCandidatoIdAndEstadoInOrderByIdDesc(2L, VIGENTES)).thenReturn(Optional.of(q));

        Map<String, Object> sesion = service.autenticarCandidatoCuestionario(" dni ", " 30000002 ", "a8k3m9p2");

        assertEquals("CANDIDATO", sesion.get("perfil"));
        assertEquals(2L, sesion.get("candidatoId"));
        assertEquals("SOSA, VALERIA SOFIA", sesion.get("candidatoNombre"));
        assertEquals(7L, sesion.get("cuestionarioId"));
        assertEquals("ACTIVO", sesion.get("estado"));
    }

    @Test
    void autenticarCandidato_datosIncompletos_rechaza() {
        assertEquals(CANDIDATO_INVALIDO, assertThrows(BusinessException.class,
                () -> service.autenticarCandidatoCuestionario(null, "30000002", "A8K3M9P2")).getMessage());
        assertEquals(CANDIDATO_INVALIDO, assertThrows(BusinessException.class,
                () -> service.autenticarCandidatoCuestionario("DNI", " ", "A8K3M9P2")).getMessage());
        assertEquals(CANDIDATO_INVALIDO, assertThrows(BusinessException.class,
                () -> service.autenticarCandidatoCuestionario("DNI", "30000002", null)).getMessage());
        verifyNoInteractions(candidatoDao, cuestionarioDao);
    }

    @Test
    void autenticarCandidato_eliminado_rechaza() {
        Candidato eliminado = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        eliminado.setEliminado(true);
        when(candidatoDao.findByTipoDocumentoIgnoreCaseAndNroDocumento("DNI", "30000002")).thenReturn(Optional.of(eliminado));

        assertThrows(BusinessException.class, () -> service.autenticarCandidatoCuestionario("DNI", "30000002", "A8K3M9P2"));
        verifyNoInteractions(cuestionarioDao);
    }

    @Test
    void autenticarCandidato_claveIncorrecta_rechaza() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        when(candidatoDao.findByTipoDocumentoIgnoreCaseAndNroDocumento("DNI", "30000002")).thenReturn(Optional.of(sosa));
        when(cuestionarioDao.findFirstByCandidatoIdAndEstadoInOrderByIdDesc(2L, VIGENTES))
                .thenReturn(Optional.of(Datos.cuestionario(7L, sosa, null, "EN_PROCESO")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.autenticarCandidatoCuestionario("DNI", "30000002", "OTRACLAV"));

        assertEquals(CANDIDATO_INVALIDO, e.getMessage());
    }

    @Test
    void autenticarCandidato_sinCuestionarioActivoNiEnProceso_rechaza() {
        Candidato sosa = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA SOFIA");
        when(candidatoDao.findByTipoDocumentoIgnoreCaseAndNroDocumento("DNI", "30000002")).thenReturn(Optional.of(sosa));

        assertThrows(BusinessException.class, () -> service.autenticarCandidatoCuestionario("DNI", "30000002", "A8K3M9P2"));
    }
}
