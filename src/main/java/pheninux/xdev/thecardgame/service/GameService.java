package pheninux.xdev.thecardgame.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pheninux.xdev.thecardgame.dto.GameDTO;
import pheninux.xdev.thecardgame.dto.GameStateDTO;
import pheninux.xdev.thecardgame.dto.PlayerDTO;
import pheninux.xdev.thecardgame.model.Card;
import pheninux.xdev.thecardgame.model.Game;
import pheninux.xdev.thecardgame.model.Player;
import pheninux.xdev.thecardgame.repository.GameRepository;
import pheninux.xdev.thecardgame.repository.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    // Constantes
    private static final String[] COLORS = {"cardRed", "cardBlue", "cardGreen", "cardYellow"};
    private static final String[] SYMBOLS = {"★", "✦", "◉", "⬠", "△", "▢", "◇", "○"};
    private static final Map<String, String> SPECIAL_CARDS = Map.of(
            "★", "draw-two",
            "✦", "skip",
            "◉", "reverse",
            "⬠", "color-picker"
    );

    /**
     * Crée une nouvelle partie
     */
    @Transactional
    public GameDTO createGame(GameDTO gameDTO) {
        // Générer un ID unique pour la partie si non fourni
        String gameId = gameDTO.getId() != null ?
                gameDTO.getId() : UUID.randomUUID().toString().substring(0, 8);

        // Créer une nouvelle partie
        Game game = new Game();
        game.setId(gameId);
        game.setPlayers(new ArrayList<>());
        game.setDeck(new ArrayList<>());
        game.setCenterCards(new ArrayList<>());
        game.setCurrentPlayerIndex(0);
        game.setTurnCounter(1);
        game.setGameDirection(1);
        game.setGameStarted(false);
        game.setGameFinished(false);

        // Ajouter le créateur comme premier joueur si fourni
        if (gameDTO.getCreatorId() != null && gameDTO.getCreatorName() != null) {
            Player creator = new Player();
            creator.setId(gameDTO.getCreatorId());
            creator.setName(gameDTO.getCreatorName());
            creator.setCreator(true);
            creator.setCards(new ArrayList<>());

            game.getPlayers().add(creator);
            playerRepository.save(creator);
        }

        // Sauvegarder et retourner la partie
        gameRepository.save(game);

        return convertToDTO(game);
    }

    /**
     * Récupère les détails d'une partie
     */
    @Transactional(readOnly = true)
    public GameDTO getGame(String gameId) {
        Game game = findGameById(gameId);
        return convertToDTO(game);
    }

    /**
     * Récupère l'état du jeu pour un joueur spécifique
     */
    @Transactional(readOnly = true)
    public GameStateDTO getGameStateForPlayer(String gameId, String playerId) {
        Game game = findGameById(gameId);
        return convertToGameStateDTO(game, playerId);
    }

    /**
     * Ajoute un joueur à une partie
     */
    @Transactional
    public GameStateDTO addPlayer(String gameId, PlayerDTO playerDTO) {
        Game game = addPlayerAndGetGame(gameId, playerDTO);
        return convertToGameStateDTO(game, playerDTO.getId());
    }

    /**
     * Ajoute un joueur à une partie et retourne l'objet Game
     */
    @Transactional
    public Game addPlayerAndGetGame(String gameId, PlayerDTO playerDTO) {
        Game game = findGameById(gameId);

        log.info("Ajout du joueur: {} avec ID: {}", playerDTO.getName(), playerDTO.getId());

        // Vérifier si la partie est déjà commencée
        if (game.isGameStarted()) {
            throw new IllegalStateException("La partie a déjà commencé");
        }

        // Vérifier si le joueur existe déjà
        Optional<Player> existingPlayer = game.getPlayers().stream()
                .filter(p -> p.getId() != null && p.getId().equals(playerDTO.getId()))
                .findFirst();

        if (existingPlayer.isPresent()) {
            log.info("Le joueur est déjà dans la partie: {}", playerDTO.getId());
            return game;
        }

        // Vérifier si la partie n'est pas déjà pleine (max 4 joueurs)
        if (game.getPlayers().size() >= 4) {
            throw new IllegalStateException("La partie est déjà pleine (4 joueurs maximum)");
        }

        // Créer et ajouter le nouveau joueur
        Player newPlayer = new Player();
        newPlayer.setId(playerDTO.getId());
        newPlayer.setName(playerDTO.getName());
        newPlayer.setCreator(false);
        newPlayer.setCards(new ArrayList<>());

        game.getPlayers().add(newPlayer);
        playerRepository.save(newPlayer);
        gameRepository.save(game);

        return game;
    }

    /**
     * Démarre une partie
     */
    @Transactional
    public GameStateDTO startGame(String gameId, String playerId) {
        Game game = startGameAndGetGame(gameId, playerId);
        return convertToGameStateDTO(game, playerId);
    }

    /**
     * Démarre une partie et retourne l'objet Game
     */
    @Transactional
    public Game startGameAndGetGame(String gameId, String playerId) {
        Game game = findGameById(gameId);

        // Vérifier si le joueur est le créateur
        boolean isCreator = game.getPlayers().stream()
                .anyMatch(p -> p.getId() != null && p.getId().equals(playerId) && p.isCreator());

        if (!isCreator) {
            throw new IllegalStateException("Seul le créateur peut démarrer la partie");
        }

        // Vérifier s'il y a au moins 2 joueurs
        if (game.getPlayers().size() < 2) {
            throw new IllegalStateException("Il faut au moins 2 joueurs pour démarrer la partie");
        }

        // Initialiser le jeu
        initializeGame(game);
        game.setGameStarted(true);

        // Débogage - Vérifier que chaque joueur a bien reçu des cartes
        for (Player player : game.getPlayers()) {
            log.info("Joueur {} a {} cartes après distribution", player.getName(), player.getCards().size());
        }

        gameRepository.save(game);

        return game;
    }

    /**
     * Joue une carte
     */
    @Transactional
    public GameStateDTO playCard(String gameId, String playerId, int cardIndex) {
        Game game = playCardAndGetGame(gameId, playerId, cardIndex);
        return convertToGameStateDTO(game, playerId);
    }

    /**
     * Joue une carte et retourne l'objet Game
     */
    @Transactional
    public Game playCardAndGetGame(String gameId, String playerId, int cardIndex) {
        Game game = findGameById(gameId);

        // Vérifier si c'est le tour du joueur
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        if (currentPlayer.getId() == null || !currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Ce n'est pas votre tour");
        }

        // Vérifier si l'index de carte est valide
        if (cardIndex < 0 || cardIndex >= currentPlayer.getCards().size()) {
            throw new IllegalArgumentException("Index de carte invalide");
        }

        // Récupérer la carte à jouer
        Card cardToPlay = currentPlayer.getCards().get(cardIndex);

        // Vérifier si la carte peut être jouée
        if (!canCardBePlayed(game, cardToPlay)) {
            throw new IllegalStateException("Cette carte ne peut pas être jouée");
        }

        // Jouer la carte
        currentPlayer.getCards().remove(cardIndex);

        // Ajouter la carte au centre
        game.getCenterCards().add(cardToPlay);
        if (game.getCenterCards().size() > 4) {
            game.getCenterCards().remove(0);
        }

        // Vérifier si le joueur a gagné
        if (currentPlayer.getCards().isEmpty()) {
            game.setGameFinished(true);
            game.setWinnerId(currentPlayer.getId());
            gameRepository.save(game);
            return game;
        }

        // Gérer les cartes spéciales
        handleSpecialCard(game, cardToPlay);

        // Passer au joueur suivant
        if (!game.isGameFinished()) {
            nextPlayer(game);
        }

        gameRepository.save(game);
        return game;
    }

    /**
     * Pioche une carte
     */
    @Transactional
    public GameStateDTO drawCard(String gameId, String playerId) {
        Game game = drawCardAndGetGame(gameId, playerId);
        return convertToGameStateDTO(game, playerId);
    }

    /**
     * Pioche une carte et retourne l'objet Game
     */
    @Transactional
    public Game drawCardAndGetGame(String gameId, String playerId) {
        Game game = findGameById(gameId);

        // Vérifier si c'est le tour du joueur
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        if (currentPlayer.getId() == null || !currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Ce n'est pas votre tour");
        }

        // Vérifier s'il reste des cartes dans le paquet
        if (game.getDeck().isEmpty()) {
            // Reconstituer le paquet avec les cartes du centre
            if (game.getCenterCards().size() > 1) {
                Card lastCard = game.getCenterCards().get(game.getCenterCards().size() - 1);
                List<Card> newDeck = new ArrayList<>(game.getCenterCards().subList(0, game.getCenterCards().size() - 1));
                game.setCenterCards(new ArrayList<>(Collections.singletonList(lastCard)));
                game.setDeck(newDeck);
                shuffleDeck(game);
            } else {
                throw new IllegalStateException("La pioche est vide");
            }
        }

        // Piocher une carte
        Card drawnCard = game.getDeck().remove(0);
        currentPlayer.getCards().add(drawnCard);

        // Vérifier si la carte piochée peut être jouée
        boolean canPlay = canCardBePlayed(game, drawnCard);

        // Si la carte ne peut pas être jouée, passer au joueur suivant
        if (!canPlay) {
            nextPlayer(game);
        }

        gameRepository.save(game);
        return game;
    }

    /**
     * Choix de couleur (pour la carte spéciale "color-picker")
     */
    @Transactional
    public GameStateDTO chooseColor(String gameId, String playerId, String color) {
        Game game = chooseColorAndGetGame(gameId, playerId, color);
        return convertToGameStateDTO(game, playerId);
    }

    /**
     * Choix de couleur et retourne l'objet Game
     */
    @Transactional
    public Game chooseColorAndGetGame(String gameId, String playerId, String color) {
        Game game = findGameById(gameId);

        // Vérifier si c'est le tour du joueur
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        if (currentPlayer.getId() == null || !currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Ce n'est pas votre tour");
        }

        // Vérifier si une couleur valide a été choisie
        if (!Arrays.asList(COLORS).contains(color)) {
            throw new IllegalArgumentException("Couleur invalide");
        }

        // Définir la couleur sélectionnée
        game.setSelectedColor(color);

        // Passer au joueur suivant
        nextPlayer(game);

        gameRepository.save(game);
        return game;
    }

    // ===== MÉTHODES UTILITAIRES PRIVÉES =====

    /**
     * Trouve une partie par son ID
     */
    private Game findGameById(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Partie non trouvée: " + gameId));
    }

    /**
     * Initialise le jeu (cartes, distribution, etc.)
     */
    private void initializeGame(Game game) {
        // Créer le jeu de cartes
        List<Card> deck = createDeck();

        // Mélanger le jeu
        Collections.shuffle(deck);
        game.setDeck(deck);

        log.info("Deck créé avec {} cartes", deck.size());

        // Distribuer les cartes aux joueurs
        for (Player player : game.getPlayers()) {
            // S'assurer que la liste de cartes est vide avant de commencer
            player.setCards(new ArrayList<>());

            for (int i = 0; i < 5; i++) {
                if (!game.getDeck().isEmpty()) {
                    Card card = game.getDeck().remove(0);
                    player.getCards().add(card);
                    log.info("Carte {} de couleur {} distribuée à {}",
                            card.getSymbol(), card.getColor(), player.getName());
                }
            }

            log.info("Joueur {} a reçu {} cartes", player.getName(), player.getCards().size());
        }

        // Placer les cartes centrales
        List<Card> centerCards = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!game.getDeck().isEmpty()) {
                centerCards.add(game.getDeck().remove(0));
            }
        }
        game.setCenterCards(centerCards);

        // Initialiser les autres paramètres
        game.setCurrentPlayerIndex(0);
        game.setTurnCounter(1);
        game.setGameDirection(1);
        game.setSelectedColor(null);
    }

    /**
     * Crée un jeu de cartes complet
     */
    private List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();

        // Création de deux ensembles de cartes pour chaque combinaison couleur/symbole
        for (int i = 0; i < 2; i++) {
            for (String color : COLORS) {
                for (String symbol : SYMBOLS) {
                    Card card = new Card();
                    card.setColor(color);
                    card.setSymbol(symbol);
                    deck.add(card);
                }
            }
        }

        return deck;
    }

    /**
     * Mélange le jeu de cartes
     */
    private void shuffleDeck(Game game) {
        Collections.shuffle(game.getDeck());
    }

    /**
     * Vérifie si une carte peut être jouée
     */
    private boolean canCardBePlayed(Game game, Card card) {
        // Si une couleur a été sélectionnée (suite à une carte "choisir couleur")
        if (game.getSelectedColor() != null) {
            boolean canPlay = card.getColor().equals(game.getSelectedColor());
            if (canPlay) {
                game.setSelectedColor(null);
            }
            return canPlay;
        }

        // Vérification des correspondances avec les cartes du centre
        for (Card centerCard : game.getCenterCards()) {
            if (card.getColor().equals(centerCard.getColor()) ||
                    card.getSymbol().equals(centerCard.getSymbol())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gère les effets des cartes spéciales
     */
    private void handleSpecialCard(Game game, Card card) {
        if (SPECIAL_CARDS.containsKey(card.getSymbol())) {
            String effect = SPECIAL_CARDS.get(card.getSymbol());

            switch (effect) {
                case "draw-two":
                    // Le joueur suivant pioche 2 cartes
                    int nextPlayerIndex = getNextPlayerIndex(game);
                    Player nextPlayer = game.getPlayers().get(nextPlayerIndex);
                    for (int i = 0; i < 2; i++) {
                        if (!game.getDeck().isEmpty()) {
                            nextPlayer.getCards().add(game.getDeck().remove(0));
                        }
                    }
                    break;

                case "skip":
                    // Le joueur suivant est sauté
                    game.setCurrentPlayerIndex(getNextPlayerIndex(game));
                    break;

                case "reverse":
                    // Inversion du sens du jeu
                    game.setGameDirection(game.getGameDirection() * -1);
                    break;

                case "color-picker":
                    // La couleur sera choisie via un appel séparé à chooseColor()
                    // Ne pas changer de joueur pour l'instant
                    return;
            }
        }
    }

    /**
     * Obtient l'index du joueur suivant
     */
    private int getNextPlayerIndex(Game game) {
        return (game.getCurrentPlayerIndex() + game.getGameDirection() + game.getPlayers().size())
                % game.getPlayers().size();
    }

    /**
     * Passe au joueur suivant
     */
    private void nextPlayer(Game game) {
        game.setCurrentPlayerIndex(getNextPlayerIndex(game));
        game.setTurnCounter(game.getTurnCounter() + 1);
    }

    /**
     * Convertit une entité Game en DTO
     */
    public GameDTO convertToDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setPlayerCount(game.getPlayers().size());
        dto.setGameStarted(game.isGameStarted());
        dto.setGameFinished(game.isGameFinished());

        // Trouver le créateur
        Optional<Player> creator = game.getPlayers().stream()
                .filter(Player::isCreator)
                .findFirst();

        if (creator.isPresent()) {
            dto.setCreatorId(creator.get().getId());
            dto.setCreatorName(creator.get().getName());
        }

        if (game.isGameFinished() && game.getWinnerId() != null) {
            dto.setWinnerId(game.getWinnerId());
            game.getPlayers().stream()
                    .filter(p -> p.getId() != null && p.getId().equals(game.getWinnerId()))
                    .findFirst()
                    .ifPresent(winner -> dto.setWinnerName(winner.getName()));
        }

        return dto;
    }

    /**
     * Convertit une entité Game en GameStateDTO
     */
    public GameStateDTO convertToGameStateDTO(Game game, String currentPlayerId) {
        log.info("Conversion de l'état du jeu pour le joueur: {}", currentPlayerId);

        GameStateDTO dto = new GameStateDTO();
        dto.setGameId(game.getId());
        dto.setTurnCounter(game.getTurnCounter());
        dto.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
        dto.setGameDirection(game.getGameDirection());
        dto.setSelectedColor(game.getSelectedColor());
        dto.setGameStarted(game.isGameStarted());
        dto.setGameFinished(game.isGameFinished());
        dto.setWinnerId(game.getWinnerId());

        // Convertir les joueurs
        dto.setPlayers(game.getPlayers().stream()
                .map(player -> {
                    PlayerDTO playerDTO = new PlayerDTO();
                    playerDTO.setId(player.getId());
                    playerDTO.setName(player.getName());
                    playerDTO.setCreator(player.isCreator());
                    playerDTO.setCardCount(player.getCards().size());

                    // N'inclure les cartes que pour le joueur actuel
                    if (currentPlayerId != null && player.getId() != null &&
                            player.getId().equals(currentPlayerId)) {
                        log.info("Envoi des cartes pour le joueur: {}", player.getName());
                        playerDTO.setCards(player.getCards());
                    }

                    return playerDTO;
                })
                .collect(Collectors.toList()));

        // Convertir les cartes centrales
        dto.setCenterCards(game.getCenterCards());

        // Informations sur le deck (juste le nombre de cartes)
        dto.setDeckSize(game.getDeck().size());

        return dto;
    }
}