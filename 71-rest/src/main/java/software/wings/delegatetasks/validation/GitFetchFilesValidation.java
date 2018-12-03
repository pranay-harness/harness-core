package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

public class GitFetchFilesValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(GitValidation.class);
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;

  public GitFetchFilesValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    GitFetchFilesTaskParams taskParams = (GitFetchFilesTaskParams) getParameters()[0];
    GitConfig gitConfig = taskParams.getGitConfig();

    logger.info("Running validation for task {} for repo {}", delegateTaskId, gitConfig.getRepoUrl());
    List<EncryptedDataDetail> encryptionDetails = taskParams.getEncryptedDataDetails();
    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + gitConfig, e);
      return singletonList(DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
    }

    boolean validated = true;
    if (isNotEmpty(gitClient.validate(gitConfig, false))) {
      validated = false;
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        "GIT_FETCH_FILES:" + ((GitFetchFilesTaskParams) getParameters()[0]).getGitConfig().getRepoUrl());
  }
}
