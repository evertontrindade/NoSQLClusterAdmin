package br.com.evertontrindade.nosql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by everton on 04/09/16.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataChart {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long clusterId;
    private Long nodeId;
    private Date executionTime;

    private String cpuNode;
    private String memoryTotalNode;
    private String memoryUsageNode;
    private String diskTotalNode;
    private String diskUsageNode;
    private String cpuProc;
    private String memoryTotalProc;
    private String memoryUsageProc;
    private String diskTotalProc;
    private String diskUsageProc;
    private String ioInProc;
    private String ioOutProc;
}
