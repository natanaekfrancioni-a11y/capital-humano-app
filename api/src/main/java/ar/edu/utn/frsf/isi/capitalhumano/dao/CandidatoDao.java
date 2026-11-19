package ar.edu.utn.frsf.isi.capitalhumano.dao;

import ar.edu.utn.frsf.isi.capitalhumano.model.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CandidatoDao extends JpaRepository<Candidato, Long> {
    Optional<Candidato> findByNroCandidato(String nroCandidato);
    Optional<Candidato> findByTipoDocumentoIgnoreCaseAndNroDocumento(String tipoDocumento, String nroDocumento);
}
