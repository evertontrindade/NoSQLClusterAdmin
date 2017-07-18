package br.com.evertontrindade.nosql.repository;

import br.com.evertontrindade.nosql.model.ExecFile;
import br.com.evertontrindade.nosql.model.ExecType;
import br.com.evertontrindade.nosql.model.Node;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by everton on 26/05/16.
 */
public interface ExecFileRepository extends CrudRepository<ExecFile, Long> {

    List<ExecFile> findByTypeAndNodeOrderByExecutionOrderAsc(ExecType type, Node node);
}
