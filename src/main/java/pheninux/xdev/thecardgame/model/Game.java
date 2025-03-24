package pheninux.xdev.thecardgame.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Game {
    @Id
    private String id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Player> players = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_deck", joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> deck = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_center_cards", joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> centerCards = new ArrayList<>();

    private int currentPlayerIndex;
    private int turnCounter;
    private int gameDirection;
    private String selectedColor;
    private boolean gameStarted;
    private boolean gameFinished;

    // Champ manquant qui a causé l'erreur
    private String winnerId;

    // Méthodes utilitaires (optionnelles)
    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty() || currentPlayerIndex >= players.size()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public boolean isPlayerTurn(String playerId) {
        Player currentPlayer = getCurrentPlayer();
        return currentPlayer != null && currentPlayer.getId().equals(playerId);
    }
}
