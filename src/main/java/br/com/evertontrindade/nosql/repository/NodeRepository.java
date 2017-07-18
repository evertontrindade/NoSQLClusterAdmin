package br.com.evertontrindade.nosql.repository;

import br.com.evertontrindade.nosql.model.Cluster;
import br.com.evertontrindade.nosql.model.Node;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by everton on 26/05/16.
 */
public interface NodeRepository extends CrudRepository<Node, Long> {

    List<Node> findByClusterOrderByExecutionOrderAsc(Cluster cluster);
    List<Node> findByClusterId(Long clusterId);
    List<Node> findByHost(String host);

}
