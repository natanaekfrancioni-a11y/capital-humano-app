package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.dao.CandidatoDao;
import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CandidatoImportService {

    private static final Set<String> TIPOS_DOC = Set.of("DNI", "LE", "LC", "PP");
    private static final Set<String> GENEROS = Set.of("H", "M");

    private final CandidatoDao candidatoDao;

    public CandidatoImportService(CandidatoDao candidatoDao) {
        this.candidatoDao = candidatoDao;
    }

    @Transactional
    public Map<String, Object> importarCsv(String contenidoCsv) {
        if (contenidoCsv == null || contenidoCsv.isBlank()) {
            throw new BusinessException("El archivo CSV está vacío");
        }

        // Requerimiento especial CU006: la primera fila contiene los nombres de las columnas.
        String[] lineas = contenidoCsv.replace("﻿", "").split("\r?\n");
        List<String> lineasValidas = new ArrayList<>();
        boolean encabezadoOmitido = false;
        for (String linea : lineas) {
            String l = linea.trim();
            if (l.isEmpty()) continue;
            if (!encabezadoOmitido) {
                encabezadoOmitido = true;
                continue;
            }
            lineasValidas.add(l);
        }

        if (lineasValidas.isEmpty()) {
            throw new BusinessException("El archivo CSV no contiene registros de candidatos");
        }

        int creados = 0;
        int actualizados = 0;
        int eliminados = 0;

        for (int idx = 0; idx < lineasValidas.size(); idx++) {
            String[] cols = lineasValidas.get(idx).split(";", -1);
            if (cols.length < 11) {
                throw new BusinessException("Fila " + (idx + 1) + " inválida: se esperan 11 columnas separadas por ';'");
            }

            String nroCandidato = cols[0].trim().toUpperCase();
            String apellido = cols[1].trim().toUpperCase();
            String nombre = cols[2].trim().toUpperCase();
            String nroDoc = cols[3].trim().toUpperCase();
            String tipoDoc = cols[4].trim().toUpperCase();
            String fechaNacStr = cols[5].trim();
            String genero = cols[6].trim().toUpperCase();
            // Orden de columnas del enunciado: ...;Nacionalidad;Email;Escolaridad;Eliminado
            String nacionalidad = cols[7].trim().toUpperCase();
            String email = cols[8].trim();
            String escolaridad = cols[9].trim().toUpperCase();
            String eliminadoStr = cols[10].trim().toLowerCase();

            if (nroCandidato.isEmpty() || apellido.isEmpty() || nombre.isEmpty() || nroDoc.isEmpty()) {
                throw new BusinessException("Fila " + (idx + 1) + ": faltan datos obligatorios del candidato");
            }
            if (!TIPOS_DOC.contains(tipoDoc)) {
                throw new BusinessException("Fila " + (idx + 1) + ": tipo de documento inválido (" + tipoDoc + ")");
            }
            if (!GENEROS.contains(genero)) {
                throw new BusinessException("Fila " + (idx + 1) + ": género inválido (" + genero + ")");
            }

            LocalDate fechaNac;
            try {
                fechaNac = LocalDate.parse(fechaNacStr);
            } catch (Exception ex) {
                throw new BusinessException("Fila " + (idx + 1) + ": fecha de nacimiento inválida (" + fechaNacStr + ")");
            }

            boolean eliminado = Boolean.parseBoolean(eliminadoStr);

            Candidato existente = candidatoDao.findByNroCandidato(nroCandidato).orElse(null);
            if (existente == null) {
                Candidato nuevo = new Candidato();
                nuevo.setNroCandidato(nroCandidato);
                nuevo.setApellido(apellido);
                nuevo.setNombre(nombre);
                nuevo.setNroDocumento(nroDoc);
                nuevo.setTipoDocumento(tipoDoc);
                nuevo.setFechaNacimiento(fechaNac);
                nuevo.setGenero(genero);
                nuevo.setNacionalidad(nacionalidad);
                nuevo.setEscolaridad(escolaridad);
                nuevo.setEmail(email);
                nuevo.setEliminado(eliminado);
                nuevo.setActualizadoEn(LocalDateTime.now());
                candidatoDao.save(nuevo);
                creados++;
                if (eliminado) eliminados++;
            } else {
                boolean antesEliminado = existente.isEliminado();
                existente.setApellido(apellido);
                existente.setNombre(nombre);
                existente.setNroDocumento(nroDoc);
                existente.setTipoDocumento(tipoDoc);
                existente.setFechaNacimiento(fechaNac);
                existente.setGenero(genero);
                existente.setNacionalidad(nacionalidad);
                existente.setEscolaridad(escolaridad);
                existente.setEmail(email);
                existente.setEliminado(eliminado);
                existente.setActualizadoEn(LocalDateTime.now());
                candidatoDao.save(existente);
                actualizados++;
                if (!antesEliminado && eliminado) eliminados++;
            }
        }

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("creados", creados);
        resumen.put("actualizados", actualizados);
        resumen.put("eliminados", eliminados);
        resumen.put("totalProcesados", lineasValidas.size());
        return resumen;
    }
}
