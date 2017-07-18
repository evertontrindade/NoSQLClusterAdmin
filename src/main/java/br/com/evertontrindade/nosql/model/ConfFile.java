package br.com.evertontrindade.nosql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Created by everton on 29/05/16.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String path;

    @ManyToOne
    private Node node;

    @Transient
    private String contentFile;

    @Enumerated(EnumType.STRING)
    private Status status;

    public ConfFile clone() {
        return new ConfFile(null, path, null, contentFile, Status.PENDING);
    }

    @Override
    public String toString() {
        return "ConfFile{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", status='" + status + '\'' +
                ", node=" + node.getId() +
                '}';
    }
}
