package pheninux.xdev.thecardgame.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Player {
    @Id
    private String id;

    private String name;
    private boolean creator;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_cards", joinColumns = @JoinColumn(name = "player_id"))
    private List<Card> cards = new ArrayList<>();
}
