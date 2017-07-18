package br.com.evertontrindade.nosql.controller;

import br.com.evertontrindade.nosql.model.Level;
import br.com.evertontrindade.nosql.model.Messenger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Created by everton on 01/08/16.
 */
@MessageMapping("/connect-socket")
@Controller
public class WebSocketController {

    @SendTo("/topic/exec")
    public Messenger sendMessage(String message) {
        return new Messenger(Level.INFO, message);
    }

}
