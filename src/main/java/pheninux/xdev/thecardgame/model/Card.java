package pheninux.xdev.thecardgame.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Card {
    private String color;
    private String symbol;
}
