package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.service.impl.ContainerServiceParams;
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
  @Inject private ContainerValidationHelper containerValidationHelper;

  public GitFetchFilesValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    GitFetchFilesTaskParams parameters = (GitFetchFilesTaskParams) getParameters()[0];

    // Run validation task for container service parameters
    logger.info("Running validation for task {} for container service parameters", delegateTaskId);
    ContainerServiceParams containerServiceParams = parameters.getContainerServiceParams();

    if (containerServiceParams == null) {
      logger.info("Container Service Parameters is null for task {}", delegateTaskId);
    } else {
      if (parameters.isBindTaskFeatureSet()
          && !containerValidationHelper.validateContainerServiceParams(containerServiceParams)) {
        logger.info("Failed validation for task {} for container service parameters", delegateTaskId);
        return taskValidationResult(false);
      }
    }

    logger.info("Running validation for task {} for repo {}", delegateTaskId, getRepoUrls());
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = parameters.getGitFetchFilesConfigMap();

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

    if (isNotEmpty(gitClient.validate(gitConfig))) {
      return false;
    }
    return true;
  }

  @Override
  public List<String> getCriteria() {
    StringBuilder sb = new StringBuilder(128);
    sb.append("GIT_FETCH_FILES:").append(getRepoUrls());
    GitFetchFilesTaskParams parameters = (GitFetchFilesTaskParams) getParameters()[0];
    if (parameters.isBindTaskFeatureSet() && parameters.getContainerServiceParams() != null) {
      sb.append(" from ").append(containerValidationHelper.getCriteria(parameters.getContainerServiceParams()));
    }

    return singletonList(sb.toString());
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
