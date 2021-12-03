package io.harness.ccm.connectors;

import io.harness.delegate.beans.connector.ConnectorType;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CEConnectorValidatorFactory {
  @Inject Injector injector;
  public io.harness.ccm.connectors.AbstractCEConnectorValidator getValidator(ConnectorType connectorType) {
    switch (connectorType) {
      case CE_AWS:
        return injector.getInstance(CEAWSConnectorValidator.class);
      case CE_AZURE:
        return injector.getInstance(CEAzureConnectorValidator.class);
      case GCP_CLOUD_COST:
        return injector.getInstance(CEGcpConnectorValidator.class);
      default:
        log.error("Unknown connector type: {}", connectorType);
    }
    return null;
  }
}