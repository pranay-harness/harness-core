/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers;

import static io.harness.connector.ConnectivityStatus.UNKNOWN;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorActivityDetails;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorRegistryFactory;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.DelegateSelectable;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.Scope;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.DX)
public class ConnectorMapper {
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
    connector.setCategories(Arrays.asList(ConnectorRegistryFactory.getConnectorCategory(connector.getType())));
    if (connectorInfo.getConnectorConfig() instanceof DelegateSelectable) {
      Set<String> delegateSelectors = ((DelegateSelectable) connectorInfo.getConnectorConfig()).getDelegateSelectors();
      connector.setDelegateSelectors(delegateSelectors);
    }
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
    ConnectorInfoDTO connectorInfo = getConnectorInfoDTO(connector);
    Long timeWhenConnectorIsLastUpdated =
        getTimeWhenTheConnectorWasUpdated(connector.getTimeWhenConnectorIsLastUpdated(), connector.getLastModifiedAt());
    EntityGitDetails entityGitDetails = EntityGitDetailsMapper.mapEntityGitDetails(connector);
    return ConnectorResponseDTO.builder()
        .connector(connectorInfo)
        .status(updateTheConnectivityStatus(connector.getConnectivityDetails(), connector.getIsFromDefaultBranch()))
        .createdAt(connector.getCreatedAt())
        .lastModifiedAt(timeWhenConnectorIsLastUpdated)
        .harnessManaged(isHarnessManaged(connector))
        .activityDetails(getConnectorActivity(connector.getActivityDetails(), timeWhenConnectorIsLastUpdated))
        .gitDetails(entityGitDetails)
        .build();
  }

  public ConnectorInfoDTO getConnectorInfoDTO(Connector connector) {
    ConnectorConfigDTO connectorConfigDTO = createConnectorConfigDTO(connector);
    return ConnectorInfoDTO.builder()
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
  }

  private ConnectorActivityDetails getConnectorActivity(
      ConnectorActivityDetails activityDetails, Long timeWhenConnectorIsLastUpdated) {
    if (activityDetails == null) {
      return ConnectorActivityDetails.builder().lastActivityTime(timeWhenConnectorIsLastUpdated).build();
    }
    return activityDetails;
  }

  private ConnectorConnectivityDetails updateTheConnectivityStatus(
      ConnectorConnectivityDetails connectivityDetails, Boolean isFromDefaultBranch) {
    updateLastTestedAt(connectivityDetails);
    if (connectivityDetails == null) {
      if (isFromDefaultBranch != null && !isFromDefaultBranch) {
        return ConnectorConnectivityDetails.builder().status(UNKNOWN).build();
      }
    }
    return connectivityDetails;
  }

  private ConnectorConnectivityDetails updateLastTestedAt(ConnectorConnectivityDetails connectivityDetails) {
    if (connectivityDetails == null) {
      return null;
    }
    if (connectivityDetails.getTestedAt() == 0L) {
      connectivityDetails.setTestedAt(connectivityDetails.getLastTestedAt());
    }
    return connectivityDetails;
  }

  private Long getTimeWhenTheConnectorWasUpdated(Long timeWhenConnectorIsLastUpdated, Long lastModifiedAt) {
    // todo @deepak: Remove this logic later, currently it handles the old records too where the new field
    // timeWhenConnectorIsLastUpdated is not present
    if (timeWhenConnectorIsLastUpdated == null) {
      return lastModifiedAt;
    }
    return timeWhenConnectorIsLastUpdated;
  }

  private boolean isHarnessManaged(Connector connector) {
    switch (connector.getType()) {
      case GCP_KMS:
        return Boolean.TRUE.equals(((GcpKmsConnector) connector).getHarnessManaged());
      case AWS_KMS:
        return Boolean.TRUE.equals(((AwsKmsConnector) connector).getHarnessManaged());
      case LOCAL:
        return Boolean.TRUE.equals(((LocalConnector) connector).getHarnessManaged());
      default:
        return false;
    }
  }

  private ConnectorConfigDTO createConnectorConfigDTO(Connector connector) {
    ConnectorEntityToDTOMapper connectorEntityToDTOMapper =
        connectorEntityToDTOMapperMap.get(connector.getType().toString());
    ConnectorConfigDTO connectorDTO = connectorEntityToDTOMapper.createConnectorDTO(connector);
    if (connectorDTO instanceof DelegateSelectable) {
      Set<String> delegateSelectors = Optional.ofNullable(connector.getDelegateSelectors()).orElse(new HashSet<>());
      ((DelegateSelectable) connectorDTO).setDelegateSelectors(delegateSelectors);
    }
    return connectorDTO;
  }
}
