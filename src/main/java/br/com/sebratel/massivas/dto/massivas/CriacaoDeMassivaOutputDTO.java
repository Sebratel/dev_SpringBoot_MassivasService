package br.com.sebratel.massivas.dto.massivas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriacaoDeMassivaOutputDTO {
    private String id;
    private CriacaoDeMassivaInputDTO input;
    private String send_email;
    private String send_sms;
    private String email_model_id;
    private String return_email_model_id;
    private String send_push;
    private String push_model_id;
    private String return_push_model_id;
    private String protocolo;
}
