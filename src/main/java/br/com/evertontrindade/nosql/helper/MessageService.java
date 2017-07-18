package br.com.evertontrindade.nosql.helper;

import br.com.evertontrindade.nosql.model.Level;
import br.com.evertontrindade.nosql.model.Messenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * Created by unik on 01/08/16.
 */
@Service
public class MessageService {

    @Autowired
    public SimpMessageSendingOperations messagingTemplate;

    public void sendMessage(String uri, boolean clean, Level level, String message ) {
        messagingTemplate.convertAndSend( uri, new Messenger(clean, level, message) );
    }

    public void sendMessage(String uri, Level level, String message ) {
        messagingTemplate.convertAndSend( uri, new Messenger(level, message) );
    }

}