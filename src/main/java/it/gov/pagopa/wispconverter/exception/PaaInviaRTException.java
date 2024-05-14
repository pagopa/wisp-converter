package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;

@Getter
public class PaaInviaRTException extends RuntimeException {

    private final String faultCode;
    private final String faultString;
    private final String description;

    public PaaInviaRTException(String faultCode, String faultString, String description) {
        this.faultCode = faultCode;
        this.faultString = faultString;
        this.description = description;
    }

    public String getErrorMessage(){
        StringBuilder errorMessage = new StringBuilder();
        if( faultCode != null ) {
            errorMessage.append("FaultCode: ").append(faultCode);
        }
        if( faultString != null ) {
            errorMessage.append("FaultString: ").append(faultString);
        }
        if( description != null ) {
            errorMessage.append("Description: ").append(description);
        }
        return errorMessage.toString();
    }

}
