package br.com.evertontrindade.nosql.controller;

import br.com.evertontrindade.nosql.helper.IOHelper;
import br.com.evertontrindade.nosql.helper.MessageService;
import br.com.evertontrindade.nosql.model.ConnectionType;
import br.com.evertontrindade.nosql.model.ExecFile;
import br.com.evertontrindade.nosql.model.Level;
import br.com.evertontrindade.nosql.model.Node;
import br.com.evertontrindade.nosql.repository.ExecFileRepository;
import br.com.evertontrindade.nosql.repository.NodeRepository;
import com.jcraft.jsch.JSchException;
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
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by everton on 29/05/16.
 */
@Controller
public class ExecFileController {
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private ExecFileRepository repo;
    @Autowired
    private IOHelper ioHelper;
    @Autowired
    private MessageService messageService;

    @PreAuthorize("hasAnyAuthority('INSERT_EXECFILE')")
    @RequestMapping("execFile/new/{id}")
    public String create(@PathVariable Long id, Model model) {
        Node node = nodeRepo.findOne(id);

        ExecFile bean = new ExecFile();
        bean.setNode(node);
        model.addAttribute("execFile", bean);
        return "execfileform";
    }

    @PreAuthorize("hasAnyAuthority('INSERT_EXECFILE')")
    @RequestMapping(value = "execFile", method = RequestMethod.POST)
    public String save(ExecFile bean) {
        Node node = nodeRepo.findOne(bean.getNode().getId());

        if (node.getExecFiles() == null) {
            node.setExecFiles(new ArrayList<ExecFile>());
        }
        node.getExecFiles().add(bean);
        bean.setNode(node);
        Logger.getLogger(NodeController.class).info(node);

        nodeRepo.save(node);
        return "redirect:/execFiles/" + node.getId();
    }

    @PreAuthorize("hasAnyAuthority('LIST_EXECFILE', 'INSERT_EXECFILE')")
    @RequestMapping("/execFiles/{id}")
    public String listAll(@PathVariable Long id, Model model) {
        Node node = nodeRepo.findOne(id);

        Collections.sort(node.getExecFiles(), new Comparator<ExecFile>() {
            @Override
            public int compare(ExecFile o1, ExecFile o2) {
                int result = o1.getType().compareTo(o2.getType());
                if (result == 0) {
                    result = Integer.valueOf(o1.getExecutionOrder()).compareTo(o2.getExecutionOrder());
                }

                return result;
            }
        });

        model.addAttribute("node", node);
        return "execfileslist";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_EXECFILE')")
    @RequestMapping("execFile/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        ExecFile conf = repo.findOne(id);
        model.addAttribute("execFile", conf);
        return "editexecfileform";
    }

    @PreAuthorize("hasAnyAuthority('UPDATE_EXECFILE')")
    @RequestMapping(value = "execFile/edit", method = RequestMethod.POST)
    public String saveEdit(ExecFile bean) {
        repo.save(bean);
        return "redirect:/execFiles/" + bean.getNode().getId();
    }

    @PreAuthorize("hasAnyAuthority('DELETE_EXECFILE')")
    @RequestMapping("execFile/delete/{id}")
    public String delete(@PathVariable Long id) {
        ExecFile bean = repo.findOne(id);
        Long nodeId = bean.getNode().getId();
        repo.delete(id);
        return "redirect:/execFiles/" + nodeId;
    }

    @PreAuthorize("hasAnyAuthority('SERVICE_EXECFILE')")
    @RequestMapping("execFile/execute/{id}")
    public String execute(@PathVariable Long id) {
        messageService.sendMessage("/topic/exec", true, Level.INFO, "");
        ExecFile bean = repo.findOne(id);

        Logger.getLogger(NodeController.class).info(bean.toString());

        try {
            if ("REMOTO".equals(bean.getNode().getType())) {
                if (ConnectionType.SSH.equals(bean.getNode().getConnectionType())) {
                    ioHelper.executeRemote(bean.getNode(), bean, true);
                }
            } else {
                ioHelper.executeLocal(bean.getNode(), bean);
            }
        } catch (JSchException e) {
            messageService.sendMessage("/topic/exec", Level.ERROR, e.getLocalizedMessage());
            Logger.getLogger(ConfFileController.class).error("[JSchException]", e);
        } catch (IOException e) {
            messageService.sendMessage("/topic/exec", Level.ERROR, e.getLocalizedMessage());
            Logger.getLogger(ConfFileController.class).error("[IOException]", e);
        }

        return "redirect:/execFiles/" + bean.getNode().getId();
    }

}
