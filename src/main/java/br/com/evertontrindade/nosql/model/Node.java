package br.com.evertontrindade.nosql.model;

import br.com.evertontrindade.nosql.helper.IOHelper;
import com.jcraft.jsch.JSchException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.log4j.Logger;

import javax.persistence.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by everton on 28/05/16.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    public static final String DEFAULT_DIR = "/tmp";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String host;
    private String type;
    private String port;
    @Enumerated(EnumType.STRING)
    private ConnectionType connectionType;
    private String remoteUser;
    private String remotePasswd;

    @ManyToOne
    private Cluster cluster;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "node", fetch = FetchType.LAZY)
    private List<ConfFile> confFiles;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "node", fetch = FetchType.LAZY)
    private List<ExecFile> execFiles;

    @Column(nullable = false, columnDefinition = "BIT default 0", length = 1)
    private boolean needSudo;

    private int executionOrder;

    @Transient
    private boolean cloneFiles;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "node", fetch = FetchType.LAZY)
    private List<FileDirectory> fileDirectories;

    @Transient
    private Boolean active;

    public Node(String name, String host, String type,
                String port, ConnectionType connectionType,
                String remoteUser, String remotePasswd, Cluster cluster, boolean needSudo, int executionOrder) {

        this(null, name, host, type,
                port, connectionType, remoteUser,
                remotePasswd, cluster, new ArrayList<>(), new ArrayList<>(), needSudo, executionOrder, false, new ArrayList<>(), null);

    }

    @Transient
    public void adjustByType() {
        Logger.getLogger(Node.class).info("type selected: " + type);

        if ("LOCAL".equals(type)) {
            Logger.getLogger(Node.class).info("cleaning data...");
            this.host = null;
            this.port = null;
            this.connectionType = null;
        }
    }

    @Transient
    public void restoreBackups(IOHelper ioHelper) throws IOException, JSchException {
        for (ConfFile confFile : confFiles) {
            if ("LOCAL".equals(this.type)) {
                ioHelper.restoreLocal(this, confFile);
            } else {
                ioHelper.restoreRemote(this, confFile);
            }

        }

    }

    @Transient
    public Node clone() {
        Node node = new Node(null, host, type, port,
                connectionType, remoteUser, remotePasswd, cluster, needSudo, getExecutionOrder() + 1);

        for (ConfFile cf : this.confFiles) {
            ConfFile cloned = cf.clone();
            cloned.setNode(node);
            node.getConfFiles().add(cloned);
        }

        for (ExecFile cf : this.execFiles) {
            ExecFile cloned = cf.clone();
            cloned.setNode(node);
            node.getExecFiles().add(cloned);
        }

        for (FileDirectory cf : this.fileDirectories) {
            FileDirectory cloned = cf.clone();
            cloned.setNode(node);
            node.getFileDirectories().add(cloned);
        }

        node.adjustByType();

        return node;
    }

    @Transient
    public String getNameFormatted() {
        StringBuilder result = new StringBuilder("[")
                .append(this.cluster.getName())
                .append(": ")
                .append(this.name);

        if ("LOCAL".equals(this.type)) {
            result.append(" : ").append(this.type);
        } else {
            result.append(" : ").append(this.host.toUpperCase());
        }

        return result.append("]").toString().toUpperCase();
    }

    @Transient
    public Status getStatus() {
        Status status = Status.OK;

        if (this.confFiles.isEmpty()) {
            status = Status.PENDING;
        } else {
            for (ConfFile item : this.confFiles) {
                if (Status.PENDING.equals(item.getStatus())) {
                    status = Status.PENDING;
                    break;
                }
            }
        }


        return status;
    }

    @Transient
    public String getHostFormatted() throws UnknownHostException {

        if ("LOCAL".equals(type)) {
            return InetAddress.getLocalHost().getHostAddress();
        } else {
            return host;
        }
    }

    @Transient
    public String getConnectionTypeFormatted() throws UnknownHostException {

        if ("LOCAL".equals(type)) {
            return "-";
        } else {
            return connectionType.name();
        }
    }

    @Transient
    public String getPortFormatted() throws UnknownHostException {

        if ("LOCAL".equals(type)) {
            return "-";
        } else {
            return port;
        }
    }

    @Transient
    public boolean containsExecStart() {
        return containsExectype(ExecType.START);
    }

    @Transient
    public boolean containsExecRestart() {
        return containsExectype(ExecType.RESTART);
    }

    @Transient
    public boolean containsExecStop() {
        return containsExectype(ExecType.STOP);
    }

    @Transient
    public boolean containsExecCustom() {
        return containsExectype(ExecType.CUSTOM);
    }


    @Transient
    private boolean containsExectype(ExecType type) {
        for (ExecFile bean: execFiles) {
            if (bean.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", type='" + type + '\'' +
                ", port='" + port + '\'' +
                ", connectionType=" + connectionType +
                ", remoteUser='" + remoteUser + '\'' +
                ", remotePasswd='" + remotePasswd + '\'' +
                ", cluster=" + cluster.getId() +
                ", confFiles=" + confFiles +
                ", execFiles=" + execFiles +
                ", needSudo=" + needSudo +
                ", executionOrder=" + executionOrder +
                ", fileDirectories=" + fileDirectories +
                '}';
    }
}
