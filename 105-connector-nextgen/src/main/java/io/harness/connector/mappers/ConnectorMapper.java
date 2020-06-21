package io.harness.connector.mappers;

import com.google.inject.Inject;

import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.connector.ConnectorConfigDTO;
import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.exception.UnsupportedOperationException;

public class ConnectorMapper {
  @Inject KubernetesDTOToEntity kubernetesDTOToEntity;
  @Inject KubernetesEntityToDTO kubernetesEntityToDTO;
  public Connector toConnector(ConnectorDTO connectorDTO) {
    Connector connector = null;
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        connector = kubernetesDTOToEntity.toKubernetesClusterConfig(
            (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig());
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("The connectorType [%s] is invalid", connectorDTO.getConnectorType()));
    }
    connector.setIdentifier(connectorDTO.getIdentifier());
    connector.setName(connectorDTO.getName());
    return connector;
  }

  public ConnectorDTO writeDTO(Connector connector) {
    ConnectorConfigDTO connectorConfigDTO = createConnectorConfigDTO(connector);
    return ConnectorDTO.builder()
        .name(connector.getName())
        .identifier(connector.getIdentifier())
        .connectorConfig(connectorConfigDTO)
        .connectorType(connector.getType())
        .build();
  }

  private ConnectorConfigDTO createConnectorConfigDTO(Connector connector) {
    switch (connector.getType()) {
      case KUBERNETES_CLUSTER:
        return kubernetesEntityToDTO.createK8ClusterConfigDTO(connector);
      default:
        throw new UnsupportedOperationException(
            String.format("The connectorType [%s] is invalid", connector.getType()));
    }
  }
}
