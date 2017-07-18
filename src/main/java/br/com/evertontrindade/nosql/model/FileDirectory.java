package br.com.evertontrindade.nosql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Created by everton on 28/06/16.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDirectory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String path;
    private String file;

    @Column(nullable = false, columnDefinition = "BIT default 0", length = 1)
    private boolean addAsConfigFile = true;

    @ManyToOne
    private Node node;

    @OneToOne(cascade = CascadeType.ALL)
    private ConfFile confFile;

    @Transient
    public FileDirectory clone() {
        return new FileDirectory(null, path, file, false, null, null);
    }

    @Override
    public String toString() {
        return "FileDirectory{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", file='" + file + '\'' +
                ", addAsConfigFile='" + addAsConfigFile + '\'' +
                '}';
    }
}
