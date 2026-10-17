package br.com.sebratel.massivas.dto.massivas;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("send_email")
    private String sendEmail;
    @JsonProperty("send_sms")
    private String sendSms;
    @JsonProperty("email_model_id")
    private String emailModelId;
    @JsonProperty("return_email_model_id")
    private String returnEmailModelId;
    @JsonProperty("send_push")
    private String sendPush;
    @JsonProperty("push_model_id")
    private String pushModelId;
    @JsonProperty("return_push_model_id")
    private String returnPushModelId;
    private String protocolo;
}
