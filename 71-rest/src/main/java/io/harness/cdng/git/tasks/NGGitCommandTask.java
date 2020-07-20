package io.harness.cdng.git.tasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Inject;

import io.harness.cdng.gitclient.GitClientNG;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommand.GitCommandType;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class NGGitCommandTask extends AbstractDelegateRunnableTask {
  @Inject private GitClientNG gitClient;
  @Inject private EncryptionService encryptionService;
  public NGGitCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    GitCommandParams gitCommandParams = (GitCommandParams) parameters;
    GitConfigDTO gitConfig = gitCommandParams.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = gitCommandParams.getEncryptionDetails();
    encryptionService.decrypt(gitConfig.getGitAuth(), encryptionDetails);
    GitCommandType gitCommandType = gitCommandParams.getGitCommandType();
    switch (gitCommandType) {
      case VALIDATE:
        return handleValidateTask(gitConfig);
      default:
        return GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandStatus.FAILURE)
            .errorMessage(GIT_YAML_LOG_PREFIX + "Git Operation not supported")
            .build();
    }
  }

  private ResponseData handleValidateTask(GitConfigDTO gitConfig) {
    logger.info("Processing Git command: VALIDATE");
    String errorMessage = gitClient.validate(gitConfig);
    if (isEmpty(errorMessage)) {
      return GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();
    } else {
      return GitCommandExecutionResponse.builder()
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .errorMessage(errorMessage)
          .build();
    }
  }
}
