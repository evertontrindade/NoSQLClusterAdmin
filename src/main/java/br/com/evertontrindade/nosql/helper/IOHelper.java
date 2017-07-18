package br.com.evertontrindade.nosql.helper;

import br.com.evertontrindade.nosql.model.ConfFile;
import br.com.evertontrindade.nosql.model.ExecFile;
import br.com.evertontrindade.nosql.model.Level;
import br.com.evertontrindade.nosql.model.Node;
import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

/**
 * Created by unik on 20/06/16.
 */
@Component
public class IOHelper {

    @Autowired
    private LoggerConnector loggerConnector;
    @Autowired
    private MessageService messageService;


    public void executeRemote(Node node, ExecFile execFile, boolean sendMessage) throws JSchException, IOException {
        executeRemote(node, execFile.getPath() + " " + execFile.getParameters(), sendMessage);
    }

    public void executeRemote(Node node, ExecFile execFile) throws JSchException, IOException {
        executeRemote(node, execFile.getPath() + " " + execFile.getParameters(), false);
    }

    public void executeRemote(Node node, String command, boolean sendMessage) throws JSchException, IOException {

        Session session = connect(node, sendMessage, 100);
        Channel channel = getChannel(session, node, command);

        OutputStream out = null;
        if (node.isNeedSudo()) {
            out = channel.getOutputStream();
        }

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();

        Logger.getLogger(IOHelper.class).info("opening channel... ");
        channel.connect();
        Logger.getLogger(IOHelper.class).info("channel opened... ");

        if (node.isNeedSudo()) {
            Logger.getLogger(IOHelper.class).info("setting sudo password... ");
            out.write((node.getRemotePasswd() + "\n").getBytes());
            out.flush();
        }

        if (sendMessage) {
            printInputStream(in, Level.EXEC);
            printInputStream(err, Level.ERROR);
        }
        closeInputStream(in, err);

        if (out != null) {
            out.close();
        }
        channel.disconnect();
        Logger.getLogger(IOHelper.class).info("Channel closed...");
        session.disconnect();
        Logger.getLogger(IOHelper.class).info("Connection closed...");
    }

    public void executeLocal(Node node, ExecFile execFile) throws JSchException, IOException {
        executeLocal(node, execFile.getPath() + " " + execFile.getParameters());
    }

    public void executeLocal(Node node, String cmd) throws JSchException, IOException {
        Logger.getLogger(IOHelper.class).info("Opening process builder...");
        ProcessBuilder pb = new ProcessBuilder();

        String command = getCommand(node, cmd);

        OutputStream out = null;
        pb.command("sh", "-c", command);
        Process pr = pb.start();
        Logger.getLogger(IOHelper.class).info("Command executed locally...");

        InputStream in = pr.getInputStream();
        InputStream inError = pr.getErrorStream();

        if (node.isNeedSudo()) {
            Logger.getLogger(IOHelper.class).info("setting sudo password... ");
            out = pr.getOutputStream();
            out.write((node.getRemotePasswd() + "\n").getBytes());
            out.flush();
        }

        printInputStream(in, Level.EXEC);
        printInputStream(inError, Level.ERROR);
        closeInputStream(in, inError);

        if (out != null) {
            out.close();
        }

        Logger.getLogger(IOHelper.class).info("Command successfully executed");
    }

    public String readRemote(Node node, ConfFile file) throws JSchException, IOException {
        return readRemote(node, file.getPath());
    }

    public String readRemote(Node node, String file) throws JSchException, IOException {

        String result = null;
        Session session = connect(node, false, 100);
        Channel channel = getChannel(session, node, "cat " + file);

        OutputStream out = null;
        if (node.isNeedSudo()) {
            out = channel.getOutputStream();
        }

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();

        Logger.getLogger(IOHelper.class).info("opening channel... ");
        channel.connect();
        Logger.getLogger(IOHelper.class).info("channel opened... ");

        if (node.isNeedSudo()) {
            Logger.getLogger(IOHelper.class).info("setting sudo password... ");
            out.write((node.getRemotePasswd() + "\n").getBytes());
            out.flush();
        }

        if (in != null) {
            result = IOUtils.toString(in, "UTF-8");
            Logger.getLogger(IOHelper.class).info("Content read successfully");
        }

        if (channel.getExitStatus() > 0) {
            Logger.getLogger(IOHelper.class).info("Problems into execution...");
            String inStr = IOUtils.toString(err, "UTF-8");
            throw new IOException(inStr);
        }
        

        printInputStream(in, Level.EXEC);
        printInputStream(err, Level.ERROR);
        closeInputStream(in, err);

        if (out != null) {
            out.close();
        }
        channel.disconnect();
        Logger.getLogger(IOHelper.class).info("Channel closed...");
        session.disconnect();
        Logger.getLogger(IOHelper.class).info("Connection closed...");

        return result;
    }

    public String readLocal(Node node, ConfFile file) throws IOException {
        return readLocal(node, file.getPath());
    }

    public String readLocal(Node node, String file) throws IOException {
        Logger.getLogger(IOHelper.class).info("File to read: " + file);

        String result = IOUtils.toString(Files.newInputStream(Paths.get(file)), "UTF-8");
        Logger.getLogger(IOHelper.class).info("file read locally... ");

        return result;
    }

    public void writeRemote(Node node, ConfFile file) throws JSchException, IOException, SftpException {

        backupRemote(node, file, 0);

        String filename = "/tmp/arquivo_" + System.currentTimeMillis();
        Files.write(Paths.get(filename), file.getContentFile().getBytes());

        sendFile(node, filename, "/tmp");

        Session session = connect(node, false, 100);
        Channel channel = getChannel(session, node, "cp " + filename + " " + file.getPath());

        OutputStream out = null;
        if (node.isNeedSudo()) {
            out = channel.getOutputStream();
        }

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();

        Logger.getLogger(IOHelper.class).info("opening channel... ");
        channel.connect();
        Logger.getLogger(IOHelper.class).info("channel opened... ");

        if (node.isNeedSudo()) {
            Logger.getLogger(IOHelper.class).info("setting sudo password... ");
            out.write((node.getRemotePasswd() + "\n").getBytes());
            out.flush();
        }

        printInputStream(in, Level.EXEC);
        printInputStream(err, Level.ERROR);
        closeInputStream(in, err);

        if (out != null) {
            out.close();
        }
        channel.disconnect();
        Logger.getLogger(IOHelper.class).info("Channel closed...");
        session.disconnect();
        Logger.getLogger(IOHelper.class).info("Connection closed...");
    }

    public void writeLocal(Node node, ConfFile file) throws JSchException, IOException {

        backupLocal(node, file, 0);

        String filename = "/tmp/arquivo_" + System.currentTimeMillis();
        Files.write(Paths.get(filename), file.getContentFile().getBytes());

        Logger.getLogger(IOHelper.class).info("Opening process builder...");
        ProcessBuilder pb = new ProcessBuilder();

        String command = getCommand(node, "cp " + filename + " " + file.getPath());

        OutputStream out = null;
        pb.command("sh", "-c", command);
        Process pr = pb.start();
        Logger.getLogger(IOHelper.class).info("Command executed locally...");

        InputStream in = pr.getInputStream();
        InputStream inError = pr.getErrorStream();

        if (node.isNeedSudo()) {
            Logger.getLogger(IOHelper.class).info("setting sudo password... ");
            out = pr.getOutputStream();
            out.write((node.getRemotePasswd() + "\n").getBytes());
            out.flush();
        }

        printInputStream(in, Level.EXEC);
        printInputStream(inError, Level.ERROR);
        closeInputStream(in, inError);

        if (out != null) {
            out.close();
        }

        Logger.getLogger(IOHelper.class).info("Command successfully executed");

    }

    public void restoreRemote(Node node, ConfFile file) throws JSchException {
        String fn = file.getPath() + ".bkp.0";
        Logger.getLogger(IOHelper.class).info("Searching for " + fn);

        try {
            readRemote(node, fn);
            Logger.getLogger(IOHelper.class).info("File exists...");
            moveRemote(node, fn, file.getPath());
        } catch (IOException e) {
            Logger.getLogger(IOHelper.class).info("file " + fn + " not found... nothing to do...");
        }
    }

    public void restoreLocal(Node node, ConfFile file) throws JSchException, IOException {
        Path p = Paths.get(file.getPath() + ".bkp.0");
        Logger.getLogger(IOHelper.class).info("Searching for " + p.toFile().getAbsolutePath());
        if (Files.exists(p)) {
            Logger.getLogger(IOHelper.class).info("file " + p.toFile().getAbsolutePath() + " already exists...");
            moveLocal(node, "mv " + p.toFile().getAbsolutePath() + " " + file.getPath());
        } else {
            Logger.getLogger(IOHelper.class).info("file " + p.toFile().getAbsolutePath() + " not found... nothing to do...");
        }
    }

    public void clone(Node newNode, Node oldNode) throws IOException, JSchException, SftpException {
        Logger.getLogger(IOHelper.class).info("cloning node: " + oldNode.toString());
        for (ConfFile file : oldNode.getConfFiles()) {

            if ("LOCAL".equals(oldNode.getType())) {
                file.setContentFile(readLocal(oldNode, file));
            } else {
                file.setContentFile(readRemote(oldNode, file));
            }

            if ("LOCAL".equals(newNode.getType())) {
                writeLocal(newNode, file);
            } else {
                writeRemote(newNode, file);
            }
        }
    }

    public boolean checkConnection(Node node) {
        boolean isActive = false;

        try {

            if (node.getCluster().getLightIndicator() != null && !"".equals(node.getCluster().getLightIndicator())) {
                Session session = connect(node, false, 100);
                Channel channel = getChannel(session, node, node.getCluster().getLightIndicator());

                OutputStream out = null;
                if (node.isNeedSudo()) {
                    out = channel.getOutputStream();
                }

                InputStream in = channel.getInputStream();
                InputStream err = ((ChannelExec) channel).getErrStream();

                Logger.getLogger(IOHelper.class).info("opening channel... ");
                channel.connect();
                Logger.getLogger(IOHelper.class).info("channel opened... ");

                if (node.isNeedSudo()) {
                    Logger.getLogger(IOHelper.class).info("setting sudo password... ");
                    out.write((node.getRemotePasswd() + "\n").getBytes());
                    out.flush();
                }

                String inStr = IOUtils.toString(in, "UTF-8");
                Logger.getLogger(IOHelper.class).info(inStr);
                isActive = (inStr != null && "active".toLowerCase().equals(inStr.trim().toLowerCase()));
                Logger.getLogger(IOHelper.class).info("active: " + isActive);
                closeInputStream(in, err);

                if (out != null) {
                    out.close();
                }
                channel.disconnect();
                Logger.getLogger(IOHelper.class).info("Channel closed...");
                session.disconnect();
                Logger.getLogger(IOHelper.class).info("Connection closed...");
            }
        } catch (Exception e) {
            Logger.getLogger(IOHelper.class).error("error check connection", e);
        }

        return isActive;
    }

    private Session connect(Node node, boolean sendCommands, int timeout) throws JSchException {

        JSch jsch = new JSch();

        if (sendCommands) {
            loggerConnector.setUri("/topic/exec");
            jsch.setLogger(loggerConnector);
        } else {
            jsch.setLogger(null);
        }

        Session session = jsch.getSession(node.getRemoteUser(), node.getHost(), Integer.valueOf(node.getPort()));
        session.setPassword(node.getRemotePasswd());
        session.setConfig("StrictHostKeyChecking", "no");

        Logger.getLogger(IOHelper.class).info("Establishing Connection...");

        if (timeout > 0) {
            session.connect(timeout);
        } else {
            session.connect();
        }
        Logger.getLogger(IOHelper.class).info("Connection Established...");

        return session;
    }

    private String getCommand(Node node, String command) {
        if (node.isNeedSudo()) {
            command = "sudo -S -p '' " + command;
        }
        Logger.getLogger(IOHelper.class).info(command);

        return command;
    }

    private Channel getChannel(Session session, Node node, String command) throws JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");

        if (!command.contains("sudo")) {
            command = getCommand(node, command);
        }
        channel.setCommand(command);
        return channel;
    }

    private void printInputStream(InputStream in, Level level) throws IOException {
        if (in != null) {
            String inStr = IOUtils.toString(in, "UTF-8");

            String[] arr = inStr.split("\n");
            Level lev =  level;
            for (String str : arr) {
                if (str.toUpperCase().contains("ERROR")) {
                    lev = Level.ERROR;
                }
                this.messageService.sendMessage("/topic/exec", lev, str);
            }
        }
    }

    private void closeInputStream(InputStream... ins) throws IOException {
        for (InputStream in : ins) {
            if (in != null) {
                in.close();
            }
        }
    }

    private void backupRemote(Node node, ConfFile file, int counter) throws JSchException, IOException {
        String fn = file.getPath() + "." + "bkp" + "." + counter;
        Logger.getLogger(IOHelper.class).info("Searching for " + fn);

        try {
            readRemote(node, fn);
            Logger.getLogger(IOHelper.class).info("File exists...");
            backupRemote(node, file, ++counter);
        } catch (IOException e) {
            Logger.getLogger(IOHelper.class).info("file " + fn + " not found");
            Logger.getLogger(IOHelper.class).info("Backuping file: " + path);
            moveRemote(node, file.getPath(), fn);
        }

    }

    private void backupLocal(Node node, ConfFile file, int counter) throws IOException, JSchException {
        Path p = Paths.get(file.getPath() + ".bkp." + counter);
        Logger.getLogger(IOHelper.class).info("Searching for " + p.toFile().getAbsolutePath());
        if (Files.exists(p)) {
            Logger.getLogger(IOHelper.class).info("file " + p.toFile().getAbsolutePath() + " already exists...");
            backupLocal(node, file, ++counter);
        } else {
            Logger.getLogger(IOHelper.class).info("file " + p.toFile().getAbsolutePath() + " not exists... making backup file");
            moveLocal(node, "mv " + file.getPath() + " " + p.toFile().getAbsolutePath());
        }
    }

    private void moveRemote(Node node, String file, String newFile) throws JSchException, IOException {
        Session session = connect(node, false, 100);
        Channel channel = getChannel(session, node, "mv " + file + " " + newFile);

        OutputStream out = null;
        if (node.isNeedSudo()) {
            out = channel.getOutputStream();
        }

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();

        Logger.getLogger(IOHelper.class).info("opening channel... ");
        channel.connect();
        Logger.getLogger(IOHelper.class).info("channel opened... ");

        if (node.isNeedSudo()) {
            Logger.getLogger(IOHelper.class).info("setting sudo password... ");
            out.write((node.getRemotePasswd() + "\n").getBytes());
            out.flush();
        }

        printInputStream(in, Level.EXEC);
        printInputStream(err, Level.ERROR);
        closeInputStream(in, err);

        if (out != null) {
            out.close();
        }
        channel.disconnect();
        Logger.getLogger(IOHelper.class).info("Channel closed...");
        session.disconnect();
        Logger.getLogger(IOHelper.class).info("Connection closed...");

    }

    private void moveLocal(Node node, String cmd) throws IOException, JSchException {
        executeLocal(node, cmd);
    }

    public void sendFile(Node node, String filename, String distDir) throws JSchException, SftpException, IOException {
        Session session = connect(node, false, 100);
        ChannelSftp channel = (ChannelSftp)session.openChannel("sftp");
        channel.connect();

        Logger.getLogger(IOHelper.class).info("send file " + filename + " to " + distDir);
        channel.put(filename, distDir, ChannelSftp.OVERWRITE);
        Logger.getLogger(IOHelper.class).info("File sent...");

        channel.disconnect();
        session.disconnect();
    }
}
