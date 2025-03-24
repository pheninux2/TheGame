package pheninux.xdev.thecardgame.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import pheninux.xdev.thecardgame.dto.ErrorResponseDTO;
import pheninux.xdev.thecardgame.dto.GameStateDTO;
import pheninux.xdev.thecardgame.dto.PlayerActionDTO;
import pheninux.xdev.thecardgame.dto.PlayerDTO;
import pheninux.xdev.thecardgame.model.Game;
import pheninux.xdev.thecardgame.model.Player;
import pheninux.xdev.thecardgame.service.GameService;


@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Rejoint une partie
     */
    @MessageMapping("/game/{gameId}/join")
    public void joinGame(@DestinationVariable String gameId,
                         PlayerDTO player) {
        log.info("WebSocket: Demande de rejoindre la partie {} pour le joueur {}", gameId, player.getId());
        try {
            Game game = gameService.addPlayerAndGetGame(gameId, player);
            sendGameStateToAllPlayers(game);
        } catch (Exception e) {
            sendErrorToPlayer(gameId, player.getId(), e.getMessage());
        }
    }

    /**
     * Démarre une partie
     */
    @MessageMapping("/game/{gameId}/start")
    public void startGame(@DestinationVariable String gameId,
                          PlayerActionDTO action) {
        try {
            Game game = gameService.startGameAndGetGame(gameId, action.getPlayerId());
            sendGameStateToAllPlayers(game);
        } catch (Exception e) {
            sendErrorToPlayer(gameId, action.getPlayerId(), e.getMessage());
        }
    }

    /**
     * Joue une carte
     */
    @MessageMapping("/game/{gameId}/play-card")
    public void playCard(@DestinationVariable String gameId,
                         PlayerActionDTO action) {
        try {
            Game game = gameService.playCardAndGetGame(gameId, action.getPlayerId(), action.getCardIndex());
            sendGameStateToAllPlayers(game);
        } catch (Exception e) {
            sendErrorToPlayer(gameId, action.getPlayerId(), e.getMessage());
        }
    }

    /**
     * Pioche une carte
     */
    @MessageMapping("/game/{gameId}/draw-card")
    public void drawCard(@DestinationVariable String gameId,
                         PlayerActionDTO action) {
        try {
            Game game = gameService.drawCardAndGetGame(gameId, action.getPlayerId());
            sendGameStateToAllPlayers(game);
        } catch (Exception e) {
            sendErrorToPlayer(gameId, action.getPlayerId(), e.getMessage());
        }
    }

    /**
     * Choisit une couleur (pour la carte spéciale color-picker)
     */
    @MessageMapping("/game/{gameId}/choose-color")
    public void chooseColor(@DestinationVariable String gameId,
                            PlayerActionDTO action) {
        try {
            Game game = gameService.chooseColorAndGetGame(gameId, action.getPlayerId(), action.getColor());
            sendGameStateToAllPlayers(game);
        } catch (Exception e) {
            sendErrorToPlayer(gameId, action.getPlayerId(), e.getMessage());
        }
    }

    /**
     * Envoie un état de jeu personnalisé à chaque joueur
     */
    private void sendGameStateToAllPlayers(Game game) {
        for (Player player : game.getPlayers()) {
            if (player.getId() != null) {
                GameStateDTO personalState = gameService.convertToGameStateDTO(game, player.getId());
                messagingTemplate.convertAndSend("/topic/game/" + game.getId() + "/player/" + player.getId(), personalState);
            }
        }
    }

    /**
     * Envoie un message d'erreur à un joueur spécifique
     */
    private void sendErrorToPlayer(String gameId, String playerId, String errorMessage) {
        log.error("Erreur pour le joueur {} dans la partie {}: {}", playerId, gameId, errorMessage);
        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setError(true);
        errorResponse.setMessage(errorMessage);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + playerId + "/error", errorResponse);
    }
}