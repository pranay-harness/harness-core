package io.harness.connector.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorMapper {
  KubernetesDTOToEntity kubernetesDTOToEntity;
  KubernetesEntityToDTO kubernetesEntityToDTO;
  GitDTOToEntity gitDTOToEntity;
  GitEntityToDTO gitEntityToDTO;
  @Inject private Map<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapperMap;
  @Inject private Map<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapperMap;

  public Connector toConnector(ConnectorRequestDTO connectorRequestDTO) {
    ConnectorDTOToEntityMapper connectorDTOToEntityMapper =
        connectorDTOToEntityMapperMap.get(connectorRequestDTO.getConnectorType().toString());
    Connector connector = connectorDTOToEntityMapper.toConnectorEntity(connectorRequestDTO.getConnectorConfig());
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
    ConnectorEntityToDTOMapper connectorEntityToDTOMapper =
        connectorEntityToDTOMapperMap.get(connector.getType().toString());
    return connectorEntityToDTOMapper.createConnectorDTO(connector);
  }
}
