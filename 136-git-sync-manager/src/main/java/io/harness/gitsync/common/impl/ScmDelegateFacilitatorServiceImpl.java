package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.scm.GitFileTaskResponseData;
import io.harness.delegate.task.scm.GitFileTaskType;
import io.harness.delegate.task.scm.GitPRTaskType;
import io.harness.delegate.task.scm.GitRefType;
import io.harness.delegate.task.scm.ScmGitFileTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.delegate.task.scm.ScmPRTaskParams;
import io.harness.delegate.task.scm.ScmPRTaskResponseData;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.BaseNGAccess;
import io.harness.product.ci.scm.proto.CreatePRResponse;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

// Don't inject this directly go through ScmClientOrchestrator.
@Slf4j
@OwnedBy(DX)
public class ScmDelegateFacilitatorServiceImpl extends AbstractScmClientFacilitatorServiceImpl {
  private SecretManagerClientService secretManagerClientService;
  private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Inject
  public ScmDelegateFacilitatorServiceImpl(@Named("connectorDecoratorService") ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper, YamlGitConfigService yamlGitConfigService,
      SecretManagerClientService secretManagerClientService, DelegateGrpcClientWrapper delegateGrpcClientWrapper) {
    super(connectorService, connectorErrorMessagesHelper, yamlGitConfigService);
    this.secretManagerClientService = secretManagerClientService;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
  }

  @Override
  public List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL, PageRequest pageRequest,
      String searchTerm) {
    final IdentifierRef gitConnectorIdentifierRef =
        getConnectorIdentifierRef(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef);
    final ScmConnector scmConnector = getScmConnector(gitConnectorIdentifierRef);
    scmConnector.setUrl(repoURL);
    final BaseNGAccess baseNGAccess = getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    final DecryptableEntity apiAccessDecryptableEntity =
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmConnector);
    final List<EncryptedDataDetail> encryptionDetails =
        secretManagerClientService.getEncryptionDetails(baseNGAccess, apiAccessDecryptableEntity);
    final ScmGitRefTaskParams scmGitRefTaskParams = ScmGitRefTaskParams.builder()
                                                        .scmConnector(scmConnector)
                                                        .gitRefType(GitRefType.BRANCH)
                                                        .encryptedDataDetails(encryptionDetails)
                                                        .build();
    DelegateTaskRequest delegateTaskRequest =
        getDelegateTaskRequest(accountIdentifier, scmGitRefTaskParams, TaskType.SCM_GIT_REF_TASK);
    final DelegateResponseData delegateResponseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    ScmGitRefTaskResponseData scmGitRefTaskResponseData = (ScmGitRefTaskResponseData) delegateResponseData;
    return scmGitRefTaskResponseData.getListBranchesResponse().getBranchesList();
  }

  @Override
  public GitFileContent getFileContent(String yamlGitConfigIdentifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filePath, String branch, String commitId) {
    validateFileContentParams(branch, commitId);
    YamlGitConfigDTO yamlGitConfigDTO =
        getYamlGitConfigDTO(accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier);
    final IdentifierRef gitConnectorIdentifierRef =
        getConnectorIdentifierRef(yamlGitConfigDTO.getAccountIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
            yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getGitConnectorRef());
    final ScmConnector scmConnector = getScmConnector(gitConnectorIdentifierRef);
    final BaseNGAccess baseNGAccess = getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    final DecryptableEntity apiAccessDecryptableEntity =
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmConnector);
    final List<EncryptedDataDetail> encryptionDetails =
        secretManagerClientService.getEncryptionDetails(baseNGAccess, apiAccessDecryptableEntity);
    final GitFilePathDetails gitFilePathDetails = getGitFilePathDetails(filePath, branch, commitId);
    final ScmGitFileTaskParams scmGitFileTaskParams = ScmGitFileTaskParams.builder()
                                                          .gitFileTaskType(GitFileTaskType.GET_FILE_CONTENT)
                                                          .scmConnector(scmConnector)
                                                          .gitFilePathDetails(gitFilePathDetails)
                                                          .encryptedDataDetails(encryptionDetails)
                                                          .build();
    DelegateTaskRequest delegateTaskRequest =
        getDelegateTaskRequest(accountIdentifier, scmGitFileTaskParams, TaskType.SCM_GIT_FILE_TASK);
    final DelegateResponseData delegateResponseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    GitFileTaskResponseData gitFileTaskResponseData = (GitFileTaskResponseData) delegateResponseData;
    return validateAndGetGitFileContent(gitFileTaskResponseData.getFileContent());
  }

  @Override
  public Boolean createPullRequest(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigRef, GitPRCreateRequest gitCreatePRRequest) {
    YamlGitConfigDTO yamlGitConfigDTO =
        getYamlGitConfigDTO(accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigRef);
    final IdentifierRef gitConnectorIdentifierRef =
        getConnectorIdentifierRef(yamlGitConfigDTO.getAccountIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
            yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getGitConnectorRef());
    final ScmConnector scmConnector = getScmConnector(gitConnectorIdentifierRef);
    final BaseNGAccess baseNGAccess = getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    final DecryptableEntity apiAccessDecryptableEntity =
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmConnector);
    final List<EncryptedDataDetail> encryptionDetails =
        secretManagerClientService.getEncryptionDetails(baseNGAccess, apiAccessDecryptableEntity);
    ScmPRTaskParams scmPRTaskParams = ScmPRTaskParams.builder()
                                          .scmConnector(scmConnector)
                                          .gitPRCreateRequest(gitCreatePRRequest)
                                          .gitPRTaskType(GitPRTaskType.CREATE_PR)
                                          .encryptedDataDetails(encryptionDetails)
                                          .build();
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(accountIdentifier)
                                                  .taskType(TaskType.SCM_PULL_REQUEST_TASK.name())
                                                  .taskParameters(scmPRTaskParams)
                                                  .executionTimeout(Duration.ofMinutes(2))
                                                  .build();
    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    ScmPRTaskResponseData scmCreatePRResponse = (ScmPRTaskResponseData) responseData;
    final CreatePRResponse createPRResponse = scmCreatePRResponse.getCreatePRResponse();
    if (createPRResponse.getStatus() != 200 || createPRResponse.getStatus() != 201) {
      log.error("Could not create the pull request from {} to {}", gitCreatePRRequest.getSourceBranch(),
          gitCreatePRRequest.getTargetBranch());
    }
    return createPRResponse.getStatus() == 200 || createPRResponse.getStatus() == 201;
  }

  DelegateTaskRequest getDelegateTaskRequest(
      String accountIdentifier, TaskParameters taskParameters, TaskType taskType) {
    return DelegateTaskRequest.builder()
        .accountId(accountIdentifier)
        .taskParameters(taskParameters)
        .taskType(taskType.name())
        .executionTimeout(Duration.ofMinutes(2))
        .build();
  }

  private BaseNGAccess getBaseNGAccess(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
