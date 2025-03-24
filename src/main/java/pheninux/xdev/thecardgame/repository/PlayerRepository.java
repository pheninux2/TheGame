package pheninux.xdev.thecardgame.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pheninux.xdev.thecardgame.model.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
    // Vous pouvez ajouter des méthodes de requête personnalisées ici
}