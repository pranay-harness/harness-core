/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.gitsync;

import static io.harness.connector.entities.Connector.ConnectorKeys;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.remote.NGObjectMapperHelper.configureNGObjectMapper;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.helper.ConnectorEntityDetailUtils;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorService;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.exceptions.NGYamlParsingException;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.utils.NGYamlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.List;
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
  ConnectorFullSyncHelper connectorFullSyncHelper;

  @Inject
  public ConnectorGitSyncHelper(@Named("connectorDecoratorService") ConnectorService connectorService,
      ConnectorMapper connectorMapper, ConnectorFullSyncHelper connectorFullSyncHelper) {
    this.connectorService = connectorService;
    this.connectorMapper = connectorMapper;
    configureNGObjectMapper(objectMapper);
    this.connectorFullSyncHelper = connectorFullSyncHelper;
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
    return ConnectorEntityDetailUtils.getEntityDetail(entity);
  }

  @Override
  public ConnectorDTO save(String accountIdentifier, String yaml) {
    ConnectorDTO connectorDTO = getYamlDTO(yaml);
    validate(connectorDTO);
    ConnectorResponseDTO connectorResponseDTO = connectorService.create(connectorDTO, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorResponseDTO.getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Override
  public ConnectorDTO update(String accountIdentifier, String yaml, ChangeType changeType) {
    ConnectorDTO connectorDTO = getYamlDTO(yaml);
    validate(connectorDTO);
    ConnectorResponseDTO connectorResponseDTO = connectorService.update(connectorDTO, accountIdentifier, changeType);
    ConnectorInfoDTO connectorInfo = connectorResponseDTO.getConnector();
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Override
  public boolean markEntityInvalid(String accountIdentifier, EntityReference entityReference, String erroneousYaml) {
    return connectorService.markEntityInvalid(accountIdentifier, entityReference, erroneousYaml);
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
  public List<FileChange> listAllEntities(ScopeDetails scope) {
    return connectorFullSyncHelper.getAllEntitiesForFullSync(scope);
  }

  @Override
  public String getYamlFromEntityRef(EntityDetailProtoDTO entityReference) {
    final IdentifierRefProtoDTO identifierRef = entityReference.getIdentifierRef();
    final Optional<ConnectorResponseDTO> connectorResponseDTO =
        connectorService.get(StringValueUtils.getStringFromStringValue(identifierRef.getAccountIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getOrgIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getProjectIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getIdentifier()));
    return NGYamlUtils.getYamlString(
        ConnectorDTO.builder().connectorInfo(connectorResponseDTO.get().getConnector()).build(), objectMapper);
  }

  @Override
  public Optional<EntityGitDetails> getEntityDetailsIfExists(String accountIdentifier, String yaml) {
    final ConnectorDTO connectorDTO = getYamlDTO(yaml);
    final ConnectorInfoDTO connectorInfo = connectorDTO.getConnectorInfo();
    final Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(accountIdentifier,
        connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier());
    return connectorResponseDTO.map(ConnectorResponseDTO::getGitDetails);
  }

  @Override
  public ConnectorDTO fullSyncEntity(FullSyncChangeSet fullSyncChangeSet) {
    final EntityDetailProtoDTO entityDetail = fullSyncChangeSet.getEntityDetail();
    final String yaml = getYamlFromEntityRef(entityDetail);
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      GlobalContextManager.upsertGlobalContextRecord(super.createGitEntityInfo(fullSyncChangeSet));
      return update(fullSyncChangeSet.getAccountIdentifier(), yaml, ChangeType.ADD);
    }
  }
}
