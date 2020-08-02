package io.harness.connector.mappers;

import static io.harness.connector.entities.Connector.Scope.ACCOUNT;
import static io.harness.connector.entities.Connector.Scope.ORGANIZATION;
import static io.harness.connector.entities.Connector.Scope.PROJECT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
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
  AppDynamicsDTOToEntity appDynamicsDTOToEntity;
  AppDynamicsEntityToDTO appDynamicsEntityToDTO;

  @Inject private Map<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapperMap;
  @Inject private Map<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapperMap;

  public Connector toConnector(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    ConnectorDTOToEntityMapper connectorDTOToEntityMapper =
        connectorDTOToEntityMapperMap.get(connectorRequestDTO.getConnectorType().toString());
    Connector connector = connectorDTOToEntityMapper.toConnectorEntity(connectorRequestDTO.getConnectorConfig());
    connector.setIdentifier(connectorRequestDTO.getIdentifier());
    connector.setName(connectorRequestDTO.getName());
    connector.setScope(getScopeFromConnectorDTO(connectorRequestDTO));
    connector.setAccountIdentifier(accountIdentifier);
    connector.setOrgIdentifier(connectorRequestDTO.getOrgIdentifier());
    connector.setProjectIdentifier(connectorRequestDTO.getProjectIdentifer());
    connector.setFullyQualifiedIdentifier(FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorRequestDTO.getOrgIdentifier(), connectorRequestDTO.getProjectIdentifer(),
        connectorRequestDTO.getIdentifier()));
    connector.setTags(connectorRequestDTO.getTags());
    connector.setDescription(connectorRequestDTO.getDescription());
    connector.setType(connectorRequestDTO.getConnectorType());
    return connector;
  }

  @VisibleForTesting
  Connector.Scope getScopeFromConnectorDTO(ConnectorRequestDTO connectorRequestDTO) {
    if (isNotBlank(connectorRequestDTO.getProjectIdentifer())) {
      return PROJECT;
    }
    if (isNotBlank(connectorRequestDTO.getOrgIdentifier())) {
      return ORGANIZATION;
    }
    return ACCOUNT;
  }

  public ConnectorDTO writeDTO(Connector connector) {
    ConnectorConfigDTO connectorConfigDTO = createConnectorConfigDTO(connector);
    return ConnectorDTO.builder()
        .name(connector.getName())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .accountIdentifier(connector.getAccountIdentifier())
        .orgIdentifier(connector.getOrgIdentifier())
        .projectIdentifer(connector.getProjectIdentifier())
        .connectorConfig(connectorConfigDTO)
        .connectorType(connector.getType())
        .tags(connector.getTags())
        .createdAt(connector.getCreatedAt())
        .lastModifiedAt(connector.getLastModifiedAt())
        .connectorType(connector.getType())
        .status(connector.getStatus())
        .build();
  }

  private ConnectorConfigDTO createConnectorConfigDTO(Connector connector) {
    ConnectorEntityToDTOMapper connectorEntityToDTOMapper =
        connectorEntityToDTOMapperMap.get(connector.getType().toString());
    return connectorEntityToDTOMapper.createConnectorDTO(connector);
  }
}
