package ar.edu.utn.frsf.isi.capitalhumano.service.exportacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Archivo CSV ya armado: nombre, contenido y cantidad de filas de datos. Es inmutable.
 *
 * Patrón Builder: el archivo se arma paso a paso con {@link Builder} (nombre, columnas, una fila
 * por vez) y recién al final {@link Builder#construir()} valida los datos y genera el texto. Así quien
 * exporta solo dice qué datos van, y el formato (separador, escapes, fin de línea) queda en un
 * único lugar.
 */
public final class ArchivoCsv {

    /** Fin de línea estándar de CSV (RFC 4180); Excel lo abre sin problemas. */
    static final String FIN_DE_LINEA = "\r\n";

    private final String nombre;
    private final String contenido;
    private final int cantidadFilas;

    private ArchivoCsv(String nombre, String contenido, int cantidadFilas) {
        this.nombre = nombre;
        this.contenido = contenido;
        this.cantidadFilas = cantidadFilas;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNombre() {
        return nombre;
    }

    public String getContenido() {
        return contenido;
    }

    /** Filas de datos, sin contar la de los nombres de las columnas. */
    public int getCantidadFilas() {
        return cantidadFilas;
    }

    public static final class Builder {

        private String nombre;
        private char separador = ';';
        private final List<String> columnas = new ArrayList<>();
        private final List<List<String>> filas = new ArrayList<>();

        private Builder() {
        }

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        /** Por defecto punto y coma, como pide el enunciado para los archivos del sistema. */
        public Builder separador(char separador) {
            this.separador = separador;
            return this;
        }

        /** La primera fila del archivo: los nombres de las columnas. */
        public Builder columnas(String... nombres) {
            columnas.clear();
            columnas.addAll(Arrays.asList(nombres));
            return this;
        }

        /** Agrega una fila de datos. Debe tener tantos valores como columnas; null se escribe vacío. */
        public Builder fila(Object... valores) {
            if (columnas.isEmpty()) {
                throw new IllegalStateException("Primero hay que indicar las columnas del archivo");
            }
            if (valores.length != columnas.size()) {
                throw new IllegalArgumentException("La fila tiene " + valores.length + " valores y el archivo "
                        + columnas.size() + " columnas");
            }
            filas.add(Arrays.stream(valores).map(v -> v == null ? "" : v.toString()).toList());
            return this;
        }

        public ArchivoCsv construir() {
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalStateException("El archivo necesita un nombre");
            }
            if (columnas.isEmpty()) {
                throw new IllegalStateException("El archivo necesita al menos una columna");
            }
            StringBuilder texto = new StringBuilder();
            agregarLinea(texto, columnas);
            filas.forEach(fila -> agregarLinea(texto, fila));
            return new ArchivoCsv(nombre, texto.toString(), filas.size());
        }

        private void agregarLinea(StringBuilder texto, List<String> valores) {
            for (int i = 0; i < valores.size(); i++) {
                if (i > 0) {
                    texto.append(separador);
                }
                texto.append(escapar(valores.get(i)));
            }
            texto.append(FIN_DE_LINEA);
        }

        /** Un valor con separador, comillas o saltos de línea va entre comillas, con las comillas duplicadas. */
        private String escapar(String valor) {
            boolean requiereComillas = valor.indexOf(separador) >= 0 || valor.contains("\"")
                    || valor.contains("\n") || valor.contains("\r");
            return requiereComillas ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
        }
    }
}
