package br.com.evertontrindade.nosql.model;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import com.jcraft.jsch.JSchException;

import br.com.evertontrindade.nosql.helper.IOHelper;
import lombok.Data;

/**
 * Created by everton on 24/05/16.
 */
@Entity
@Data
public class Cluster {

    public static final String PATH_FILE_SCRIPT_MONITORING = System.getProperty("user.home")+ File.separator;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "BIT default 0", length = 1, nullable = false)
    private boolean executeCustom = false;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cluster")
    private List<Node> nodes;

    @Column(columnDefinition = "BIT default 0", length = 1, nullable = false)
    private boolean enableMonitoring = false;

    private Integer collectTime;
    private Integer saveTime;

    private String pathScriptMonitor;
    private Date nextExecution;

    private String usageCPU;
    private String memoryAvailable;
    private String memoryUsage;
    private String diskSpaceAvailable;
    private String diskSpaceUsage;
    private String usageCPUProcess;
    private String memoryAvailableProcess;
    private String memoryUsageProcess;
    private String dataInputStream;
    private String dataOutputStream;
    private String lightIndicator;

    private String mailTo;
    @Column(columnDefinition = "BIT default 0", length = 1, nullable = false)
    private Boolean alertActivity = false;
    private Integer alertCpu;
    private Integer alertMemory;
    private Integer alertDisk;

    @OneToMany(orphanRemoval = true)
    @JoinTable(name="cluster_user", joinColumns={@JoinColumn(name="cluster_id")}, inverseJoinColumns={@JoinColumn(name="user_id")})
    private List<User> usersMail;

    @Enumerated(EnumType.STRING)
    private DataIOType dataIoType = DataIOType.KBPS;
    
    @Transient
    private String usersAdded;

    @Transient
    public void restoreBackups(IOHelper ioHelper) throws IOException, JSchException {

        for (Node node : nodes) {
            node.restoreBackups(ioHelper);
        }

    }

    @Transient
    public boolean containsExecStart() {
        for (Node node: nodes) {
            if (node.containsExecStart()) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public boolean containsExecRestart() {
        for (Node node: nodes) {
            if (node.containsExecRestart()) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public boolean containsExecStop() {
        for (Node node: nodes) {
            if (node.containsExecStop()) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public boolean containsExecCustom() {
        for (Node node: nodes) {
            if (node.containsExecCustom()) {
                return true;
            }
        }
        return false;
    }

    public String getNameFormatted() {
        return new StringBuilder("[").append(this.name.toUpperCase()).append("]").toString();
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", executeCustom=" + executeCustom +
                ", enableMonitoring=" + enableMonitoring +
                ", collectTime=" + collectTime +
                ", saveTime=" + saveTime +
                ", pathScriptMonitor='" + pathScriptMonitor + '\'' +
                ", nextExecution=" + nextExecution +
                ", nodes=" + nodes +
                '}';
    }
}
