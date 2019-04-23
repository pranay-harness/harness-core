package software.wings.service.impl.yaml.gitdiff;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.beans.FeatureName;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.service.impl.yaml.gitdiff.gitaudit.YamlAuditRecordGenerationUtil;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlGitService;

import java.io.IOException;
import java.util.Map;

@Singleton
@Slf4j
public class GitChangeSetProcesser {
  @Inject private GitChangeSetHandler gitChangesToEntityConverter;
  @Inject private YamlAuditRecordGenerationUtil gitChangeAuditRecordHandler;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private YamlGitService yamlGitService;

  public void processGitChangeSet(String accountId, GitDiffResult gitDiffResult) {
    // ensure gitCommit is not already processed. Else nothing to be done.
    boolean commitAlreadyProcessed = yamlGitService.isCommitAlreadyProcessed(accountId, gitDiffResult.getCommitId());
    if (commitAlreadyProcessed) {
      // do nothing
      logger.warn("Commit [{}] already processed for account {}", gitDiffResult.getCommitId(), accountId);
      return;
    }

    // Currently Audit is behind feature flag.
    if (!featureFlagService.isEnabled(FeatureName.AUDIT_TRAIL, accountId)) {
      gitChangesToEntityConverter.ingestGitYamlChangs(accountId, gitDiffResult);
      return;
    }

    // Injest Yaml Changes with Auditing
    ingestYamlChangeWithAudit(accountId, gitDiffResult);
  }

  private void ingestYamlChangeWithAudit(String accountId, GitDiffResult gitDiffResult) {
    AuditHeader auditHeader = gitChangeAuditRecordHandler.processGitChangesForAudit(accountId, gitDiffResult);

    Map<String, ChangeWithErrorMsg> changeWithErrorMsgs = null;
    try (GlobalContextGuard guard = GlobalContextManager.globalContextGuard(
             AuditGlobalContextData.builder().auditId(auditHeader.getUuid()).build())) {
      // changeWithErrorMsgs is a map of <YamlPath, ErrorMessage> for failed yaml changes
      changeWithErrorMsgs = gitChangesToEntityConverter.ingestGitYamlChangs(accountId, gitDiffResult);

      // Finalize audit.
      // 1. Mark exit point.
      // 2. Set status code 200 / 207 (multi-status code (indicating success and failure for some paths)
      // 3. Set detailed message
      gitChangeAuditRecordHandler.finalizeAuditRecord(accountId, gitDiffResult, changeWithErrorMsgs);
    } catch (IOException e) {
      logger.warn("Error occured in  in GlobalContextGuard..." + e);
    }
  }
}
