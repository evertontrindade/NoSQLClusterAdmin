package br.com.evertontrindade.nosql.repository;

import br.com.evertontrindade.nosql.model.Cluster;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

/**
 * Created by everton on 26/05/16.
 */
public interface ClusterRepository extends CrudRepository<Cluster, Long> {

    List<Cluster> findByenableMonitoring(Boolean enableMonitoring);
    List<Cluster> findByenableMonitoringAndNextExecution(Boolean enableMonitoring, Date nextExecution);

}
