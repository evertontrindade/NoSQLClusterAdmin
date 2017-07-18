package br.com.evertontrindade.nosql.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.model.Cluster;
import br.com.evertontrindade.nosql.model.DataChart;
import br.com.evertontrindade.nosql.model.Node;
import br.com.evertontrindade.nosql.repository.ClusterRepository;
import br.com.evertontrindade.nosql.repository.DataChartRepository;
import br.com.evertontrindade.nosql.repository.NodeRepository;

/**
 * Created by everton on 04/09/16.
 */
@Controller
public class ChartController {

    @Autowired
    private DataChartRepository dataChartRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private IOHelper ioHelper;

    private enum ChartType {
        cpunode, cpuprocess,
        memorynode, memoryprocess,
        disknode, ioinnode, iooutnode
    }

    private enum ChartGroup {
        cluster, node;
    }

    @RequestMapping("chart/{type}/{group}/{id}")
    public @ResponseBody Map<String,List<Object>> getCpuNodeInfo(@PathVariable ChartType type,
                                                                 @PathVariable ChartGroup group,
                                                                 @PathVariable Long id) {

        switch (group) {
            case cluster:
                return getCusterInfo(id, type);
            default:
                return getNodeInfo(id, type);
        }
    }

    @PreAuthorize("hasAnyAuthority('MONITOR_CLUSTER', 'MONITOR_NODE')")
    @RequestMapping("chart/lights/{group}/{id}")
    public @ResponseBody Map<String, Boolean> getLightsInfo(@PathVariable ChartGroup group,
                                                            @PathVariable Long id) {

        List<Node> nodes = new ArrayList<>();
        Cluster cluster = null;
        switch (group) {
            case cluster:
                nodes = nodeRepository.findByClusterId(id);
                cluster = clusterRepository.findOne(id);
                break;
            default:
                Node node = nodeRepository.findOne(id);
                nodes.add(node);
                cluster = clusterRepository.findOne(node.getCluster().getId());
                break;
        }

        Map<String, Boolean> map = new HashMap<>();
        for (Node node: nodes) {
            if (cluster.getLightIndicator() != null && !"".equals(cluster.getLightIndicator())) {
                map.put(node.getName(), ioHelper.checkConnection(node));
            }
        }

        return map;
    }


    private Map<String, List<Object>> getNodeInfo(Long nodeId, ChartType type) {
        List<DataChart> list = dataChartRepository.findByNodeIdOrderByExecutionTime(nodeId);

        Map<String, List<Object>> map = new HashMap<>();
        for (DataChart data: list) {
            initMap(map, "executionTime");
            ((List)map.get("executionTime")).add(data.getExecutionTime());

            switch (type){
                case cpunode:
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getCpuNode()));
                    break;
                case cpuprocess:
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getCpuProc()));
                    break;
                case disknode:
                    initMap(map, "available");
                    ((List)map.get("available")).add(Double.valueOf(data.getDiskTotalNode()));
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getDiskUsageNode()));
                    break;
                case ioinnode:
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getIoInProc()));
                    break;
                case iooutnode:
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getIoOutProc()));
                    break;
                case memorynode:
                    initMap(map, "available");
                    ((List)map.get("available")).add(Double.valueOf(data.getMemoryTotalNode()));
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getMemoryUsageNode()));
                    break;
                case memoryprocess:
                    initMap(map, "available");
                    ((List)map.get("available")).add(Double.valueOf(data.getMemoryTotalProc()));
                    initMap(map, "usage");
                    ((List)map.get("usage")).add(Double.valueOf(data.getMemoryUsageProc()));
                    break;
            }
        }

        return map;
    }

    private Map<String, List<Object>> getCusterInfo(Long clusterId, ChartType type) {
        List<Node> nodeList = nodeRepository.findByClusterId(clusterId);
        List<DataChart> list = dataChartRepository.findByClusterIdOrderByExecutionTime(clusterId);
        Map<String, List<Object>> map = new HashMap<>();

        //definir as datas
        for (DataChart data: list) {
            initMap(map, "executionTime");
            List<Date> d = ((List) map.get("executionTime"));
            if (!d.contains(data.getExecutionTime())) {
                ((List) map.get("executionTime")).add(data.getExecutionTime());
                for (Node node : nodeList) {
                    if (type.equals(ChartType.disknode) ||
                            type.equals(ChartType.memorynode) ||
                            type.equals(ChartType.memoryprocess)) {

                        initMap(map, "[Disponivel] " + node.getName().toLowerCase());
                        ((List)map.get("[Disponivel] " + node.getName().toLowerCase())).add(Double.valueOf(0));
                        initMap(map, "[Utilizado] " + node.getName().toLowerCase());
                        ((List)map.get("[Utilizado] " + node.getName().toLowerCase())).add(Double.valueOf(0));
                    } else {
                        initMap(map, node.getName().toLowerCase());
                        ((List)map.get(node.getName().toLowerCase())).add(Double.valueOf(0));
                    }
                }
            }
        }


        //validar se valor corresponde a data
//        for (Object date : map.get("executionTime")) {
        for(int i = 0; i < map.get("executionTime").size(); i++) {
            Date date = (Date)map.get("executionTime").get(i);
            for (Node node : nodeList) {
                for (DataChart data: list) {
                    if (data.getExecutionTime().equals(date) && node.getId() == data.getNodeId()) {
                        switch (type){
                            case cpunode:
                                ((List)map.get(node.getName().toLowerCase())).set(i, Double.valueOf(data.getCpuNode()));
                                break;
                            case cpuprocess:
                                ((List)map.get(node.getName().toLowerCase())).set(i, Double.valueOf(data.getCpuProc()));
                                break;
                            case disknode:
                                ((List)map.get("[Disponivel] " + node.getName().toLowerCase())).set(i, Double.valueOf(data.getDiskTotalNode()));
                                ((List)map.get("[Utilizado] " + node.getName().toLowerCase())).set(i, Double.valueOf(data.getDiskUsageNode()));
                                break;
                            case ioinnode:
                                ((List)map.get(node.getName().toLowerCase())).set(i, Double.valueOf(data.getIoInProc()));
                                break;
                            case iooutnode:
                                ((List)map.get(node.getName().toLowerCase())).set(i, Double.valueOf(data.getIoOutProc()));
                                break;
                            case memorynode:
                                ((List)map.get("[Disponivel] " + node.getName().toLowerCase())).set(i, Double.valueOf(data.getMemoryTotalNode()));
                                ((List)map.get("[Utilizado] " + node.getName().toLowerCase())).set(i, Double.valueOf(data.getMemoryUsageNode()));
                                break;
                            case memoryprocess:
                                ((List)map.get("[Disponivel] " + node.getName().toLowerCase())).set(i, Double.valueOf(data.getMemoryTotalProc()));
                                ((List)map.get("[Utilizado] " + node.getName().toLowerCase())).set(i, Double.valueOf(data.getMemoryUsageProc()));
                                break;
                        }
                    }
                }
            }
        }

        return map;
    }

    @RequestMapping("chart/total/cluster/{id}")
    public @ResponseBody Map<String, String> getCusterTotals(@PathVariable Long id) {
        Logger.getLogger(getClass()).info("getCusterTotals: " + id);
        Map<String, String> map = new HashMap<>();

        Cluster cluster = clusterRepository.findOne(id);
        List<Node> nodeList = nodeRepository.findByClusterId(id);
        Logger.getLogger(getClass()).info("nodeList: " + nodeList.toString());

        BigDecimal totalDiskAvailable = BigDecimal.ZERO;
        BigDecimal totalDiskUsage = BigDecimal.ZERO;

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;

        BigDecimal totalCpu = BigDecimal.ZERO;

        for (Node node: nodeList) {
            Logger.getLogger(getClass()).info(node);
            Logger.getLogger(getClass()).info(dataChartRepository);
            Long dataChartId = dataChartRepository.findByNodeIdOrderByExecutionTimeDesc(node.getId());
            if (dataChartId != null) {
                DataChart data = dataChartRepository.findOne(dataChartId);
                totalDiskAvailable = totalDiskAvailable.add(new BigDecimal(data.getDiskTotalNode()));
                totalDiskUsage = totalDiskUsage.add(new BigDecimal(data.getDiskUsageNode()));

                totalIn = totalIn.add(new BigDecimal(data.getIoInProc()));
                totalOut = totalOut.add(new BigDecimal(data.getIoOutProc()));

                totalCpu = totalCpu.add(new BigDecimal(data.getCpuNode()));
            }
        }

        map.put("total-disk-available", totalDiskAvailable.setScale(2) + " GB");
        map.put("total-disk-usage", totalDiskUsage.setScale(2) + " GB");
        map.put("total-in", totalIn.setScale(2) + " " + cluster.getDataIoType().name().toLowerCase());
        map.put("total-out", totalOut.setScale(2) + " " + cluster.getDataIoType().name().toLowerCase());
        map.put("media-in", ((!nodeList.isEmpty()) ? totalIn.setScale(2).divide(BigDecimal.valueOf(nodeList.size()), RoundingMode.HALF_EVEN) : BigDecimal.ZERO) + " " + cluster.getDataIoType().name().toLowerCase());
        map.put("media-out", ((!nodeList.isEmpty()) ? totalOut.setScale(2).divide(BigDecimal.valueOf(nodeList.size()), RoundingMode.HALF_EVEN) : BigDecimal.ZERO) + " " + cluster.getDataIoType().name().toLowerCase());
        map.put("media-cpu", ((!nodeList.isEmpty()) ? totalCpu.setScale(2).divide(BigDecimal.valueOf(nodeList.size()), RoundingMode.HALF_EVEN) : BigDecimal.ZERO) + " %");

        Logger.getLogger(getClass()).info("getCusterTotals end");
        return map;
    }

    @RequestMapping("chart/total/node/{id}")
    public @ResponseBody Map<String, String> getNodeTotals(@PathVariable Long id) {
        Logger.getLogger(getClass()).info("getNodeTotals: " + id);
        Map<String, String> map = new HashMap<>();

        Node node = nodeRepository.findOne(id);
        BigDecimal totalDiskAvailable = BigDecimal.ZERO;
        BigDecimal totalDiskUsage = BigDecimal.ZERO;
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        BigDecimal totalCpu = BigDecimal.ZERO;

        Logger.getLogger(getClass()).info(node);
        Logger.getLogger(getClass()).info(dataChartRepository);

        Long dataChartId = dataChartRepository.findByNodeIdOrderByExecutionTimeDesc(node.getId());
        if (dataChartId != null) {
            DataChart data = dataChartRepository.findOne(dataChartId);
            totalDiskAvailable = new BigDecimal(data.getDiskTotalNode());
            totalDiskUsage = new BigDecimal(data.getDiskUsageNode());
        }


        List<DataChart> list = dataChartRepository.findByNodeIdOrderByExecutionTime(node.getId());
        for (DataChart item : list) {
            totalIn = totalIn.add(new BigDecimal(item.getIoInProc()));
            totalOut = totalOut.add(new BigDecimal(item.getIoOutProc()));
            totalCpu = totalCpu.add(new BigDecimal(item.getCpuNode()));
        }

        map.put("total-disk-available", totalDiskAvailable.setScale(2) + " GB");
        map.put("total-disk-usage", totalDiskUsage.setScale(2) + " GB");
        map.put("total-in", totalIn.setScale(2) + " kbps");
        map.put("total-out", totalOut.setScale(2) + " kbps");
        map.put("media-in", ((!list.isEmpty()) ? totalIn.setScale(2).divide(BigDecimal.valueOf(list.size()), RoundingMode.HALF_EVEN) : BigDecimal.ZERO) + " kbps");
        map.put("media-out", ((!list.isEmpty()) ? totalOut.setScale(2).divide(BigDecimal.valueOf(list.size()), RoundingMode.HALF_EVEN) : BigDecimal.ZERO) + " kbps");
        map.put("media-cpu", ((!list.isEmpty()) ? totalCpu.setScale(2).divide(BigDecimal.valueOf(list.size()), RoundingMode.HALF_EVEN) : BigDecimal.ZERO) + " %");

        Logger.getLogger(getClass()).info("getNodeTotals end");
        return map;
    }

    private void initMap(Map map, String key) {
        if (map.get(key) == null) {
            map.put(key, new ArrayList<>());
        }
    }

}
