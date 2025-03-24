package pheninux.xdev.thecardgame.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pheninux.xdev.thecardgame.dto.GameDTO;
import pheninux.xdev.thecardgame.dto.GameStateDTO;
import pheninux.xdev.thecardgame.service.GameService;

@RestController
@RequestMapping("/api/games")
public class GameController {
    @Autowired
    private GameService gameService;

    // Créer une nouvelle partie
    @PostMapping
    public ResponseEntity<GameDTO> createGame(@RequestBody GameDTO gameDTO) {
        return ResponseEntity.ok(gameService.createGame(gameDTO));
    }

    // Obtenir les détails d'une partie
    @GetMapping("/{gameId}")
    public ResponseEntity<GameDTO> getGame(@PathVariable String gameId) {
        return ResponseEntity.ok(gameService.getGame(gameId));
    }
    @GetMapping("/{gameId}/player/{playerId}")
    public ResponseEntity<GameStateDTO> getGameStateForPlayer(
            @PathVariable String gameId,
            @PathVariable String playerId) {
        return ResponseEntity.ok(gameService.getGameStateForPlayer(gameId, playerId));
    }
}
