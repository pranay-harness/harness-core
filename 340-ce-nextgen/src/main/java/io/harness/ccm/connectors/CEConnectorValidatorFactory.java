package io.harness.ccm.connectors;

import io.harness.delegate.beans.connector.ConnectorType;

public class CEConnectorValidatorFactory {
    public static io.harness.ccm.connectors.AbstractCEConnectorValidator getValidator(ConnectorType connectorType){
        switch (connectorType) {
            case AWS:
                return new io.harness.ccm.connectors.CEAWSConnectorValidator();
            case CE_AZURE:
                return new io.harness.ccm.connectors.CEAzureConnectorValidator();
            case GCP:
                return new io.harness.ccm.connectors.CEGcpConnectorValidator();
            default:
                //TODO: add error message
        }
        return null;
    }
}