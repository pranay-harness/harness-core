package software.wings.service.impl.yaml.gitdiff;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.manage.GlobalContextManager.ensureGlobalContextGuard;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.mongo.ProcessTimeLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.service.impl.yaml.YamlProcessingLogContext;
import software.wings.service.impl.yaml.gitdiff.gitaudit.YamlAuditRecordGenerationUtils;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlGitService;

import java.util.Map;

@Singleton
@Slf4j
public class GitChangeSetProcesser {
  @Inject private GitChangeSetHandler gitChangesToEntityConverter;
  @Inject private YamlAuditRecordGenerationUtils gitChangeAuditRecordHandler;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private YamlGitService yamlGitService;

  public void processGitChangeSet(String accountId, GitDiffResult gitDiffResult) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    try (AccountLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         YamlProcessingLogContext ignore2 = YamlProcessingLogContext.builder()
                                                .branchName(gitDiffResult.getBranch())
                                                .repoName(gitDiffResult.getRepoName())
                                                .commitId(gitDiffResult.getCommitId())
                                                .build(OVERRIDE_ERROR)) {
      logger.info(GIT_YAML_LOG_PREFIX + "Started processing git diff results");
      // ensure gitCommit is not already processed. Else nothing to be done.
      boolean commitAlreadyProcessed = yamlGitService.isCommitAlreadyProcessed(accountId, gitDiffResult.getCommitId());
      if (commitAlreadyProcessed) {
        // do nothing
        logger.warn(GIT_YAML_LOG_PREFIX + "Commit [{}] already processed for account {}", gitDiffResult.getCommitId(),
            accountId);
        return;
      }

      // Injest Yaml Changes with Auditing
      ingestYamlChangeWithAudit(accountId, gitDiffResult);

      try (ProcessTimeLogContext ignore3 = new ProcessTimeLogContext(stopwatch.elapsed(MILLISECONDS), OVERRIDE_ERROR)) {
        logger.info(GIT_YAML_LOG_PREFIX + "Successfully  processed git diff results");
      }
    }
  }

  private void ingestYamlChangeWithAudit(String accountId, GitDiffResult gitDiffResult) {
    // This will create Audit header record for git sync
    gitChangeAuditRecordHandler.processGitChangesForAudit(accountId, gitDiffResult);

    Map<String, ChangeWithErrorMsg> changeWithErrorMsgs = null;

    try (GlobalContextGuard guard = ensureGlobalContextGuard()) {
      // changeWithErrorMsgs is a map of <YamlPath, ErrorMessage> for failed yaml changes
      changeWithErrorMsgs = gitChangesToEntityConverter.ingestGitYamlChangs(accountId, gitDiffResult);

      // Finalize audit.
      // 1. Mark exit point.
      // 2. Set status code 200 / 207 (multi-status code (indicating success and failure for some paths)
      // 3. Set detailed message
      gitChangeAuditRecordHandler.finalizeAuditRecord(accountId, gitDiffResult, changeWithErrorMsgs);
    }
  }
}
