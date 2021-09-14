/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.GitSyncModule.SCM_ON_DELEGATE;
import static io.harness.gitsync.GitSyncModule.SCM_ON_MANAGER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.ScmOrchestratorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Singleton
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class ScmOrchestratorServiceImpl implements ScmOrchestratorService {
  GitSyncSettingsService gitSyncSettingsService;
  ScmClientFacilitatorService scmClientManagerService;
  ScmClientFacilitatorService scmClientDelegateService;

  @Inject
  public ScmOrchestratorServiceImpl(GitSyncSettingsService gitSyncSettingsService,
      @Named(SCM_ON_MANAGER) ScmClientFacilitatorService scmClientManagerService,
      @Named(SCM_ON_DELEGATE) ScmClientFacilitatorService scmClientDelegateService) {
    this.gitSyncSettingsService = gitSyncSettingsService;
    this.scmClientManagerService = scmClientManagerService;
    this.scmClientDelegateService = scmClientDelegateService;
  }

  @Override
  public <R> R processScmRequest(Function<ScmClientFacilitatorService, R> scmRequest, String projectIdentifier,
      String orgIdentifier, String accountId) {
    final boolean executeOnDelegate = isExecuteOnDelegate(projectIdentifier, orgIdentifier, accountId);
    if (executeOnDelegate) {
      return scmRequest.apply(scmClientDelegateService);
    }
    return scmRequest.apply(scmClientManagerService);
  }

  @Override
  public boolean isExecuteOnDelegate(String projectIdentifier, String orgIdentifier, String accountId) {
    final Optional<GitSyncSettingsDTO> gitSyncSettingsDTO =
        gitSyncSettingsService.get(accountId, orgIdentifier, projectIdentifier);
    GitSyncSettingsDTO gitSyncSettings = gitSyncSettingsDTO.orElse(GitSyncSettingsDTO.builder()
                                                                       .accountIdentifier(accountId)
                                                                       .projectIdentifier(projectIdentifier)
                                                                       .organizationIdentifier(orgIdentifier)
                                                                       .executeOnDelegate(true)
                                                                       .build());
    return gitSyncSettings.isExecuteOnDelegate();
  }
}
