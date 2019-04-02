package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    logger.info("Running validation for task {} for repo {}", delegateTaskId, getRepoUrls());
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap =
        ((GitFetchFilesTaskParams) getParameters()[0]).getGitFetchFilesConfigMap();

    for (Entry<String, GitFetchFilesConfig> entry : gitFetchFileConfigMap.entrySet()) {
      GitFetchFilesConfig gitFetchFileConfig = entry.getValue();

      if (!validateGitConfig(gitFetchFileConfig.getGitConfig(), gitFetchFileConfig.getEncryptedDataDetails())) {
        return taskValidationResult(false);
      }
    }

    return taskValidationResult(true);
  }

  private List<DelegateConnectionResult> taskValidationResult(boolean validated) {
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  private boolean validateGitConfig(GitConfig gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + gitConfig, e);
      return false;
    }

    if (isNotEmpty(gitClient.validate(gitConfig, false))) {
      return false;
    }

    return true;
  }

  @Override
  public List<String> getCriteria() {
    return singletonList("GIT_FETCH_FILES:" + getRepoUrls());
  }

  private String getRepoUrls() {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap =
        ((GitFetchFilesTaskParams) getParameters()[0]).getGitFetchFilesConfigMap();

    StringBuilder repoUrls = new StringBuilder();
    for (Entry<String, GitFetchFilesConfig> entry : gitFetchFileConfigMap.entrySet()) {
      if (repoUrls.length() != 0) {
        repoUrls.append(',');
      }
      repoUrls.append(entry.getValue().getGitConfig().getRepoUrl());
    }

    return repoUrls.toString();
  }
}
