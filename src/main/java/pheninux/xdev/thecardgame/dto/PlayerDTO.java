package pheninux.xdev.thecardgame.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import pheninux.xdev.thecardgame.model.Card;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDTO {
    private String id;
    private String name;
    private boolean creator;
    private int cardCount;

    // Les cartes ne sont pas envoyées pour les autres joueurs
    private List<Card> cards;
}