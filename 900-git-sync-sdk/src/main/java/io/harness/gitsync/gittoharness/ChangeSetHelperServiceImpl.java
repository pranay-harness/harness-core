package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.GitSyncEntitiesConfiguration;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.manage.GlobalContextManager;
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

  private GitEntityInfo getGitEntityInfo() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      log.warn("Git branch context set as null even git sync is enabled");
      // Setting to default branch in case it is not set.
      return GitEntityInfo.builder().yamlGitConfigId(DEFAULT).branch(DEFAULT).build();
    }
    return gitSyncBranchContext.getGitBranchInfo();
  }

  @Override
  public void process(ChangeSet changeSet) {
    log.info("Inside process method, git info: {} ", getGitEntityInfo());
    EntityType entityType = EventProtoToEntityHelper.getEntityTypeFromProto(changeSet.getEntityType());
    GitSyncEntitiesConfiguration gitSyncEntitiesConfiguration = gitSyncEntityConfigurationsMap.get(entityType);
    Class<? extends GitSyncableEntity> entityClass = gitSyncEntitiesConfiguration.getEntityClass();
    GitSdkEntityHandlerInterface entityGitPersistenceHelperService =
        gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
    String yaml = changeSet.getYaml();
    switch (changeSet.getChangeType()) {
      case ADD:
      case MODIFY:
        entityGitPersistenceHelperService.upsert(changeSet.getAccountId(), yaml);
        break;
      case DELETE:
        final EntityDetailProtoDTO entityRefForDeletion = changeSet.getEntityRefForDeletion();
        final EntityDetail entityDetailDTO = entityDetailProtoToRestMapper.createEntityDetailDTO(entityRefForDeletion);
        entityGitPersistenceHelperService.delete(entityDetailDTO.getEntityRef());
        break;
      case UNRECOGNIZED:
      default:
        throw new UnexpectedException(
            String.format("Got unrecognized change set type for changeset [%s]", changeSet.getFilePath()));
    }
  }
}