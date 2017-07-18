package br.com.evertontrindade.nosql.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jcraft.jsch.JSchException;

import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.model.ConfFile;
import br.com.evertontrindade.nosql.model.ConnectionType;
import br.com.evertontrindade.nosql.model.FileDirectory;
import br.com.evertontrindade.nosql.model.Node;
import br.com.evertontrindade.nosql.model.Status;
import br.com.evertontrindade.nosql.repository.ConfFileRepository;
import br.com.evertontrindade.nosql.repository.FileDirectoryRepository;
import br.com.evertontrindade.nosql.repository.NodeRepository;

/**
 * Created by everton on 29/05/16.
 */
@Controller
public class FileDirectoryController {
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private ConfFileRepository confFileRepo;
    @Autowired
    private FileDirectoryRepository repo;
    @Autowired
    private IOHelper ioHelper;

    @PreAuthorize("hasAnyAuthority('INSERT_FILEDIR')")
    @RequestMapping("fileDirectory/new/{id}")
    public String create(@PathVariable Long id, Model model) {
        Node node = nodeRepo.findOne(id);

        FileDirectory bean = new FileDirectory();
        bean.setNode(node);
        model.addAttribute("fileDirectory", bean);
        return "filedirectoryform";
    }

    @PreAuthorize("hasAnyAuthority('INSERT_FILEDIR')")
    @RequestMapping(value = "fileDirectory", method = RequestMethod.POST)
    public String save(FileDirectory bean) {
        Node node = nodeRepo.findOne(bean.getNode().getId());

        if (node.getFileDirectories() == null) {
            node.setFileDirectories(new ArrayList<>());
        }
        node.getFileDirectories().add(bean);
        bean.setNode(node);
        Logger.getLogger(NodeController.class).info(node);

        try {
            createDirectory(bean.getNode(), bean);
            if (bean.isAddAsConfigFile()) {
                ConfFile cf = new ConfFile(null, bean.getPath()+ File.separator + bean.getFile(), bean.getNode(), null, Status.PENDING);
                bean.setConfFile(cf);
            }
            nodeRepo.save(node);
        } catch (JSchException e) {
            Logger.getLogger(ConfFileController.class).error("[JSchException]", e);
        } catch (IOException e) {
            Logger.getLogger(ConfFileController.class).error("[IOException]", e);
        }

        return "redirect:/fileDirectories/" + node.getId();
    }

    @PreAuthorize("hasAnyAuthority('LIST_FILEDIR', 'INSERT_FILEDIR')")
    @RequestMapping("/fileDirectories/{id}")
    public String listAll(@PathVariable Long id, Model model) {
        Node node = nodeRepo.findOne(id);
        model.addAttribute("node", node);
        return "filedirectorieslist";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_FILEDIR')")
    @RequestMapping("fileDirectory/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        FileDirectory conf = repo.findOne(id);
        model.addAttribute("fileDirectory", conf);
        return "editfiledirectoryform";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_FILEDIR')")
    @RequestMapping(value = "fileDirectory/edit", method = RequestMethod.POST)
    public String saveEdit(FileDirectory bean) {

        FileDirectory oldBean = repo.findOne(bean.getId());

        try {
            if (bean.isAddAsConfigFile()) {
                if (bean.getConfFile() != null) {
                    confFileRepo.delete(bean.getConfFile().getId());
                }
                ConfFile cf = new ConfFile(null, bean.getPath()+ File.separator + bean.getFile(), bean.getNode(), null, Status.PENDING);
                bean.setConfFile(cf);
            }
            deleteFileDirectory(oldBean.getNode(), oldBean);
            createDirectory(bean.getNode(), bean);
            repo.save(bean);
        } catch (JSchException e) {
            Logger.getLogger(ConfFileController.class).error("[JSchException]", e);
        } catch (IOException e) {
            Logger.getLogger(ConfFileController.class).error("[IOException]", e);
        }
        return "redirect:/fileDirectories/" + bean.getNode().getId();
    }

    @PreAuthorize("hasAnyAuthority('DELETE_FILEDIR')")
    @RequestMapping("fileDirectory/delete/{id}")
    public String delete(@PathVariable Long id) {
        FileDirectory bean = repo.findOne(id);
        Long nodeId = bean.getNode().getId();
        Node node = nodeRepo.findOne(nodeId);

        try {
            deleteFileDirectory(node, bean);
            repo.delete(id);
        } catch (JSchException e) {
            Logger.getLogger(ConfFileController.class).error("[JSchException]", e);
        } catch (IOException e) {
            Logger.getLogger(ConfFileController.class).error("[IOException]", e);
        }

        return "redirect:/fileDirectories/" + nodeId;
    }

    private void deleteFileDirectory(Node node, FileDirectory bean) throws IOException, JSchException {
        Logger.getLogger(ClusterController.class).info(bean.toString());

        if ("REMOTO".equals(node.getType())) {
            if (ConnectionType.SSH.equals(node.getConnectionType())) {
                ioHelper.executeRemote(node, "rm -Rf " + bean.getPath(), false);
            }
        } else {
            ioHelper.executeLocal(node, "rm -Rf " + bean.getPath());
        }

    }

    private void createDirectory(Node node, FileDirectory bean) throws IOException, JSchException {
        Logger.getLogger(ClusterController.class).info(bean.toString());

        if ("REMOTO".equals(node.getType())) {
            if (ConnectionType.SSH.equals(node.getConnectionType())) {
                ioHelper.executeRemote(node, "mkdir -p " + bean.getPath(), false);
                if (bean.getFile() != null && !"".equals(bean.getFile())) {
                    ioHelper.executeRemote(node, "touch " + bean.getPath() + File.separator + bean.getFile(), false);
                }
            }
        } else {
            ioHelper.executeLocal(node, "mkdir -p " + bean.getPath());
            if (bean.getFile() != null && !"".equals(bean.getFile())) {
                ioHelper.executeLocal(node, "touch " + bean.getPath() + File.separator + bean.getFile());
            }
        }
    }

}
