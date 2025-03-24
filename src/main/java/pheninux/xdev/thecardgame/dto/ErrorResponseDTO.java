package pheninux.xdev.thecardgame.dto;

import lombok.Data;

@Data
public class ErrorResponseDTO {
    private boolean error;
    private String message;
}
