package io.harness.cdng.tasks.manifestFetch;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchFilesConfig;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchRequest;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.ContainerValidationHelper;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.GitFetchFilesValidationHelper;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class GitFetchValidationNG extends AbstractDelegateValidateTask {
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;
  @Inject private ContainerValidationHelper containerValidationHelper;
  @Inject private GitFetchFilesValidationHelper gitFetchFilesValidationHelper;

  public GitFetchValidationNG(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    GitFetchRequest gitFetchRequest = (GitFetchRequest) getParameters()[0];

    // Run validation task for container service parameters
    logger.info("Running validation for task {} for container service parameters", delegateTaskId);

    logger.info("Running validation for task {} for repo {}", delegateTaskId, getRepoUrls());

    for (GitFetchFilesConfig gitFetchFilesConfig : gitFetchRequest.getGitFetchFilesConfigs()) {
      if (!gitFetchFilesValidationHelper.validateGitConfig(
              gitFetchFilesConfig.getGitConfig(), gitFetchFilesConfig.getEncryptedDataDetails())) {
        return taskValidationResult(false);
      }
    }
    return taskValidationResult(true);
  }

  private List<DelegateConnectionResult> taskValidationResult(boolean validated) {
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    StringBuilder sb = new StringBuilder(128);
    sb.append("GIT_FETCH_FILES:").append(getRepoUrls());
    return singletonList(sb.toString());
  }

  private String getRepoUrls() {
    GitFetchRequest gitFetchRequest = (GitFetchRequest) getParameters()[0];

    StringBuilder repoUrls = new StringBuilder();
    for (GitFetchFilesConfig gitFetchFilesConfig : gitFetchRequest.getGitFetchFilesConfigs()) {
      if (repoUrls.length() != 0) {
        repoUrls.append(',');
      }
      repoUrls.append(gitFetchFilesConfig.getGitConfig().getRepoUrl());
    }

    return repoUrls.toString();
  }
}
