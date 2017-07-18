package br.com.evertontrindade.nosql.repository;

import br.com.evertontrindade.nosql.model.DataChart;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

/**
 * Created by everton on 04/09/16.
 */
public interface DataChartRepository extends CrudRepository<DataChart, Long> {

    void deleteByExecutionTimeLessThanAndClusterId(Date executionTime, Long clusterId);

    List<DataChart> findByNodeIdOrderByExecutionTime(Long nodeId);

    List<DataChart> findByClusterIdOrderByExecutionTime(Long clusterId);

    List<DataChart> findByExecutionTime(Date executionTime);

    @Query(nativeQuery = true, value = "SELECT MAX(ID) FROM DATA_CHART WHERE NODE_ID = ?1")
    Long findByNodeIdOrderByExecutionTimeDesc(Long nodeId);
}
