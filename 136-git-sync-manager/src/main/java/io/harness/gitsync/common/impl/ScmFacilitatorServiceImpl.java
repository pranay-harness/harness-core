package io.harness.gitsync.common.impl;

import static io.harness.gitsync.GitSyncModule.SCM_ON_DELEGATE;
import static io.harness.gitsync.GitSyncModule.SCM_ON_MANAGER;

import io.harness.connector.ManagerExecutable;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.ScmFacilitatorService;
import io.harness.ng.beans.PageRequest;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;

public class ScmFacilitatorServiceImpl implements ScmFacilitatorService {
  ScmClientFacilitatorService scmThroughManagerService;
  ScmClientFacilitatorService scmThroughDelegateService;
  GitSyncConnectorHelper gitSyncConnectorHelper;

  @Inject
  public ScmFacilitatorServiceImpl(@Named(SCM_ON_MANAGER) ScmClientFacilitatorService scmThroughManagerService,
      @Named(SCM_ON_DELEGATE) ScmClientFacilitatorService scmThroughDelegateService,
      GitSyncConnectorHelper gitSyncConnectorHelper) {
    this.scmThroughManagerService = scmThroughManagerService;
    this.scmThroughDelegateService = scmThroughDelegateService;
    this.gitSyncConnectorHelper = gitSyncConnectorHelper;
  }

  @Override
  public List<String> listBranchesUsingConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL, PageRequest pageRequest,
      String searchTerm) {
    final ScmConnector scmConnector = gitSyncConnectorHelper.getScmConnector(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef);
    if (scmConnector instanceof ManagerExecutable) {
      final Boolean executeOnManager = ((ManagerExecutable) scmConnector).getExecuteOnManager();
      if (executeOnManager) {
        return scmThroughManagerService.listBranchesForRepoByConnector(accountIdentifier, orgIdentifier,
            projectIdentifier, connectorIdentifierRef, repoURL, pageRequest, searchTerm);
      }
    }
    return scmThroughDelegateService.listBranchesForRepoByConnector(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef, repoURL, pageRequest, searchTerm);
  }
}
