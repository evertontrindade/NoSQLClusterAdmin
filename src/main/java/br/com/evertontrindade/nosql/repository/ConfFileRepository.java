package br.com.evertontrindade.nosql.repository;

import br.com.evertontrindade.nosql.model.ConfFile;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by everton on 26/05/16.
 */
public interface ConfFileRepository extends CrudRepository<ConfFile, Long> {
}
