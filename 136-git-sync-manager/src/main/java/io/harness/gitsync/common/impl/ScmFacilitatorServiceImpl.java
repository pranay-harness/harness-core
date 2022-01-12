/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.service.ScmFacilitatorService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.ng.beans.PageRequest;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.PL)
public class ScmFacilitatorServiceImpl implements ScmFacilitatorService {
  GitSyncConnectorHelper gitSyncConnectorHelper;
  ScmOrchestratorService scmOrchestratorService;

  @Inject
  public ScmFacilitatorServiceImpl(
      GitSyncConnectorHelper gitSyncConnectorHelper, ScmOrchestratorService scmOrchestratorService) {
    this.gitSyncConnectorHelper = gitSyncConnectorHelper;
    this.scmOrchestratorService = scmOrchestratorService;
  }

  @Override
  public List<String> listBranchesUsingConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL, PageRequest pageRequest,
      String searchTerm) {
    return scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
        -> scmClientFacilitatorService.listBranchesForRepoByConnector(accountIdentifier, orgIdentifier,
            projectIdentifier, connectorIdentifierRef, repoURL, pageRequest, searchTerm),
        projectIdentifier, orgIdentifier, accountIdentifier, connectorIdentifierRef);
  }
}
