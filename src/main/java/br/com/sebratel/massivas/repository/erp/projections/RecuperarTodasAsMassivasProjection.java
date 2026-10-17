package br.com.sebratel.massivas.repository.erp.projections;

import java.time.LocalDateTime;

public interface RecuperarTodasAsMassivasProjection {
    Long getID();

    String getDESCRICAO();

    LocalDateTime getCRIACAO();

    LocalDateTime getFINALIZADO();

    String getPROTOCOLO();

    String getEQUIPE();

    String getSTATUS();

    String getTIPO_SOLICITACAO();

    String getSOLICITANTE();
    LocalDateTime getSLA();

    String getRESPONSAVEL();
    String getPONTODEACESSO();
    String getCATALOGO();
}
