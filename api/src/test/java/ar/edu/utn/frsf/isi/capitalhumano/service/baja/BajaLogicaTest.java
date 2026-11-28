package ar.edu.utn.frsf.isi.capitalhumano.service.baja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.utn.frsf.isi.capitalhumano.dao.AuditoriaEliminacionDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.AuditoriaEliminacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** patrón Template Method: se prueba con una baja de prueba que anota lo que va ejecutando */
@ExtendWith(MockitoExtension.class)
class BajaLogicaTest {

    @Mock
    private AuditoriaEliminacionDao auditoriaDao;

    /** elemento mínimo para la prueba */
    static final class Elemento {
        final long id;
        boolean activo;

        Elemento(long id, boolean activo) {
            this.id = id;
            this.activo = activo;
        }
    }

    /** subclase concreta que anota el orden en que se llaman los métodos */
    static final class BajaDePrueba extends BajaLogica<Elemento> {
        final List<String> pasos = new ArrayList<>();
        final Elemento elemento;
        final String motivoEnUso;

        BajaDePrueba(AuditoriaEliminacionDao auditoriaDao, Elemento elemento, String motivoEnUso) {
            super(auditoriaDao);
            this.elemento = elemento;
            this.motivoEnUso = motivoEnUso;
        }

        @Override protected Optional<Elemento> buscarPorId(Long id) { pasos.add("buscar"); return id == elemento.id ? Optional.of(elemento) : Optional.empty(); }
        @Override protected boolean estaActiva(Elemento e) { return e.activo; }
        @Override protected String mensajeNoEncontrada() { return "Elemento no encontrado"; }
        @Override public Optional<String> motivoPorElQueNoSePuedeEliminar(Elemento e) { pasos.add("verificarUso"); return Optional.ofNullable(motivoEnUso); }
        @Override protected void darDeBajaAsociados(Elemento e, String usuario) { pasos.add("bajaAsociados"); }
        @Override protected void desactivar(Elemento e) { pasos.add("desactivar"); e.activo = false; }
        @Override protected void guardar(Elemento e) { pasos.add("guardar"); }
        @Override protected String tipoEntidad() { return "ELEMENTO"; }
        @Override protected String nombreParaMensajes() { return "elemento"; }
        @Override protected Long idDe(Elemento e) { return e.id; }
        @Override protected String identificadorDe(Elemento e) { return "ELEMENTO " + e.id; }
    }

    @Test
    void ejecutar_siempreSigueElMismoOrdenYTerminaAuditando() {
        BajaDePrueba baja = new BajaDePrueba(auditoriaDao, new Elemento(7L, true), null);

        baja.ejecutar(7L, "  mgomez");

        assertEquals(List.of("buscar", "verificarUso", "bajaAsociados", "desactivar", "guardar"), baja.pasos);
        ArgumentCaptor<AuditoriaEliminacion> auditoria = ArgumentCaptor.forClass(AuditoriaEliminacion.class);
        verify(auditoriaDao).save(auditoria.capture());
        assertEquals("ELEMENTO", auditoria.getValue().getEntidad());
        assertEquals(7L, auditoria.getValue().getEntidadId());
        assertEquals("ELEMENTO 7", auditoria.getValue().getIdentificador());
        assertEquals("MGOMEZ", auditoria.getValue().getUsuarioActor());
        assertEquals("Baja lógica de elemento", auditoria.getValue().getDetalle());
    }

    @Test
    void ejecutar_siEstaEnUso_seDetieneAntesDeModificarNada() {
        Elemento enUso = new Elemento(7L, true);
        BajaDePrueba baja = new BajaDePrueba(auditoriaDao, enUso, "El elemento está en uso");

        BusinessException e = assertThrows(BusinessException.class, () -> baja.ejecutar(7L, "MGOMEZ"));

        assertEquals("El elemento está en uso", e.getMessage());
        assertEquals(List.of("buscar", "verificarUso"), baja.pasos);
        assertTrue(enUso.activo);
        verifyNoInteractions(auditoriaDao);
    }

    @Test
    void ejecutar_elementoInexistenteOInactivo_informaConElMensajeDeLaSubclase() {
        BajaDePrueba baja = new BajaDePrueba(auditoriaDao, new Elemento(7L, false), null);

        assertEquals("Elemento no encontrado", assertThrows(BusinessException.class, () -> baja.ejecutar(7L, "X")).getMessage());
        assertEquals("Elemento no encontrado", assertThrows(BusinessException.class, () -> baja.ejecutar(8L, "X")).getMessage());
        verifyNoInteractions(auditoriaDao);
    }

    @Test
    void ganchosPorDefecto_permitenEliminarYNoTienenAsociados() {
        // BajaPregunta no redefine los ganchos, usa los de la clase base
        BajaPregunta baja = new BajaPregunta(null, auditoriaDao);

        assertEquals(Optional.empty(), baja.motivoPorElQueNoSePuedeEliminar(null));
        baja.darDeBajaAsociados(null, "X");
        verifyNoInteractions(auditoriaDao);
    }
}
