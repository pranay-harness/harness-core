package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.service.ScmClient;
import io.harness.tasks.DecryptGitApiAccessHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class ScmManagerFacilitatorServiceImpl extends AbstractScmClientFacilitatorServiceImpl {
  private ScmClient scmClient;
  private DecryptGitApiAccessHelper decryptGitApiAccessHelper;

  @Inject
  public ScmManagerFacilitatorServiceImpl(ScmClient scmClient,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper, YamlGitConfigService yamlGitConfigService,
      DecryptGitApiAccessHelper decryptGitApiAccessHelper) {
    super(connectorService, connectorErrorMessagesHelper, yamlGitConfigService);
    this.scmClient = scmClient;
    this.decryptGitApiAccessHelper = decryptGitApiAccessHelper;
  }

  @Override
  public List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL,
      io.harness.ng.beans.PageRequest pageRequest, String searchTerm) {
    final ScmConnector connector =
        getScmConnector(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef);
    ScmConnector decryptScmConnector =
        decryptGitApiAccessHelper.decryptScmApiAccess(connector, accountIdentifier, projectIdentifier, orgIdentifier);
    decryptScmConnector.setUrl(repoURL);
    return scmClient.listBranches(decryptScmConnector).getBranchesList();
  }

  @Override
  public GitFileContent getFileContent(String yamlGitConfigIdentifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filePath, String branch, String commitId) {
    validateFileContentParams(branch, commitId);
    final IdentifierRef identifierRef =
        getYamlGitConfigIdentifierRef(accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier);
    final GitFilePathDetails gitFilePathDetails = getGitFilePathDetails(filePath, branch, commitId);
    final FileContent fileContent = scmClient.getFileContent(
        decryptGitApiAccessHelper.decryptScmApiAccess(
            getScmConnector(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                identifierRef.getProjectIdentifier(), identifierRef.getIdentifier()),
            accountIdentifier, orgIdentifier, projectIdentifier),
        gitFilePathDetails);
    return validateAndGetGitFileContent(fileContent);
  }
}
