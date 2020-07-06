package io.harness.cdng.connectornextgen.impl;

import com.google.inject.Inject;

import io.harness.cdng.connectornextgen.KubernetesConnectionValidator;
import io.harness.cdng.connectornextgen.service.ConnectorValidationService;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.connector.ConnectorRequestDTO;
import io.harness.connector.common.ConnectorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorValidationServiceImpl implements ConnectorValidationService {
  private KubernetesConnectionValidator kubernetesConnectionValidator;

  public boolean validate(ConnectorRequestDTO connectorDTO, String accountId) {
    ConnectorType connectorType = connectorDTO.getConnectorType();
    switch (connectorType) {
      case KUBERNETES_CLUSTER:
        return kubernetesConnectionValidator.validate(
            (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig(), accountId);
      default:
        throw new UnsupportedOperationException(String.format("The connector type %s is invalid", connectorType));
    }
  }
}
