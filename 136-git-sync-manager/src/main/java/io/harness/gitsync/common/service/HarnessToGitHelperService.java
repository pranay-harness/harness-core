package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForGitPush;

@OwnedBy(DX)
public interface HarnessToGitHelperService {
  InfoForGitPush getInfoForPush(FileInfo fileInfo, EntityReference entityReference, EntityType entityType);

  void postPushOperation(PushInfo pushInfo);

  Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo);

  void processFilesInBranch(String accountId, String gitSyncConfigId, String projectIdentifier, String orgIdentifier,
      String branch, String filePathToBeExcluded, String repoURL);
}
