package br.com.evertontrindade.nosql.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import br.com.evertontrindade.nosql.model.*;
import br.com.evertontrindade.nosql.repository.UserRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jcraft.jsch.JSchException;

import br.com.evertontrindade.nosql.helper.DateHelper;
import br.com.evertontrindade.nosql.helper.FileHelper;
import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.helper.MailHelper;
import br.com.evertontrindade.nosql.helper.MessageService;
import br.com.evertontrindade.nosql.repository.ClusterRepository;
import br.com.evertontrindade.nosql.repository.ExecFileRepository;

/**
 * Created by everton on 26/05/16.
 */
@Controller
public class ClusterController {

    @Autowired
    private ClusterRepository repo;
    @Autowired
    private ExecFileRepository execFileRepo;

    @Autowired
    private IOHelper ioHelper;
    @Autowired
    private MessageService messageService;
    @Autowired
    private DateHelper dateHelper;
    @Autowired
    private FileHelper fileHelper;

    @Autowired
    private MailHelper mailHelper;
    @Autowired
    private UserRepository userRepository;


    @PreAuthorize("hasAnyAuthority('INSERT_CLUSTER')")
    @RequestMapping("cluster/new")
    public String newCluster(Model model) throws Exception {
        model.addAttribute("cluster", new Cluster());
        return "clusterform";
    }

    @PreAuthorize("hasAnyAuthority('INSERT_CLUSTER')")
    @RequestMapping(value = "cluster", method = RequestMethod.POST)
    public String save(Cluster cluster) throws IOException {
        Logger.getLogger(getClass()).info("saving cluster");

        Cluster cc = cluster;

        if (cluster.getId() != null && cluster.getId() != 0) {
            cc = repo.findOne(cluster.getId());
            cc.setEnableMonitoring(cluster.isEnableMonitoring());
            cc.setExecuteCustom(cluster.isExecuteCustom());
            cc.setName(cluster.getName());
        }
        repo.save(cc);
        return "redirect:/clusters";
    }

    @PreAuthorize("hasAnyAuthority('INSERT_CLUSTER','VIEW_CLUSTER')")
    @RequestMapping("cluster/{id}")
    public String show(@PathVariable Long id, Model model) {
        model.addAttribute("cluster", repo.findOne(id));
        return "clustershow";
    }

    @PreAuthorize("hasAnyAuthority('LIST_CLUSTER', 'INSERT_CLUSTER', 'MONITOR_CLUSTER')")
    @RequestMapping("/clusters")
    public String listAll(Model model) {
        model.addAttribute("clusters", repo.findAll());
        return "clusterlist";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_CLUSTER')")
    @RequestMapping("cluster/edit/{id}")
    public String edit(@PathVariable Long id, Model model) throws IOException {
        Cluster cluster = repo.findOne(id);
        model.addAttribute("cluster", cluster);

        return "clusterform";
    }

    @PreAuthorize("hasAnyAuthority('DELETE_CLUSTER')")
    @RequestMapping("cluster/delete/{id}")
    public String delete(@PathVariable Long id) throws IOException {

        Cluster cluster = repo.findOne(id);
        try {
            cluster.restoreBackups(ioHelper);
        } catch (JSchException e) {
            Logger.getLogger(getClass()).error("Error when trying delete remote host", e);
        }

        repo.delete(id);
        return "redirect:/clusters";
    }

    @PreAuthorize("hasAnyAuthority('SERVICE_CLUSTER')")
    @RequestMapping("cluster/execute/{id}/{type}")
    public String execute(@PathVariable Long id, @PathVariable ExecType type) {
        messageService.sendMessage("/topic/exec", true, Level.INFO, "");
        Cluster cluster = repo.findOne(id);
        Logger.getLogger(ClusterController.class).info(type + " cluster " + cluster.getName());

        Collections.sort(cluster.getNodes(), new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.valueOf(o1.getExecutionOrder()).compareTo(o2.getExecutionOrder());
            }
        });

        for (Node node : cluster.getNodes()) {
            Logger.getLogger(ClusterController.class).info(node.toString());

            List<ExecFile> list = execFileRepo.findByTypeAndNodeOrderByExecutionOrderAsc(type, node);
            for (ExecFile file : list) {
                Logger.getLogger(ClusterController.class).info(file.toString());

                try {
                    if ("REMOTO".equals(node.getType())) {
                        if (ConnectionType.SSH.equals(node.getConnectionType())) {
                            ioHelper.executeRemote(node, file, true);
                        }
                    } else {
                        ioHelper.executeLocal(node, file);
                    }
                } catch (JSchException e) {
                    messageService.sendMessage("/topic/exec", Level.ERROR, e.getLocalizedMessage());
                    Logger.getLogger(ConfFileController.class).error("[JSchException]", e);
                } catch (IOException e) {
                    messageService.sendMessage("/topic/exec", Level.ERROR, e.getLocalizedMessage());
                    Logger.getLogger(ConfFileController.class).error("[IOException]", e);
                }
            }
        }

        return "redirect:/clusters";
    }



    @PreAuthorize("hasAnyAuthority('NOTIFICATION_CLUSTER')")
    @RequestMapping("cluster/notification/{id}")
    public String prepareNotificationForm(@PathVariable Long id, Model model) {
        Cluster cluster = repo.findOne(id);
        model.addAttribute("cluster", cluster);
        List<User> users = userRepository.findAll();

        if (cluster.getUsersMail() != null && !cluster.getUsersMail().isEmpty()) {
            if (users != null && !users.isEmpty()) {
                cluster.setUsersAdded(null);
                for(User added : cluster.getUsersMail()) {
                    for (User user : users) {
                        if (added.getId() == user.getId()) {
                            if (cluster.getUsersAdded() == null) {
                                cluster.setUsersAdded(user.getId() +"");
                            } else {
                                cluster.setUsersAdded(cluster.getUsersAdded() + "," + user.getId());
                            }
                        }
                    }
                    users.remove(added);
                }
            }
        }

        Logger.getLogger(getClass()).info(cluster.getUsersMail().size());

        model.addAttribute("usersAvailable", users);
        return "clusternotificationform";
    }

    @PreAuthorize("hasAnyAuthority('NOTIFICATION_CLUSTER')")
    @RequestMapping(value = "cluster/notification/", method = RequestMethod.POST)
    public String saveNotification(Cluster cluster) throws IOException {
        Cluster clusterSaved = repo.findOne(cluster.getId());
        clusterSaved.setAlertActivity(cluster.getAlertActivity());
        clusterSaved.setAlertCpu(cluster.getAlertCpu());
        clusterSaved.setAlertDisk(cluster.getAlertDisk());
        clusterSaved.setAlertMemory(cluster.getAlertMemory());
        clusterSaved.setMailTo(cluster.getMailTo());

        if (!StringUtils.isEmpty(cluster.getUsersAdded())) {
            if (clusterSaved.getUsersMail() == null) {
                clusterSaved.setUsersMail(new ArrayList<>());
            }
            clusterSaved.getUsersMail().clear();
            String[] ids = cluster.getUsersAdded().split(",");
            for(String id : ids) {
                clusterSaved.getUsersMail().add(new User(Long.valueOf(id)));
            }
        } else {
            clusterSaved.getUsersMail().clear();
        }

        repo.save(clusterSaved);
        return "redirect:/clusters";
    }

    @PreAuthorize("hasAnyAuthority('CONFMONITOR_CLUSTER')")
    @RequestMapping("cluster/monitoring/{id}")
    public String prepareMonitoringForm(@PathVariable Long id, Model model) {
        Cluster cluster = repo.findOne(id);
        model.addAttribute("cluster", cluster);
        return "clustermonitorform";
    }

    @PreAuthorize("hasAnyAuthority('CONFMONITOR_CLUSTER')")
    @RequestMapping(value = "cluster/monitoring/", method = RequestMethod.POST)
    public String saveMonitoring(Cluster cluster) throws IOException {
        Cluster clusterSaved = repo.findOne(cluster.getId());

        clusterSaved.setCollectTime(cluster.getCollectTime());
        clusterSaved.setSaveTime(cluster.getSaveTime());
        clusterSaved.setUsageCPU(cluster.getUsageCPU());
        clusterSaved.setMemoryAvailable(cluster.getMemoryAvailable());
        clusterSaved.setMemoryUsage(cluster.getMemoryUsage());
        clusterSaved.setDiskSpaceAvailable(cluster.getDiskSpaceAvailable());
        clusterSaved.setDiskSpaceUsage(cluster.getDiskSpaceUsage());
        clusterSaved.setUsageCPUProcess(cluster.getUsageCPUProcess());
        clusterSaved.setMemoryAvailableProcess(cluster.getMemoryAvailableProcess());
        clusterSaved.setMemoryUsageProcess(cluster.getMemoryUsageProcess());
        clusterSaved.setDataInputStream(cluster.getDataInputStream());
        clusterSaved.setDataOutputStream(cluster.getDataOutputStream());
        clusterSaved.setLightIndicator(cluster.getLightIndicator());
        clusterSaved.setDataIoType(cluster.getDataIoType());

        if (clusterSaved.isEnableMonitoring()) {
            Logger.getLogger(getClass()).info("enable monitoring activated...");
            StringBuilder append = new StringBuilder()
                    .append("#!/bin/bash")
                    .append("\n\n")
                    .append("## METRICAS GERAIS DA MAQUINA").append("\n")
                    .append("#").append("\n")
                    .append("# Consumo de CPU (%)").append("\n")
                    .append(clusterSaved.getUsageCPU()).append("\n")
                    .append("# Memoria RAM disponivel (MB)").append("\n")
                    .append(clusterSaved.getMemoryAvailable()).append("\n")
                    .append("# Memoria RAM utilizada (MB)").append("\n")
                    .append(clusterSaved.getMemoryUsage()).append("\n")
                    .append("# Espaço em disco disponível (GB)").append("\n")
                    .append(clusterSaved.getDiskSpaceAvailable()).append("\n")
                    .append("# Espaço em disco utilizado (GB)").append("\n")
                    .append(clusterSaved.getDiskSpaceUsage()).append("\n")

                    .append("\n\n")
                    .append("## MÉTRICAS DO(A) RESPECTIVO(A) FRAGMENTO(REPLICA) DO BANCO DE DADOS").append("\n")
                    .append("#").append("\n")
                    .append("# Consumo de CPU (%)").append("\n")
                    .append(clusterSaved.getUsageCPUProcess()).append("\n")
                    .append("# Memoria RAM disponivel (MB)").append("\n")
                    .append(clusterSaved.getMemoryAvailableProcess()).append("\n")
                    .append("# Memoria RAM utilizada (MB)").append("\n")
                    .append(clusterSaved.getMemoryUsageProcess()).append("\n")
                    .append("# Fluxo de leitura de dados (KB)").append("\n")
                    .append(clusterSaved.getDataInputStream()).append("\n")
                    .append("# Fluxo de escrita de dados (Kb)").append("\n")
                    .append(clusterSaved.getDataOutputStream()).append("\n")
                    .append("echo \"$cpunode\n$memorytotalnode\n$memoryusagenode\n$disktotalnode\n$diskusagenode\n$cpuprocess\n$memorytotalprocess\n$memoryusageprocess\n$ioinprocess\n$iooutprocess\" > /tmp/monitor.txt");

            String content = append.toString().replace("\r", "");
            clusterSaved.setPathScriptMonitor("monitor-" + clusterSaved.getId() + ".sh");
            Logger.getLogger(getClass()).info("write file " + clusterSaved.getPathScriptMonitor());
            fileHelper.writeContent(Cluster.PATH_FILE_SCRIPT_MONITORING + clusterSaved.getPathScriptMonitor(), content);
            clusterSaved.setNextExecution(dateHelper.getNextExectution(clusterSaved.getCollectTime()));
            Logger.getLogger(getClass()).info("Next execution at " + SimpleDateFormat.getDateTimeInstance().format(clusterSaved.getNextExecution()));
        } else {
            Logger.getLogger(getClass()).info("enable monitoring deactivated...");

            Logger.getLogger(getClass()).info(clusterSaved.getPathScriptMonitor());
            Logger.getLogger(getClass()).info(clusterSaved.isEnableMonitoring());
            Logger.getLogger(getClass()).info(clusterSaved.getPathScriptMonitor() != null);
            Logger.getLogger(getClass()).info(!"".equals(clusterSaved.getPathScriptMonitor()));

            if (!clusterSaved.isEnableMonitoring() &&
                    (clusterSaved.getPathScriptMonitor() != null
                            && !"".equals(clusterSaved.getPathScriptMonitor()))) {
                fileHelper.deleteFile(Cluster.PATH_FILE_SCRIPT_MONITORING + clusterSaved.getPathScriptMonitor());
            }
            clusterSaved.setCollectTime(null);
            clusterSaved.setSaveTime(null);
            clusterSaved.setPathScriptMonitor(null);
            clusterSaved.setNextExecution(null);
        }

        repo.save(clusterSaved);
        return "redirect:/clusters";

    }


    @RequestMapping("cluster/testmail/{to}")
    public String testSendMail(@PathVariable String to) {
        String result = "fragments/mailnotification :: ";

        try {
            mailHelper.send(to.replace("_", "."), "email.test.title", "email.test.message");
            result += "sent";
        } catch (Exception e) {
            e.printStackTrace();
            result += "error";
        }

        return result;
    }

}
