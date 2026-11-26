package ar.edu.utn.frsf.isi.capitalhumano.service.exportacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Patrón Builder: el archivo se arma paso a paso y se valida recién al construirlo. */
class ArchivoCsvTest {

    @Test
    void construir_primeraFilaConLasColumnasYUnaLineaPorFila() {
        ArchivoCsv csv = ArchivoCsv.builder()
                .nombre("resultados.csv")
                .columnas("Nro candidato", "Apellido", "Resultado")
                .fila("CAND-002", "SOSA", "5,75")
                .fila("CAND-003", "GUTIERREZ", 3)
                .construir();

        assertEquals("resultados.csv", csv.getNombre());
        assertEquals(2, csv.getCantidadFilas());
        assertEquals("Nro candidato;Apellido;Resultado\r\nCAND-002;SOSA;5,75\r\nCAND-003;GUTIERREZ;3\r\n", csv.getContenido());
    }

    @Test
    void construir_escapaSeparadorComillasYSaltosDeLineaYDejaVacioElNull() {
        ArchivoCsv csv = ArchivoCsv.builder()
                .nombre("x.csv")
                .columnas("a", "b", "c", "d")
                .fila("UNO;DOS", "DIJO \"HOLA\"", "LINEA 1\nLINEA 2", null)
                .construir();

        assertEquals("a;b;c;d\r\n\"UNO;DOS\";\"DIJO \"\"HOLA\"\"\";\"LINEA 1\nLINEA 2\";\r\n", csv.getContenido());
    }

    @Test
    void separador_sePuedeCambiar() {
        ArchivoCsv csv = ArchivoCsv.builder()
                .separador(',')
                .nombre("x.csv")
                .columnas("a", "b")
                .fila("1,5", "2;3")
                .construir();

        assertEquals("a,b\r\n\"1,5\",2;3\r\n", csv.getContenido());
    }

    @Test
    void columnas_volverAIndicarlasReemplazaLasAnteriores() {
        ArchivoCsv csv = ArchivoCsv.builder().nombre("x.csv").columnas("a", "b").columnas("c").construir();

        assertEquals("c\r\n", csv.getContenido());
        assertEquals(0, csv.getCantidadFilas());
    }

    @Test
    void fila_antesDeLasColumnas_noSePermite() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ArchivoCsv.builder().fila("a"));

        assertEquals("Primero hay que indicar las columnas del archivo", e.getMessage());
    }

    @Test
    void fila_conOtraCantidadDeValores_noSePermite() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ArchivoCsv.builder().columnas("a", "b").fila("solo uno"));

        assertEquals("La fila tiene 1 valores y el archivo 2 columnas", e.getMessage());
    }

    @Test
    void construir_sinNombreOSinColumnas_noSePermite() {
        assertEquals("El archivo necesita un nombre", assertThrows(IllegalStateException.class,
                () -> ArchivoCsv.builder().columnas("a").construir()).getMessage());
        assertEquals("El archivo necesita un nombre", assertThrows(IllegalStateException.class,
                () -> ArchivoCsv.builder().nombre("  ").columnas("a").construir()).getMessage());
        assertEquals("El archivo necesita al menos una columna", assertThrows(IllegalStateException.class,
                () -> ArchivoCsv.builder().nombre("x.csv").construir()).getMessage());
    }
}
