package br.com.evertontrindade.nosql.model;

/**
 * Created by everton on 28/05/16.
 */
public enum ConnectionType {
    SSH("22"), TELNET("23");

    private String port;

    ConnectionType(String port) {
        this.port = port;
    }

    public String getPort() {
        return this.port;
    }
}
