package br.com.evertontrindade.nosql.repository;

import br.com.evertontrindade.nosql.model.FileDirectory;
import br.com.evertontrindade.nosql.model.Node;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by everton on 26/05/16.
 */
public interface FileDirectoryRepository extends CrudRepository<FileDirectory, Long> {

    List<FileDirectory> findByNode(Node node);

}
