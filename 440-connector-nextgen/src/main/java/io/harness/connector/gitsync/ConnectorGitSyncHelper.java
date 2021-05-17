package io.harness.connector.gitsync;

import static io.harness.connector.entities.Connector.ConnectorKeys;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorService;
import io.harness.encryption.ScopeHelper;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.ng.core.EntityDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.function.Supplier;

@Singleton
@OwnedBy(HarnessTeam.DX)
public class ConnectorGitSyncHelper implements GitSdkEntityHandlerInterface<Connector, ConnectorDTO> {
  ConnectorMapper connectorMapper;
  ConnectorService connectorService;

  @Inject
  public ConnectorGitSyncHelper(
      @Named("connectorDecoratorService") ConnectorService connectorService, ConnectorMapper connectorMapper) {
    this.connectorService = connectorService;
    this.connectorMapper = connectorMapper;
  }

  @Override
  public Supplier<ConnectorDTO> getYamlFromEntity(Connector entity) {
    return () -> ConnectorDTO.builder().connectorInfo(connectorMapper.getConnectorInfoDTO(entity)).build();
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.CONNECTORS;
  }

  @Override
  public Supplier<Connector> getEntityFromYaml(ConnectorDTO yaml, String accountIdentifier) {
    return () -> connectorMapper.toConnector(yaml, accountIdentifier);
  }

  @Override
  public EntityDetail getEntityDetail(Connector entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.CONNECTORS)
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .scope(ScopeHelper.getScope(
                           entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier()))
                       .identifier(entity.getIdentifier())
                       .build())
        .build();
  }

  @Override
  public ConnectorDTO save(ConnectorDTO yaml, String accountIdentifier) {
    ConnectorResponseDTO connectorResponseDTO = connectorService.create(yaml, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorResponseDTO.getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Override
  public ConnectorDTO update(ConnectorDTO yaml, String accountIdentifier) {
    ConnectorResponseDTO connectorResponseDTO = connectorService.update(yaml, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorResponseDTO.getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    return connectorService.delete(entityReference.getAccountIdentifier(), entityReference.getOrgIdentifier(),
        entityReference.getProjectIdentifier(), entityReference.getIdentifier());
  }

  @Override
  public String getObjectIdOfYamlKey() {
    return ConnectorKeys.objectIdOfYaml;
  }

  @Override
  public String getIsFromDefaultBranchKey() {
    return ConnectorKeys.isFromDefaultBranch;
  }

  @Override
  public String getYamlGitConfigRefKey() {
    return ConnectorKeys.yamlGitConfigRef;
  }

  @Override
  public String getUuidKey() {
    return ConnectorKeys.id;
  }

  @Override
  public String getBranchKey() {
    return ConnectorKeys.branch;
  }
}
