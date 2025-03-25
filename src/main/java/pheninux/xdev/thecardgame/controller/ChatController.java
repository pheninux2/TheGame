package pheninux.xdev.thecardgame.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import pheninux.xdev.thecardgame.model.ChatMessage;

@Controller
public class ChatController {

    @MessageMapping("/chat/{gameId}")
    @SendTo("/topic/chat/{gameId}")
    public ChatMessage sendMessage(@DestinationVariable String gameId, ChatMessage message) {
        return message;
    }
}