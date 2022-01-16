/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.persistance.EntityLookupHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class FullSyncSdkServiceImpl implements FullSyncSdkService {
  Map<EntityType, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  EntityLookupHelper entityLookupHelper;

  @Override
  public FileChanges getFileChanges(ScopeDetails scopeDetails) {
    List<FileChange> fileChangeList = new ArrayList<>();
    invalidateCacheOfGitEnabled(scopeDetails);
    gitPersistenceHelperServiceMap.forEach((entityType, gitPersistenceHelperService) -> {
      final List<FileChange> entitiesForFullSync = gitPersistenceHelperService.listAllEntities(scopeDetails);
      log.info("Entities for full sync for entityType [{}]: [{}]", entityType, entitiesForFullSync);
      fileChangeList.addAll(entitiesForFullSync);
    });
    return FileChanges.newBuilder().addAllFileChanges(fileChangeList).build();
  }

  private void invalidateCacheOfGitEnabled(ScopeDetails scopeDetails) {
    final EntityScopeInfo entityScope = scopeDetails.getEntityScope();
    entityLookupHelper.updateKey(EntityScopeInfo.newBuilder()
                                     .setOrgId(entityScope.getOrgId())
                                     .setProjectId(entityScope.getProjectId())
                                     .setAccountId(entityScope.getAccountId())
                                     .build());
  }

  @Override
  public void doFullSyncForFile(FullSyncChangeSet fullSyncChangeSet) {
    final EntityDetailProtoDTO entityDetail = fullSyncChangeSet.getEntityDetail();
    final GitSdkEntityHandlerInterface gitSdkEntityHandlerInterface =
        gitPersistenceHelperServiceMap.get(EntityType.fromString(entityDetail.getType().name()));
    gitSdkEntityHandlerInterface.fullSyncEntity(fullSyncChangeSet);
  }
}
