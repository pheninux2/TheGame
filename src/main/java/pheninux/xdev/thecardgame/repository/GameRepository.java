package pheninux.xdev.thecardgame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pheninux.xdev.thecardgame.model.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
    // Vous pouvez ajouter des méthodes de requête personnalisées ici
}