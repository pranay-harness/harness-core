package io.harness.connector.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.exception.UnsupportedOperationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorMapper {
  KubernetesDTOToEntity kubernetesDTOToEntity;
  KubernetesEntityToDTO kubernetesEntityToDTO;
  GitDTOToEntity gitDTOToEntity;
  GitEntityToDTO gitEntityToDTO;
  public Connector toConnector(ConnectorRequestDTO connectorRequestDTO) {
    Connector connector = null;
    // todo @deepak: Change this design to something so that switch case is not required
    switch (connectorRequestDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        connector = kubernetesDTOToEntity.toKubernetesClusterConfig(
            (KubernetesClusterConfigDTO) connectorRequestDTO.getConnectorConfig());
        break;
      case GIT:
        connector = gitDTOToEntity.toGitConfig((GitConfigDTO) connectorRequestDTO.getConnectorConfig());
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
    connector.setType(connectorRequestDTO.getConnectorType());
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
        .connectorType(connector.getType())
        .build();
  }

  private ConnectorConfigDTO createConnectorConfigDTO(Connector connector) {
    // todo @deepak: Change this design to something so that switch case is not required
    switch (connector.getType()) {
      case KUBERNETES_CLUSTER:
        return kubernetesEntityToDTO.createK8ClusterConfigDTO(connector);
      case GIT:
        return gitEntityToDTO.createGitConfigDTO((GitConfig) connector);
      default:
        throw new UnsupportedOperationException(
            String.format("The connectorType [%s] is invalid", connector.getType()));
    }
  }
}
