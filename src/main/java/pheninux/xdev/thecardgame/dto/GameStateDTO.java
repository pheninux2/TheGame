package pheninux.xdev.thecardgame.dto;

import lombok.Data;
import pheninux.xdev.thecardgame.model.Card;

import java.util.List;

@Data
public class GameStateDTO {
    private String gameId;
    private int turnCounter;
    private int currentPlayerIndex;
    private int gameDirection;
    private String selectedColor;
    private boolean gameStarted;
    private boolean gameFinished;
    private String winnerId;
    private List<PlayerDTO> players;
    private List<Card> centerCards;
    private int deckSize;
}
