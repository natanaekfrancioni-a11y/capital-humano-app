package ar.edu.utn.frsf.isi.capitalhumano.service;

import ar.edu.utn.frsf.isi.capitalhumano.exception.BusinessException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValidadorConsultor {

    private final JdbcTemplate jdbcTemplate;

    public ValidadorConsultor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Devuelve el username registrado del consultor activo. */
    public String obtenerUsernameActivo(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new BusinessException("Debe indicar el usuario consultor");
        }
        List<String> encontrados = jdbcTemplate.queryForList(
                "SELECT username FROM usuario_consultor WHERE UPPER(username) = UPPER(?) AND activo = TRUE",
                String.class, usuario.trim());
        if (encontrados.isEmpty()) {
            throw new BusinessException("El usuario no es un consultor activo");
        }
        return encontrados.get(0);
    }
}
