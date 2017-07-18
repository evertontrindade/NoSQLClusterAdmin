package br.com.evertontrindade.nosql.batch;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import br.com.evertontrindade.nosql.helper.DateHelper;
import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.helper.MailHelper;
import br.com.evertontrindade.nosql.model.Cluster;
import br.com.evertontrindade.nosql.model.ConnectionType;
import br.com.evertontrindade.nosql.model.DataChart;
import br.com.evertontrindade.nosql.model.Node;
import br.com.evertontrindade.nosql.model.Status;
import br.com.evertontrindade.nosql.repository.ClusterRepository;
import br.com.evertontrindade.nosql.repository.DataChartRepository;

/**
 * Created by everton on 04/09/16.
 */
@Service
public class CollectDataChartBatch {

    @Autowired
    private ClusterRepository clusterRepository;
    @Autowired
    private DataChartRepository dataChartRepository;

    @Autowired
    private IOHelper ioHelper;
    @Autowired
    private DateHelper dateHelper;
    @Autowired
    private MailHelper mailHelper;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void collect() {
        Logger.getLogger(CollectDataChartBatch.class).info("start collect...");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        Logger.getLogger(CollectDataChartBatch.class).info(" > getting clusters to collect");
        List<Cluster> list = clusterRepository.findByenableMonitoringAndNextExecution(Boolean.TRUE, cal.getTime());
        Logger.getLogger(CollectDataChartBatch.class).info(" < total clusters to collect: " + list.size());

        if (list.isEmpty()) {
            Logger.getLogger(CollectDataChartBatch.class).info(" > listing cluster to update next execution time");
            list = clusterRepository.findByenableMonitoring(Boolean.TRUE);
            Logger.getLogger(CollectDataChartBatch.class).info(" < clusters listed: " + list.size());
            for (Cluster cluster : list) {
                if (cluster.getCollectTime() == null) {
                    cluster.setCollectTime(1);
                }
                cluster.setNextExecution(dateHelper.getNextExectution(cluster.getCollectTime()));
            }
            Logger.getLogger(CollectDataChartBatch.class).info(" > updating clusters");
            clusterRepository.save(list);
            Logger.getLogger(CollectDataChartBatch.class).info(" < clusters updated");

        } else {

            for (Cluster cluster : list) {
                Logger.getLogger(CollectDataChartBatch.class).info(" > start collect for cluster: " + cluster.getId());
                for (Node node : cluster.getNodes()) {
                    try {
                        Logger.getLogger(CollectDataChartBatch.class).info(" > collect info from node: " + node.getId());
                        if (Status.OK.equals(node.getStatus())) {
                            if ("REMOTO".equals(node.getType())) {
                                Logger.getLogger(CollectDataChartBatch.class).info(" > start collect from remote node...");
                                if (ConnectionType.SSH.equals(node.getConnectionType())) {
                                    ioHelper.sendFile(node, Cluster.PATH_FILE_SCRIPT_MONITORING + cluster.getPathScriptMonitor(), Node.DEFAULT_DIR);
                                    Logger.getLogger(CollectDataChartBatch.class).info(" > file sent...");
                                    ioHelper.executeRemote(node, "sh " + Node.DEFAULT_DIR + File.separator + cluster.getPathScriptMonitor(), false);
                                    Logger.getLogger(CollectDataChartBatch.class).info(" < info collected...");
                                }
                            } else {
                                Logger.getLogger(CollectDataChartBatch.class).info(" > start collect from local node...");
                                ioHelper.executeLocal(node, "sh " + Cluster.PATH_FILE_SCRIPT_MONITORING + cluster.getPathScriptMonitor());
                                Logger.getLogger(CollectDataChartBatch.class).info(" < info collected...");
                            }
                            Logger.getLogger(CollectDataChartBatch.class).info(" < finish collect from node...");

                            populate(node, cal.getTime());
                        } else {
                            Logger.getLogger(CollectDataChartBatch.class).info(" > node pending: " + node.getId());
                            cluster.setNextExecution(Calendar.getInstance().getTime());
                        }
                    } catch (JSchException e) {
                        Logger.getLogger(CollectDataChartBatch.class).error(" > error trying connect to node: " + node.getId(), e);
                    } catch (SftpException e) {
                        Logger.getLogger(CollectDataChartBatch.class).error(" > error trying send to node: " + node.getId(), e);
                    } catch (IOException e) {
                        Logger.getLogger(CollectDataChartBatch.class).error(" > error operating file on node: " + node.getId(), e);
                    }
                }
                cluster.setNextExecution(dateHelper.getNextExectution(cluster.getCollectTime()));
                Logger.getLogger(CollectDataChartBatch.class).info(" > updating next execution time to : "
                        + SimpleDateFormat.getDateTimeInstance().format(cluster.getNextExecution()));
                clusterRepository.save(cluster);
                Logger.getLogger(CollectDataChartBatch.class).info(" < cluster updated...");
            }

        }

    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void truncate() {

        List<Cluster> list = clusterRepository.findByenableMonitoring(Boolean.TRUE);
        Logger.getLogger(CollectDataChartBatch.class).info("total clusters to collect: " + list.size());

        for (Cluster cluster : list) {
            if (cluster.getSaveTime() != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.HOUR, cluster.getSaveTime() * -1);
                Logger.getLogger(CollectDataChartBatch.class).info(" > truncating data less than " + SimpleDateFormat.getDateTimeInstance().format(cal.getTime()));
                dataChartRepository.deleteByExecutionTimeLessThanAndClusterId(cal.getTime(), cluster.getId());
                Logger.getLogger(CollectDataChartBatch.class).info(" < data truncated");
            }
        }

    }

    private void populate(Node node, Date executionTime) throws IOException, JSchException {
        Logger.getLogger(CollectDataChartBatch.class).info(" > start populate data chart table");
        String content = "";

        if ("REMOTO".equals(node.getType())) {
            Logger.getLogger(CollectDataChartBatch.class).info(" > start collect data from node remote ");
            if (ConnectionType.SSH.equals(node.getConnectionType())) {
                Logger.getLogger(CollectDataChartBatch.class).info(" > retrieve content from remote file");
                content = ioHelper.readRemote(node, Node.DEFAULT_DIR + File.separator + "monitor.txt");
                Logger.getLogger(CollectDataChartBatch.class).info(" < content retrieved");
            }
        } else {
            Logger.getLogger(CollectDataChartBatch.class).info(" > retrieve content from local file");
            content = ioHelper.readLocal(node, Node.DEFAULT_DIR + File.separator + "monitor.txt");
            Logger.getLogger(CollectDataChartBatch.class).info(" < content retrieved");
        }
        Logger.getLogger(CollectDataChartBatch.class).info(" < end collect data from node remote ");

        Logger.getLogger(CollectDataChartBatch.class).info(" > parsing data to data chart");
        Properties props = new Properties();
        props.load(new StringReader(content));

        DataChart dataChart = new DataChart(null,
                node.getCluster().getId(),
                node.getId(),
                executionTime,
                getValue(props.getProperty("cpu.node")),
                getValue(props.getProperty("memory.total.node")),
                getValue(props.getProperty("memory.usage.node")),
                getValue(props.getProperty("disk.total.node")),
                getValue(props.getProperty("disk.usage.node")),
                getValue(props.getProperty("cpu.process")),
                getValue(props.getProperty("memory.total.process")),
                getValue(props.getProperty("memory.usage.process")),
                getValue(props.getProperty("disk.total.process")),
                getValue(props.getProperty("disk.usage.process")),
                getValue(props.getProperty("io.in.process")),
                getValue(props.getProperty("io.out.process"))
        );
        Logger.getLogger(CollectDataChartBatch.class).info(" < data chart parsed");

        Logger.getLogger(CollectDataChartBatch.class).info(" > saving data chart");
        dataChartRepository.save(dataChart);
        Logger.getLogger(CollectDataChartBatch.class).info(" < data chart saved");
        Logger.getLogger(CollectDataChartBatch.class).info(" < end populate data chart table");

        sendAlerts(node, dataChart);
    }

    private void sendAlerts(Node node, DataChart chart) {
        Logger.getLogger(CollectDataChartBatch.class).info(" > start send alerts");
        Cluster cluster = clusterRepository.findOne(node.getCluster().getId());

        Logger.getLogger(CollectDataChartBatch.class).info(" > check email");
        if (cluster.getMailTo() != null) {
            try {
                Logger.getLogger(CollectDataChartBatch.class).info(" > check send cpu alert");
                if (cluster.getAlertCpu() != null &&
                        BigDecimal.valueOf(cluster.getAlertCpu()).setScale(2).compareTo(new BigDecimal(chart.getCpuNode())) < 0) {

                    Logger.getLogger(CollectDataChartBatch.class).info(" > send cpu alert");
                    mailHelper.send(
                    		cluster.getMailTo(), 
                    		"email.cpu.alert.title", 
                    		"email.cpu.alert.message",
                    		chart.getCpuNode(), 
                    		cluster.getAlertCpu().toString()
                    );
                    Logger.getLogger(CollectDataChartBatch.class).info(" < cpu alert sent ");
                }
                //cpu

                Logger.getLogger(CollectDataChartBatch.class).info(" > check send disk alert");
                if (cluster.getAlertDisk() != null) {
                    BigDecimal percent = BigDecimal.ZERO;
                    if (new BigDecimal(chart.getDiskTotalNode()).compareTo(BigDecimal.ZERO) > 0) {
                        percent = new BigDecimal(chart.getDiskUsageNode())
                                .divide(new BigDecimal(chart.getDiskTotalNode()), BigDecimal.ROUND_HALF_EVEN)
                                .multiply(BigDecimal.valueOf(100)).setScale(2);
                    }

                    Logger.getLogger(CollectDataChartBatch.class).info(" > check percent disk alert");
                    if (BigDecimal.valueOf(cluster.getAlertDisk()).setScale(2).compareTo(percent) < 0) {
                        Logger.getLogger(CollectDataChartBatch.class).info(" > send disk alert");
                        mailHelper.send(
                        		cluster.getMailTo(), 
                        		"email.disk.alert.title", 
                        		"email.disk.alert.message",
                        		percent.setScale(2).toPlainString(), 
                        		cluster.getAlertDisk().toString()
                        );
                        Logger.getLogger(CollectDataChartBatch.class).info(" < disk alert sent ");
                    }
                }

                Logger.getLogger(CollectDataChartBatch.class).info(" > check send memory alert");
                if (cluster.getAlertMemory() != null) {
                    BigDecimal percent= BigDecimal.ZERO;
                    if (new BigDecimal(chart.getMemoryTotalNode()).compareTo(BigDecimal.ZERO) > 0) {
                        percent= new BigDecimal(chart.getMemoryUsageNode())
                                .divide(new BigDecimal(chart.getMemoryTotalNode()), BigDecimal.ROUND_HALF_EVEN)
                                .multiply(BigDecimal.valueOf(100)).setScale(2);
                    }

                    Logger.getLogger(CollectDataChartBatch.class).info(" > check percent memory alert");
                    if (BigDecimal.valueOf(cluster.getAlertMemory()).setScale(2).compareTo(percent) < 0) {
                        Logger.getLogger(CollectDataChartBatch.class).info(" > send memory alert");
                        mailHelper.send(
                        		cluster.getMailTo(), 
                        		"email.memory.alert.title", 
                        		"email.memory.alert.message",
                        		percent.setScale(2).toPlainString(), 
                        		cluster.getAlertMemory().toString()
                        );
                        Logger.getLogger(CollectDataChartBatch.class).info(" < memory alert sent ");
                    }
                }

                Logger.getLogger(CollectDataChartBatch.class).info(" > check send activity alert");
                if (cluster.getAlertActivity() != null &&
                        cluster.getAlertActivity() &&
                        !ioHelper.checkConnection(node)) {

                    Logger.getLogger(CollectDataChartBatch.class).info(" > send activity alert");
                    mailHelper.send(
                    		cluster.getMailTo(), 
                    		"email.activity.alert.title", 
                    		"email.activity.alert.message",
                    		node.getCluster().getName(),
                    		node.getName(),
                    		node.getType(),
                    		node.getHostFormatted(),
                    		node.getPortFormatted()
                    );
                    Logger.getLogger(CollectDataChartBatch.class).info(" < activity alert sent ");
                }

            } catch (Exception e) {
                Logger.getLogger(CollectDataChartBatch.class).error("Error sending alert", e);
            }
        }

        Logger.getLogger(CollectDataChartBatch.class).info(" < end send alerts");
    }

    private String getValue(String prop) {
        return (prop == null) ? "0.0" : prop;
    }
}
