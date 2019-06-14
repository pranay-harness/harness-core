package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

@Slf4j
public class GitFetchFilesValidation extends AbstractDelegateValidateTask {
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

      if (!validateGitConfig(gitFetchFileConfig.getGitConfig())) {
        return taskValidationResult(false);
      }
    }

    return taskValidationResult(true);
  }

  private List<DelegateConnectionResult> taskValidationResult(boolean validated) {
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  private boolean validateGitConfig(GitConfig gitConfig) {
    if (isNotEmpty(gitClient.validate(gitConfig))) {
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
