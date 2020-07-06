package io.harness.connector.mappers;

import com.google.inject.Inject;

import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.exception.UnsupportedOperationException;

public class ConnectorMapper {
  @Inject KubernetesDTOToEntity kubernetesDTOToEntity;
  @Inject KubernetesEntityToDTO kubernetesEntityToDTO;
  public Connector toConnector(ConnectorRequestDTO connectorRequestDTO) {
    Connector connector = null;
    switch (connectorRequestDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        connector = kubernetesDTOToEntity.toKubernetesClusterConfig(
            (KubernetesClusterConfigDTO) connectorRequestDTO.getConnectorConfig());
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("The connectorType [%s] is invalid", connectorRequestDTO.getConnectorType()));
    }
    connector.setIdentifier(connectorRequestDTO.getIdentifier());
    connector.setName(connectorRequestDTO.getName());
    connector.setAccountId(connectorRequestDTO.getAccountIdentifier());
    connector.setOrgId(connectorRequestDTO.getOrgIdentifier());
    connector.setProjectId(connectorRequestDTO.getProjectIdentifer());
    connector.setFullyQualifiedIdentifier(FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        connectorRequestDTO.getAccountIdentifier(), connectorRequestDTO.getOrgIdentifier(),
        connectorRequestDTO.getProjectIdentifer(), connectorRequestDTO.getIdentifier()));
    connector.setTags(connectorRequestDTO.getTags());
    connector.setDescription(connectorRequestDTO.getDescription());
    return connector;
  }

  public ConnectorDTO writeDTO(Connector connector) {
    ConnectorConfigDTO connectorConfigDTO = createConnectorConfigDTO(connector);
    return ConnectorDTO.builder()
        .name(connector.getName())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .accountIdentifier(connector.getAccountId())
        .orgIdentifier(connector.getOrgId())
        .projectIdentifer(connector.getProjectId())
        .connectorConfig(connectorConfigDTO)
        .connectorType(connector.getType())
        .tags(connector.getTags())
        .createdAt(connector.getCreatedAt())
        .lastModifiedAt(connector.getLastModifiedAt())
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
