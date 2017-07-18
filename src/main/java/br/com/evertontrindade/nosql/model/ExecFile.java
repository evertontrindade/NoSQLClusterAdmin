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
public class ExecFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String path;
    private String parameters;
    private ExecType type;

    @ManyToOne
    private Node node;

    private int executionOrder;
    private String name;

    public ExecFile clone() {
        return new ExecFile(null, path, parameters, type, null, 0, null);
    }

    @Override
    public String toString() {
        return "ExecFile{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", parameters='" + parameters + '\'' +
                ", type=" + type +
                ", node=" + node.getId() +
                ", executionOrder=" + executionOrder +
                ", name=" + name +
                '}';
    }
}
