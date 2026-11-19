package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CandidatoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.ConfiguracionCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.CuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.FactorDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PreguntaDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.PuestoDao;
import ar.edu.utn.frsf.isi.capitalhumano.dao.RespuestaCuestionarioDao;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import ar.edu.utn.frsf.isi.capitalhumano.model.ConfiguracionCuestionario;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluacionCuestionarioServiceTest {

    @Mock
    private CandidatoDao candidatoDao;

    @Mock
    private PuestoDao puestoDao;

    @Mock
    private FactorDao factorDao;

    @Mock
    private PreguntaDao preguntaDao;

    @Mock
    private CuestionarioDao cuestionarioDao;

    @Mock
    private PreguntaCuestionarioDao preguntaCuestionarioDao;

    @Mock
    private RespuestaCuestionarioDao respuestaCuestionarioDao;

    @Mock
    private ConfiguracionCuestionarioDao configuracionDao;

    @InjectMocks
    private EvaluacionCuestionarioService service;

    @Test
    void deberiaInstanciarServicioConMocks() {
        assertNotNull(service);
    }

    @Test
    void deberiaObtenerConfiguracionDesdeDao() {
        ConfiguracionCuestionario configMock = new ConfiguracionCuestionario();
        when(configuracionDao.findAll()).thenReturn(List.of(configMock));

        ConfiguracionCuestionario resultado = service.obtenerConfiguracion();

        assertNotNull(resultado);
        verify(configuracionDao).findAll();
    }

    @Test
    void deberiaBuscarCandidatosDelegandoAlDao() {
        List<Candidato> esperados = List.of(new Candidato());
        when(candidatoDao.findAll()).thenReturn(esperados);

        List<Candidato> resultado = service.buscarCandidatos(null, null, null);

        assertNotNull(resultado);
        assertEquals(esperados.size(), resultado.size());
    }
}
