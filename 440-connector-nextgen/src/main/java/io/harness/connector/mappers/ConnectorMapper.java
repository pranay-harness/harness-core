package io.harness.connector.mappers;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.Scope;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

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

  public Connector toConnector(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connectorRequestDTO.getConnectorInfo();
    ConnectorDTOToEntityMapper connectorDTOToEntityMapper =
        connectorDTOToEntityMapperMap.get(connectorInfo.getConnectorType().toString());
    Connector connector = connectorDTOToEntityMapper.toConnectorEntity(connectorInfo.getConnectorConfig());
    connector.setIdentifier(connectorInfo.getIdentifier());
    connector.setName(connectorInfo.getName());
    connector.setScope(getScopeFromConnectorDTO(connectorRequestDTO));
    connector.setAccountIdentifier(accountIdentifier);
    connector.setOrgIdentifier(connectorInfo.getOrgIdentifier());
    connector.setProjectIdentifier(connectorInfo.getProjectIdentifier());
    connector.setFullyQualifiedIdentifier(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier()));
    connector.setTags(TagMapper.convertToList(connectorInfo.getTags()));
    connector.setDescription(connectorInfo.getDescription());
    connector.setType(connectorInfo.getConnectorType());
    connector.setCategories(connectorDTOToEntityMapper.getConnectorCategory());
    return connector;
  }

  @VisibleForTesting
  Scope getScopeFromConnectorDTO(ConnectorDTO connectorRequestDTO) {
    ConnectorInfoDTO connectorInfo = connectorRequestDTO.getConnectorInfo();
    if (isNotBlank(connectorInfo.getProjectIdentifier())) {
      return Scope.PROJECT;
    }
    if (isNotBlank(connectorInfo.getOrgIdentifier())) {
      return Scope.ORG;
    }
    return Scope.ACCOUNT;
  }

  public ConnectorResponseDTO writeDTO(Connector connector) {
    ConnectorConfigDTO connectorConfigDTO = createConnectorConfigDTO(connector);
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder()
                                         .name(connector.getName())
                                         .identifier(connector.getIdentifier())
                                         .description(connector.getDescription())
                                         .orgIdentifier(connector.getOrgIdentifier())
                                         .projectIdentifier(connector.getProjectIdentifier())
                                         .connectorConfig(connectorConfigDTO)
                                         .connectorType(connector.getType())
                                         .tags(TagMapper.convertToMap(connector.getTags()))
                                         .connectorType(connector.getType())
                                         .build();
    return ConnectorResponseDTO.builder()
        .connector(connectorInfo)
        .status(connector.getConnectivityDetails())
        .createdAt(connector.getCreatedAt())
        .lastModifiedAt(connector.getLastModifiedAt())
        .build();
  }

  private ConnectorConfigDTO createConnectorConfigDTO(Connector connector) {
    ConnectorEntityToDTOMapper connectorEntityToDTOMapper =
        connectorEntityToDTOMapperMap.get(connector.getType().toString());
    return connectorEntityToDTOMapper.createConnectorDTO(connector);
  }
}
