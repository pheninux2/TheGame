package pheninux.xdev.thecardgame.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String senderId;
    private String senderName;
    private String content;
    private String timestamp;
    private boolean isSystem;
}
