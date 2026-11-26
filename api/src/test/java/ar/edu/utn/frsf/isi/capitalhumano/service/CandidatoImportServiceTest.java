package ar.edu.utn.frsf.isi.capitalhumano.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CandidatoDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CU006 - Importar candidatos desde el CSV del sistema de candidatos y CV. */
@ExtendWith(MockitoExtension.class)
class CandidatoImportServiceTest {

    private static final String ENCABEZADO =
            "Nro candidato;Apellido;Nombre;Nro documento;Tipo documento;Fecha nacimiento;Genero;Nacionalidad;Email;Escolaridad;Eliminado";

    @Mock
    private CandidatoDao candidatoDao;

    @InjectMocks
    private CandidatoImportService service;

    private static String csv(String... filas) {
        return ENCABEZADO + "\r\n" + String.join("\r\n", filas);
    }

    @Test
    void importar_creaActualizaYDaDeBajaSegunElNroDeCandidato() {
        Candidato existente = Datos.candidato(1L, "CAND-001", "VIEJO", "APELLIDO");
        Candidato yaEliminado = Datos.candidato(2L, "CAND-002", "SOSA", "VALERIA");
        yaEliminado.setEliminado(true);
        when(candidatoDao.findByNroCandidato("CAND-001")).thenReturn(Optional.of(existente));
        when(candidatoDao.findByNroCandidato("CAND-002")).thenReturn(Optional.of(yaEliminado));
        when(candidatoDao.findByNroCandidato("CAND-010")).thenReturn(Optional.empty());
        when(candidatoDao.findByNroCandidato("CAND-011")).thenReturn(Optional.empty());

        // Con marca BOM, líneas en blanco y espacios: así suele venir un CSV exportado desde Excel.
        String contenido = "﻿" + csv(
                " cand-001 ; perez ; lucas ;35111222;dni;1990-05-10;h;argentina;lucas@mail.com;universitario;FALSE",
                "",
                "CAND-002;SOSA;VALERIA;37444555;DNI;1992-01-20;M;ARGENTINA;vale@mail.com;SECUNDARIO;true",
                "CAND-010;DIAZ;ANA;40111222;PP;2000-12-31;M;URUGUAYA;ana@mail.com;TERCIARIO;false",
                "CAND-011;ROMERO;JUAN;41000111;LE;1985-07-01;H;ARGENTINA;juan@mail.com;PRIMARIO;True",
                "   ");

        Map<String, Object> resumen = service.importarCsv(contenido);

        assertEquals(2, resumen.get("creados"));
        assertEquals(2, resumen.get("actualizados"));
        assertEquals(1, resumen.get("eliminados"), "CAND-011 nace eliminado; CAND-002 ya lo estaba");
        assertEquals(4, resumen.get("totalProcesados"));

        assertEquals("PEREZ", existente.getApellido());
        assertEquals("LUCAS", existente.getNombre());
        assertEquals("DNI", existente.getTipoDocumento());
        assertEquals(LocalDate.of(1990, 5, 10), existente.getFechaNacimiento());
        assertEquals("H", existente.getGenero());
        assertEquals("ARGENTINA", existente.getNacionalidad());
        assertEquals("UNIVERSITARIO", existente.getEscolaridad());
        assertEquals("lucas@mail.com", existente.getEmail());
        assertFalse(existente.isEliminado());
        assertTrue(yaEliminado.isEliminado());

        ArgumentCaptor<Candidato> guardados = ArgumentCaptor.forClass(Candidato.class);
        verify(candidatoDao, times(4)).save(guardados.capture());
        Candidato nuevo = guardados.getAllValues().get(2);
        assertEquals("CAND-010", nuevo.getNroCandidato());
        assertEquals("PP", nuevo.getTipoDocumento());
        assertFalse(nuevo.isEliminado());
        assertTrue(guardados.getAllValues().get(3).isEliminado());
    }

    @Test
    void importar_candidatoQuePasaAEliminado_cuentaComoBaja() {
        Candidato activo = Datos.candidato(1L, "CAND-001", "PEREZ", "LUCAS");
        when(candidatoDao.findByNroCandidato("CAND-001")).thenReturn(Optional.of(activo));

        Map<String, Object> resumen = service.importarCsv(
                csv("CAND-001;PEREZ;LUCAS;35111222;DNI;1990-05-10;H;ARGENTINA;lucas@mail.com;UNIVERSITARIO;true"));

        assertEquals(0, resumen.get("creados"));
        assertEquals(1, resumen.get("actualizados"));
        assertEquals(1, resumen.get("eliminados"));
        assertTrue(activo.isEliminado());
    }

    @Test
    void importar_archivoVacioOSoloConEncabezado_informa() {
        assertEquals("El archivo CSV está vacío",
                assertThrows(BusinessException.class, () -> service.importarCsv("  ")).getMessage());
        assertEquals("El archivo CSV está vacío",
                assertThrows(BusinessException.class, () -> service.importarCsv(null)).getMessage());
        assertEquals("El archivo CSV no contiene registros de candidatos",
                assertThrows(BusinessException.class, () -> service.importarCsv(ENCABEZADO + "\n\n")).getMessage());
        verifyNoInteractions(candidatoDao);
    }

    @Test
    void importar_filasConErrores_indicaLaFilaYElMotivo() {
        List<String[]> casos = List.of(
                new String[]{"CAND-001;PEREZ;LUCAS", "Fila 1 inválida: se esperan 11 columnas separadas por ';'"},
                new String[]{"CAND-001;;LUCAS;35111222;DNI;1990-05-10;H;AR;a@b.com;X;false", "Fila 1: faltan datos obligatorios del candidato"},
                new String[]{"CAND-001;PEREZ;LUCAS;35111222;CUIT;1990-05-10;H;AR;a@b.com;X;false", "Fila 1: tipo de documento inválido (CUIT)"},
                new String[]{"CAND-001;PEREZ;LUCAS;35111222;DNI;1990-05-10;X;AR;a@b.com;X;false", "Fila 1: género inválido (X)"},
                new String[]{"CAND-001;PEREZ;LUCAS;35111222;DNI;10/05/1990;H;AR;a@b.com;X;false", "Fila 1: fecha de nacimiento inválida (10/05/1990)"});

        for (String[] caso : casos) {
            BusinessException e = assertThrows(BusinessException.class, () -> service.importarCsv(csv(caso[0])));
            assertEquals(caso[1], e.getMessage());
        }
        verifyNoInteractions(candidatoDao);
    }
}
