package br.com.evertontrindade.nosql.controller;

import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.model.ConfFile;
import br.com.evertontrindade.nosql.model.ConnectionType;
import br.com.evertontrindade.nosql.model.Node;
import br.com.evertontrindade.nosql.model.Status;
import br.com.evertontrindade.nosql.repository.ConfFileRepository;
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

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by everton on 29/05/16.
 */
@Controller
public class ConfFileController {
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private ConfFileRepository repo;
    @Autowired
    private IOHelper ioHelper;

    @PreAuthorize("hasAnyAuthority('INSERT_CONFFILE')")
    @RequestMapping("confFile/new/{id}")
    public String create(@PathVariable Long id, Model model) {
        Node node = nodeRepo.findOne(id);

        ConfFile conf = new ConfFile();
        conf.setNode(node);
        model.addAttribute("confFile", conf);
        return "configfileform";
    }

    @PreAuthorize("hasAnyAuthority('INSERT_CONFFILE')")
    @RequestMapping(value = "confFile", method = RequestMethod.POST)
    public String save(ConfFile configFile) {
        Node node = nodeRepo.findOne(configFile.getNode().getId());
        if (node.getConfFiles() == null) {
            node.setConfFiles(new ArrayList<ConfFile>());
        }
        node.getConfFiles().add(configFile);
        configFile.setNode(node);
        configFile.setStatus(Status.PENDING);
        Logger.getLogger(NodeController.class).info(node);

        nodeRepo.save(node);
        return "redirect:/confFiles/" + node.getId();
    }

    @PreAuthorize("hasAnyAuthority('LIST_CONFFILE', 'INSERT_CONFFILE')")
    @RequestMapping("/confFiles/{id}")
    public String listAll(@PathVariable Long id, Model model) {
        Node node = nodeRepo.findOne(id);
        model.addAttribute("node", node);
        return "configfileslist";
    }

    @RequestMapping("/confFiles/{id}/{saveok}/{backupok}")
    public String listAll(@PathVariable Long id, @PathVariable Boolean saveok, @PathVariable Boolean backupok, Model model) {
        Node node = nodeRepo.findOne(id);
        model.addAttribute("node", node);
        model.addAttribute("saveok", saveok);
        model.addAttribute("backupok", backupok);
        return "configfileslist";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_CONFFILE')")
    @RequestMapping("confFile/edit/{id}")
    public String edit(@PathVariable Long id, Model model) throws IOException {
        ConfFile conf = repo.findOne(id);

        Logger.getLogger(ConfFileController.class).info(conf.toString());
        model.addAttribute("confFile", conf);
        return "editconffileform";
    }

    @PreAuthorize("hasAnyAuthority('CONFIGURE_CONFFILE')")
    @RequestMapping("confFile/configure/{id}")
    public String configure(@PathVariable Long id, Model model) {
        ConfFile conf = repo.findOne(id);

        Node node = conf.getNode();
        try {
            if ("REMOTO".equals(node.getType())) {
                if (ConnectionType.SSH.equals(node.getConnectionType())) {
                    if (conf.getPath() != null && !"".equals(conf.getPath())) {
                        conf.setContentFile(ioHelper.readRemote(node, conf));
                    }
                }
            } else {
                if (conf.getPath() != null && !"".equals(conf.getPath())) {
                    conf.setContentFile(ioHelper.readLocal(node, conf));
                }
            }

        } catch (JSchException e) {
            model.addAttribute("error", true);
            Logger.getLogger(ConfFileController.class).error("[JSchException] ", e);
        } catch (IOException e) {
            model.addAttribute("error", true);
            Logger.getLogger(ConfFileController.class).error("[IOException] ", e);
        }

        Logger.getLogger(ConfFileController.class).info(conf.toString());
        model.addAttribute("confFile", conf);
        return "editconffileform";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_CONFFILE')")
    @RequestMapping(value = "confFile/edit", method = RequestMethod.POST)
    public String saveEdit(ConfFile conffile) throws IOException {

        boolean saveok = false;
        boolean backupok = false;
        Logger.getLogger(ConfFileController.class).info(conffile.toString());
        try {
            if (conffile.getContentFile() != null && !"".equals(conffile.getContentFile())) {
                Node node = conffile.getNode();
                if ("REMOTO".equals(node.getType())) {
                    if (ConnectionType.SSH.equals(node.getConnectionType())) {
                        ioHelper.writeRemote(node, conffile);
                    }
                } else {
                    ioHelper.writeLocal(node, conffile);
                }
                conffile.setStatus(Status.OK);
                backupok = true;
            }
        } catch (JSchException e) {
            Logger.getLogger(ConfFileController.class).error("[JSchException] Error connecting remote host", e);
            conffile.setStatus(Status.PENDING);
        } catch (SftpException e) {
            Logger.getLogger(ConfFileController.class).error("[SftpException] Error sending file to host", e);
            conffile.setStatus(Status.PENDING);
        } catch (Exception e) {
            Logger.getLogger(ConfFileController.class).error("[Exception]", e);
            conffile.setStatus(Status.PENDING);
        }

        Logger.getLogger(ConfFileController.class).info(conffile.toString());
        repo.save(conffile);
        saveok = true;
        return "redirect:/confFiles/" + conffile.getNode().getId() + "/" + saveok + "/" + backupok;
    }

    @PreAuthorize("hasAnyAuthority('DELETE_CONFFILE')")
    @RequestMapping("confFile/delete/{id}")
    public String delete(@PathVariable Long id) {
        ConfFile bean = repo.findOne(id);
        Long nodeId = bean.getNode().getId();
        repo.delete(id);
        return "redirect:/confFiles/" + nodeId;
    }

}
