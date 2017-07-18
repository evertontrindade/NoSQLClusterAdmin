package br.com.evertontrindade.nosql.helper;

import br.com.evertontrindade.nosql.model.Level;
import com.jcraft.jsch.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by unik on 01/08/16.
 */
@Component
public class LoggerConnector implements Logger {

    @Autowired
    private MessageService messageService;
    private String uri;

    @Override
    public boolean isEnabled(int level) {
        return true;
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG: messageService.sendMessage(uri, Level.DEBUG, message); break;
            case INFO: messageService.sendMessage(uri,  Level.INFO, message); break;
            case WARN: messageService.sendMessage(uri,  Level.WARN, message); break;
            case ERROR: messageService.sendMessage(uri, Level.ERROR, message); break;
            case FATAL: messageService.sendMessage(uri, Level.FATAL, message); break;
        }
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
