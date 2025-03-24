package pheninux.xdev.thecardgame.dto;

import lombok.Data;

@Data
public class GameDTO {
    private String id;
    private String creatorId;
    private String creatorName;
    private int playerCount;
    private boolean gameStarted;
    private boolean gameFinished;
    private String winnerId;
    private String winnerName;
}