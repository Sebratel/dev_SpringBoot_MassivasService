package br.com.sebratel.massivas.dto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class EllevenCredentialsDTO {

    @Value("${elleven.client-id}")
    private String clientId;

    @Value("${elleven.client-secret}")
    private String clientSecret;

    @Value("${elleven.syndata}")
    private String syndata;

    @Value("${elleven.grant-type}")
    private String grantType;

    @Value("${elleven.scope}")
    private String scope;

    public MultiValueMap<String, String> toFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("syndata", syndata);
        formData.add("grant_type", grantType);
        formData.add("scope", scope);
        return formData;
    }
}