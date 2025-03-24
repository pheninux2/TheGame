package pheninux.xdev.thecardgame.dto;

import lombok.Data;

@Data
public class PlayerActionDTO {
    private String playerId;
    private int cardIndex;
    private String color;
}
