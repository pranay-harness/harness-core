/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.interceptor.GitSyncBranchContext.NG_GIT_SYNC_CONTEXT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.BranchDetails;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.RepoDetails;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.manage.GlobalContextManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitSyncSdkServiceImpl implements GitSyncSdkService {
  private final EntityKeySource entityKeySource;
  private final HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;

  @Override
  public boolean isGitSyncEnabled(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    try {
      return entityKeySource.fetchKey(buildEntityScopeInfo(projectIdentifier, orgIdentifier, accountIdentifier));
    } catch (Exception ex) {
      log.error("Exception while communicating to the git sync service", ex);
      return false;
    }
  }

  private EntityScopeInfo buildEntityScopeInfo(String projectIdentifier, String orgIdentifier, String accountId) {
    final EntityScopeInfo.Builder entityScopeInfoBuilder = EntityScopeInfo.newBuilder().setAccountId(accountId);
    if (!isEmpty(projectIdentifier)) {
      entityScopeInfoBuilder.setProjectId(StringValue.of(projectIdentifier));
    }
    if (!isEmpty(orgIdentifier)) {
      entityScopeInfoBuilder.setOrgId(StringValue.of(orgIdentifier));
    }
    return entityScopeInfoBuilder.build();
  }

  @Override
  public boolean isDefaultBranch(String accountId, String orgIdentifier, String projectIdentifier) {
    final GlobalContextData globalContextData = GlobalContextManager.get(NG_GIT_SYNC_CONTEXT);
    // In case global context is not set then return true.
    if (globalContextData == null) {
      return true;
    }
    final GitSyncBranchContext gitSyncBranchContext = (GitSyncBranchContext) globalContextData;
    final GitEntityInfo gitBranchInfo = gitSyncBranchContext.getGitBranchInfo();
    if (gitBranchInfo.getYamlGitConfigId() == null
        || gitBranchInfo.getYamlGitConfigId().equals(GitSyncConstants.DEFAULT)) {
      return true;
    }
    final RepoDetails.Builder repoDetailsBuilder = RepoDetails.newBuilder()
                                                       .setAccountId(accountId)
                                                       .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
                                                       .putAllContextMap(MDC.getCopyOfContextMap());
    if (!isEmpty(projectIdentifier)) {
      repoDetailsBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
    }
    if (!isEmpty(orgIdentifier)) {
      repoDetailsBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
    }
    final BranchDetails defaultBranch =
        harnessToGitPushInfoServiceBlockingStub.getDefaultBranch(repoDetailsBuilder.build());
    return defaultBranch.getDefaultBranch().equals(gitBranchInfo.getBranch());
  }
}
