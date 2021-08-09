package io.harness.connector.gitsync;

import static io.harness.connector.entities.Connector.ConnectorKeys;
import static io.harness.remote.NGObjectMapperHelper.configureNGObjectMapper;

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
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.exceptions.NGYamlParsingException;
import io.harness.ng.core.EntityDetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
public class ConnectorGitSyncHelper extends AbstractGitSdkEntityHandler<Connector, ConnectorDTO>
    implements GitSdkEntityHandlerInterface<Connector, ConnectorDTO> {
  ConnectorMapper connectorMapper;
  ConnectorService connectorService;
  ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  @Inject
  public ConnectorGitSyncHelper(
      @Named("connectorDecoratorService") ConnectorService connectorService, ConnectorMapper connectorMapper) {
    this.connectorService = connectorService;
    this.connectorMapper = connectorMapper;
    configureNGObjectMapper(objectMapper);
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
  public ConnectorDTO save(String accountIdentifier, String yaml) {
    ConnectorDTO connectorDTO = getYamlDTO(yaml);
    ConnectorResponseDTO connectorResponseDTO = connectorService.create(connectorDTO, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorResponseDTO.getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Override
  public ConnectorDTO update(String accountIdentifier, String yaml) {
    ConnectorDTO connectorDTO = getYamlDTO(yaml);
    ConnectorResponseDTO connectorResponseDTO = connectorService.update(connectorDTO, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorResponseDTO.getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Override
  public ConnectorDTO getYamlDTO(String yaml) {
    try {
      return objectMapper.readValue(yaml, ConnectorDTO.class);
    } catch (IOException ex) {
      log.error("Error converting the yaml file [{}]", yaml, ex);
      throw new NGYamlParsingException(String.format("Could not parse the YAML %s", yaml));
    }
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

  @Override
  public String getLastObjectIdIfExists(String accountIdentifier, String yaml) {
    final ConnectorDTO connectorDTO = getYamlDTO(yaml);
    final ConnectorInfoDTO connectorInfo = connectorDTO.getConnectorInfo();
    final Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(accountIdentifier,
        connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier());
    return connectorResponseDTO.map(connectorResponse -> connectorResponse.getGitDetails().getObjectId()).orElse(null);
  }
}
