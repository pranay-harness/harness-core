/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.EntityInfo;
import io.harness.gitsync.GitSyncEntitiesConfiguration;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.core.event.EventProtoToEntityHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class ChangeSetHelperServiceImpl implements GitSdkInterface {
  @Inject
  @Named("GitSyncEntityConfigurations")
  Map<EntityType, GitSyncEntitiesConfiguration> gitSyncEntityConfigurationsMap;
  @Inject @Named("GitSyncObjectMapper") ObjectMapper objectMapper;
  @Inject Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  @Inject EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;

  @Override
  public void process(ChangeSet changeSet) {
    EntityType entityType = EventProtoToEntityHelper.getEntityTypeFromProto(changeSet.getEntityType());
    GitSyncEntitiesConfiguration gitSyncEntitiesConfiguration = gitSyncEntityConfigurationsMap.get(entityType);
    Class<? extends GitSyncableEntity> entityClass = gitSyncEntitiesConfiguration.getEntityClass();
    GitSdkEntityHandlerInterface entityGitPersistenceHelperService =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    String yaml = changeSet.getYaml();
    switch (changeSet.getChangeType()) {
      case ADD:
      case MODIFY:
        entityGitPersistenceHelperService.upsert(changeSet.getAccountId(), yaml, changeSet.getFilePath());
        break;
      case DELETE:
        final EntityDetailProtoDTO entityRefForDeletion = changeSet.getEntityRefForDeletion();
        final EntityDetail entityDetailDTO = entityDetailProtoToRestMapper.createEntityDetailDTO(entityRefForDeletion);
        entityGitPersistenceHelperService.delete(entityDetailDTO.getEntityRef());
        break;
      case RENAME:
        entityGitPersistenceHelperService.updateFilePath(
            changeSet.getAccountId(), yaml, changeSet.getPrevFilePath(), changeSet.getFilePath());
        break;
      case UNRECOGNIZED:
      default:
        throw new UnexpectedException(
            String.format("Got unrecognized change set type for changeset [%s]", changeSet.getFilePath()));
    }
  }

  @Override
  public boolean markEntityInvalid(String accountId, EntityInfo entityInfo) {
    EntityType entityType = EventProtoToEntityHelper.getEntityTypeFromProto(entityInfo.getEntityDetail().getType());
    GitSyncEntitiesConfiguration gitSyncEntitiesConfiguration = gitSyncEntityConfigurationsMap.get(entityType);
    Class<? extends GitSyncableEntity> entityClass = gitSyncEntitiesConfiguration.getEntityClass();
    GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    if (gitSdkEntityHandlerInterface == null) {
      return false;
    }
    final EntityDetail entityDetailDTO =
        entityDetailProtoToRestMapper.createEntityDetailDTO(entityInfo.getEntityDetail());
    return gitSdkEntityHandlerInterface.markEntityInvalid(
        accountId, entityDetailDTO.getEntityRef(), entityInfo.getYaml());
  }
}
