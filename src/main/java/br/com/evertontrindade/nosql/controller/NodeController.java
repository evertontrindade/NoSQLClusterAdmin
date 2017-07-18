package br.com.evertontrindade.nosql.controller;

import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.helper.MessageService;
import br.com.evertontrindade.nosql.model.*;
import br.com.evertontrindade.nosql.repository.ClusterRepository;
import br.com.evertontrindade.nosql.repository.ExecFileRepository;
import br.com.evertontrindade.nosql.repository.NodeRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.*;
import java.util.*;

/**
 * Created by everton on 26/05/16.
 */
@Controller
public class NodeController {

    @Autowired
    private NodeRepository repo;
    @Autowired
    private ClusterRepository clusterRepo;
    @Autowired
    private ExecFileRepository execFileRepo;
    @Autowired
    private IOHelper ioHelper;
    @Autowired
    private MessageService messageService;

    @PreAuthorize("hasAnyAuthority('INSERT_NODE')")
    @RequestMapping("node/new/{id}")
    public String create(@PathVariable Long id, Model model) {
        Cluster cluster = clusterRepo.findOne(id);
        Node node = new Node();
        node.setCluster(cluster);
        model.addAttribute("node", node);
        return "nodeform";
    }

    @PreAuthorize("hasAnyAuthority('INSERT_NODE')")
    @RequestMapping(value = "node", method = RequestMethod.POST)
    public String save(Node node) {
        Cluster cluster = clusterRepo.findOne(node.getCluster().getId());
        if (cluster.getNodes() == null) {
            cluster.setNodes(new ArrayList<Node>());
        }
        cluster.getNodes().add(node);
        node.setCluster(cluster);
        Logger.getLogger(NodeController.class).info(cluster);

        clusterRepo.save(cluster);
        return "redirect:/nodes/" + cluster.getId();
    }

    @PreAuthorize("hasAnyAuthority('INSERT_NODE', 'LIST_NODE', 'VIEW_CLUSTER')")
    @RequestMapping("/nodes/{id}")
    public String listAll(@PathVariable Long id, Model model) {
        Cluster cluster = clusterRepo.findOne(id);

        Collections.sort(cluster.getNodes(), new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.valueOf(o1.getExecutionOrder()).compareTo(o2.getExecutionOrder());
            }
        });

        for (Node node: cluster.getNodes()) {
            if (cluster.isEnableMonitoring()) {
                node.setActive(ioHelper.checkConnection(node));
            } else {
                node.setActive(false);
            }
        }

        model.addAttribute("cluster", cluster);
        return "nodeslist";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_NODE')")
    @RequestMapping("node/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Node node = repo.findOne(id);
        model.addAttribute("node", node);
        return "editnodeform";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_NODE')")
    @RequestMapping(value = "node/edit", method = RequestMethod.POST)
    public String saveEdit(Node node) {
        Node oldNode = repo.findOne(node.getId());
        if (node.getRemotePasswd() == null || "".equals(node.getRemotePasswd())) {
            node.setRemotePasswd(oldNode.getRemotePasswd());
        }
        node.adjustByType();
        repo.save(node);
        return "redirect:/nodes/" + node.getCluster().getId();
    }

    @PreAuthorize("hasAnyAuthority('DELETE_NODE')")
    @RequestMapping("node/delete/{id}")
    public String delete(@PathVariable Long id) throws IOException {

        Node node = repo.findOne(id);
        try {
            node.restoreBackups(ioHelper);
        } catch (JSchException e) {
            Logger.getLogger(getClass()).error("Error when trying delete remote host", e);
        }
        Long clusterId = node.getCluster().getId();
        repo.delete(id);
        return "redirect:/nodes/" + clusterId;
    }

    @PreAuthorize("hasAnyAuthority('CLONE_NODE')")
    @RequestMapping("/node/clone/{id}")
    public String prepareClone(@PathVariable Long id, Model model) {
        Node node = repo.findOne(id);
        model.addAttribute("oldNodeId", node.getId());
        model.addAttribute("oldNodeName", node.getNameFormatted());
        node.setId(0L);
        node.setName(null);
        model.addAttribute("node", node);
        Logger.getLogger(NodeController.class).info("cloned");
        return "clonenodeform";
    }

    @PreAuthorize("hasAnyAuthority('CLONE_NODE')")
    @RequestMapping(value = "/node/clone", method = RequestMethod.POST)
    public String clone(Node node, long oldNodeId, Model model) throws IOException, SftpException, JSchException {

        node.setConfFiles(new ArrayList<>());
        node.setExecFiles(new ArrayList<>());

        Node oldNode = repo.findOne(oldNodeId);

        if (node.isCloneFiles()) {
            ioHelper.clone(node, oldNode);
        }


        for (ConfFile cf : oldNode.getConfFiles()) {
            ConfFile cloned = cf.clone();
            cloned.setNode(node);
            node.getConfFiles().add(cloned);
        }

        for (ExecFile cf : oldNode.getExecFiles()) {
            ExecFile cloned = cf.clone();
            cloned.setNode(node);
            node.getExecFiles().add(cloned);
        }

        for (FileDirectory cf : oldNode.getFileDirectories()) {
            FileDirectory cloned = cf.clone();
            cloned.setNode(node);
            node.getFileDirectories().add(cloned);
        }

        repo.save(node);

        return  "redirect:/nodes/" + node.getCluster().getId();
    }


    private void executeExecFileList(List<ExecFile> list, Node node) {
        for (ExecFile file : list) {
            Logger.getLogger(NodeController.class).info(file.toString());

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
                Logger.getLogger(NodeController.class).error("[JSchException]", e);
            } catch (IOException e) {
                messageService.sendMessage("/topic/exec", Level.ERROR, e.getLocalizedMessage());
                Logger.getLogger(NodeController.class).error("[IOException]", e);
            }
        }

    }

    @PreAuthorize("hasAnyAuthority('SERVICE_NODE')")
    @RequestMapping("node/execute/{id}/{type}")
    public String execute(@PathVariable Long id, @PathVariable ExecType type) {
        messageService.sendMessage("/topic/exec", true, Level.INFO, "");
        Node node = repo.findOne(id);
        Logger.getLogger(NodeController.class).info(type + " node...");
        Logger.getLogger(NodeController.class).info(node.toString());

        List<ExecFile> list = execFileRepo.findByTypeAndNodeOrderByExecutionOrderAsc(type, node);
        executeExecFileList(list, node);

        return "redirect:/nodes/" + node.getCluster().getId();
    }

    @RequestMapping(value = "node/validatenewip/{ip}", method = RequestMethod.GET)
    public String validateIP(@PathVariable String ip) {
        Logger.getLogger(NodeController.class).info(ip);
        String result = "fragments/ipalreadyadded :: ";
        ip = ip.replace("_", ".");
        Logger.getLogger(NodeController.class).info(ip);

        List<Node> nodesWithHost = repo.findByHost(ip);
        if (!nodesWithHost.isEmpty()) {
            result += "ipalreadyadded";
        } else {
            result += "ipok";
        }
        Logger.getLogger(NodeController.class).info(" validateip: " + result);
        return result;
    }

}
