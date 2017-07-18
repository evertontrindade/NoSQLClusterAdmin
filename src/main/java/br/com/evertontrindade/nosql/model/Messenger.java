package br.com.evertontrindade.nosql.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by everton on 01/08/16.
 */
@Data
@AllArgsConstructor
public class Messenger {

    private boolean clean = false;
    private Level type;
    private String content;

    public Messenger(Level type, String content) {
        this(false, type, content);
    }
}
