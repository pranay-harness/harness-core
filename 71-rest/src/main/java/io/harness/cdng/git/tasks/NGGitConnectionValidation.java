package io.harness.cdng.git.tasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.git.NGGitService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class NGGitConnectionValidation extends AbstractDelegateValidateTask {
  @Inject private SecretDecryptionService decryptionService;
  @Inject private NGGitService gitService;

  public NGGitConnectionValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    GitCommandParams gitCommandParams = (GitCommandParams) getParameters()[0];
    GitConfigDTO gitConfig = gitCommandParams.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = gitCommandParams.getEncryptionDetails();

    logger.info("Running validation for task {} for repo {}", delegateTaskId, gitConfig.getUrl());
    try {
      decryptionService.decrypt(gitConfig.getGitAuth(), encryptionDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + gitConfig, e);
      return singletonList(DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
    }

    boolean validated = true;
    // TODO(abhinav): add accountId
    if (isNotEmpty(gitService.validate(gitConfig, "accountId"))) {
      validated = false;
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    GitCommandParams gitCommandParams = (GitCommandParams) getParameters()[0];
    GitConfigDTO gitConfig = gitCommandParams.getGitConfig();
    return singletonList("GIT:" + gitConfig.getUrl());
  }
}
