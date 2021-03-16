package io.harness.tasks;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.FeatureName;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.gitapi.DecryptGitAPIAccessTaskResponse;
import io.harness.delegate.beans.gitapi.DecryptGitAPiAccessTaskParams;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.core.NGAccess;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DecryptGitApiAccessHelper {
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private EncryptionHelper encryptionHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private GitApiAccessDecryptionHelper gitApiAccessDecryptionHelper;

  public GithubConnectorDTO decryptGithubApiAccess(GithubConnectorDTO githubConnectorDTO, NGAccess ngAccess) {
    List<EncryptedDataDetail> encryptedDataDetailsForAPIAccess = getEncryptedDataDetailsForAPIAccess(
        gitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(githubConnectorDTO), ngAccess);
    return (GithubConnectorDTO) executeDecryptionTask(
        githubConnectorDTO, ngAccess.getAccountIdentifier(), encryptedDataDetailsForAPIAccess);
  }

  private ScmConnector executeDecryptionTask(
      ScmConnector scmConnector, String accountIdentifier, List<EncryptedDataDetail> encryptedDataDetailsForAPIAccess) {
    if (!featureFlagService.isEnabled(FeatureName.GIT_SYNC_NG, accountIdentifier)) {
      throw new InvalidRequestException("Decrypting the git details is not supported for this account");
    }
    DecryptGitAPiAccessTaskParams apiAccessTaskParams = DecryptGitAPiAccessTaskParams.builder()
                                                            .scmConnector(scmConnector)
                                                            .encryptedDataDetails(encryptedDataDetailsForAPIAccess)
                                                            .build();
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(accountIdentifier)
                                                  .taskType("NG_DECRYT_GIT_API_ACCESS_TASK")
                                                  .taskParameters(apiAccessTaskParams)
                                                  .executionTimeout(Duration.ofMinutes(2))
                                                  .build();
    DelegateResponseData responseData = delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    if (responseData instanceof ErrorNotifyResponseData || responseData instanceof RemoteMethodReturnValueData) {
      log.error("Error decrypting the credentials, the responseData returned from delegate: {}", responseData);
      throw new UnexpectedException("Error while decrypting api access");
    }
    DecryptGitAPIAccessTaskResponse gitConnectorResponse = (DecryptGitAPIAccessTaskResponse) responseData;
    return gitConnectorResponse.getScmConnector();
  }

  private List<EncryptedDataDetail> getEncryptedDataDetailsForAPIAccess(
      DecryptableEntity decryptableEntity, NGAccess ngAccess) {
    return encryptionHelper.getEncryptionDetail(decryptableEntity, ngAccess.getAccountIdentifier(),
        ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
  }

  public BitbucketConnectorDTO decryptBitBucketApiAccess(
      BitbucketConnectorDTO bitbucketConnectorDTO, NGAccess ngAccess) {
    List<EncryptedDataDetail> encryptedDataDetailsForAPIAccess = getEncryptedDataDetailsForAPIAccess(
        gitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(bitbucketConnectorDTO), ngAccess);
    return (BitbucketConnectorDTO) executeDecryptionTask(
        bitbucketConnectorDTO, ngAccess.getAccountIdentifier(), encryptedDataDetailsForAPIAccess);
  }

  public GitlabConnectorDTO decryptGitlabApiAccess(GitlabConnectorDTO gitlabConnectorDTO, NGAccess ngAccess) {
    List<EncryptedDataDetail> encryptedDataDetailsForAPIAccess = getEncryptedDataDetailsForAPIAccess(
        gitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(gitlabConnectorDTO), ngAccess);
    return (GitlabConnectorDTO) executeDecryptionTask(
        gitlabConnectorDTO, ngAccess.getAccountIdentifier(), encryptedDataDetailsForAPIAccess);
  }
}
