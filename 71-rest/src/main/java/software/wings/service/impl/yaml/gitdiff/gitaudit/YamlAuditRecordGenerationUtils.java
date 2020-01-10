package software.wings.service.impl.yaml.gitdiff.gitaudit;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.audit.AuditHeader.Builder.anAuditHeader;
import static software.wings.beans.User.Builder.anUser;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.manage.GlobalContextManager;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.ResponseType;
import software.wings.audit.AuditRecord;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.GitAuditDetails;
import software.wings.beans.FeatureName;
import software.wings.beans.HttpMethod;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.common.AuditHelper;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.service.intfc.FeatureFlagService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class YamlAuditRecordGenerationUtils {
  @Inject private AuditHelper auditHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AuditYamlHelperForFailedChanges auditYamlHelper;
  @Inject private FeatureFlagService featureFlagService;

  public AuditHeader processGitChangesForAudit(String accountId, GitDiffResult gitDiffResult) {
    AuditHeader.Builder builder = anAuditHeader();
    builder.withCreatedAt(System.currentTimeMillis())
        .withCreatedBy(EmbeddedUser.builder().name("GIT_SYNC").uuid("GIT").build())
        .withGitAuditDetails(GitAuditDetails.builder()
                                 .gitCommitId(gitDiffResult.getCommitId())
                                 .repoUrl(gitDiffResult.getRepoName())
                                 .build())
        .withRemoteUser(anUser().withName("GIT_SYNC").withUuid("GIT").build())
        .withRequestMethod(HttpMethod.POST)
        .withRequestTime(System.currentTimeMillis())
        .withUrl("setup-as-code/yaml/webhook");

    return auditHelper.create(builder.build());
  }

  public boolean verifyGitAsSourceForAuditTrail(EmbeddedUser createdBy) {
    return createdBy != null && "GIT_SYNC".equals(createdBy.getName()) && "GIT".equals(createdBy.getUuid());
  }

  public void finalizeAuditRecord(
      String accountId, GitDiffResult gitDiffResult, Map<String, ChangeWithErrorMsg> changeWithErrorMsgs) {
    AuditGlobalContextData auditGlobalContextData =
        (AuditGlobalContextData) GlobalContextManager.get(AuditGlobalContextData.AUDIT_ID);
    AuditHeader auditHeader = wingsPersistence.get(AuditHeader.class, auditGlobalContextData.getAuditId());
    if (auditHeader == null) {
      logger.warn("Somehow AuditHeader was not created for GitSync operation. ALERT...");
      return;
    }

    auditHeader.setResponseTime(System.currentTimeMillis());
    if (isEmpty(changeWithErrorMsgs)) {
      // All changes were successfully ingested.
      updateAuditHeaderAsSuccess(accountId, auditHeader);
    } else {
      // There were some failures for yaml changes
      updateAuditHeaderCompletedWithErrors(accountId, auditHeader, gitDiffResult, changeWithErrorMsgs);
    }
  }

  /**
   * Everything was successful, just update AuditHeader with response details.
   */
  private void updateAuditHeaderAsSuccess(String accountId, AuditHeader auditHeader) {
    String msg = new StringBuilder(128)
                     .append("Yaml Changes Were Ingested Successfully for Account: ")
                     .append(accountId)
                     .append(", CommitId: ")
                     .append(auditHeader.getGitAuditDetails().getGitCommitId())
                     .toString();

    auditHeader.setResponseType(ResponseType.SUCCESS);
    auditHeader.setResponseStatusCode(200);
    byte[] response = msg.getBytes(Charset.forName("UTF-8"));
    auditHelper.finalizeAudit(auditHeader, response);
  }

  /**
   * Generate EntityAuditRecords for yamlPaths failed, and update those in AuditHeader
   */
  private void updateAuditHeaderCompletedWithErrors(String accountId, AuditHeader auditHeader,
      GitDiffResult gitDiffResult, Map<String, ChangeWithErrorMsg> changeWithErrorMsgs) {
    List<GitFileChange> changeListWithFailedChanges =
        gitDiffResult.getGitFileChanges()
            .stream()
            .filter(gitFileChange -> changeWithErrorMsgs.containsKey(gitFileChange.getFilePath()))
            .collect(toList());

    if (isEmpty(auditHeader.getEntityAuditRecords())) {
      auditHeader.setEntityAuditRecords(new ArrayList<>());
    }

    changeListWithFailedChanges.forEach(change -> {
      ChangeWithErrorMsg changeWithErrorMsg = changeWithErrorMsgs.get(change.getFilePath());
      try {
        EntityAuditRecord record = auditYamlHelper.generateEntityAuditRecordForFailedChanges(change, accountId);
        if (record != null) {
          if (changeWithErrorMsg != null) {
            record.setFailure(true);
            record.setYamlError(changeWithErrorMsg.getErrorMsg());
          }

          if (featureFlagService.isEnabled(FeatureName.ENTITY_AUDIT_RECORD, accountId)) {
            long now = System.currentTimeMillis();
            record.setCreatedAt(now);
            AuditRecord auditRecord = AuditRecord.builder()
                                          .auditHeaderId(auditHeader.getUuid())
                                          .entityAuditRecord(record)
                                          .createdAt(now)
                                          .accountId(accountId)
                                          .nextIteration(now + TimeUnit.MINUTES.toMillis(3))
                                          .build();

            wingsPersistence.save(auditRecord);
          } else {
            auditHeader.getEntityAuditRecords().add(record);
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to created EntityAuditRecord for error yaml path: " + change.getFilePath());
      }
    });

    String failureStatusMsg = generateFailureStatusMessage(changeWithErrorMsgs);
    boolean ifAllChangesFailed = gitDiffResult.getGitFileChanges().size() == changeWithErrorMsgs.size();
    auditHeader.setResponseType(ifAllChangesFailed ? ResponseType.FAILED : ResponseType.COMPLETED_WITH_ERRORS);
    auditHeader.setResponseStatusCode(ifAllChangesFailed ? 400 : 207);
    String msg = generateResponseMessage(
        accountId, auditHeader, changeListWithFailedChanges, gitDiffResult, changeWithErrorMsgs);
    updateAuditHeader(accountId, auditHeader, msg, failureStatusMsg);
  }

  private String generateFailureStatusMessage(Map<String, ChangeWithErrorMsg> changeWithErrorMsgs) {
    try {
      StringBuilder msg = new StringBuilder(128);
      msg.append("Failed paths are:  \n");
      changeWithErrorMsgs.keySet().forEach(path -> msg.append(path).append("  \n"));
      return msg.toString();
    } catch (Exception e) {
      logger.warn("Exception in handling audit for GitChanges: " + e);
    }

    return null;
  }

  /**
   * Update audit header with all details
   */
  private void updateAuditHeader(String accountId, AuditHeader auditHeader, String msg, String failureStatusMsg) {
    UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
    if (isNotEmpty(auditHeader.getEntityAuditRecords())
        && !featureFlagService.isEnabled(FeatureName.ENTITY_AUDIT_RECORD, accountId)) {
      operations.addToSet("entityAuditRecords", auditHeader.getEntityAuditRecords());
    }
    operations.set("accountId", accountId);

    if (isNotBlank(failureStatusMsg)) {
      operations.set("failureStatusMsg", failureStatusMsg);
    }

    // Add EntityAuditRecords to AuditHeader
    wingsPersistence.update(
        wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeader.getUuid()), operations);

    // This is mark exit of this audit process.
    auditHelper.finalizeAudit(auditHeader, msg.getBytes(Charset.forName("UTF-8")));
  }

  /**
   * Detailed Audit Response
   * with details of failed and successful changes
   */
  private String generateResponseMessage(String accountId, AuditHeader auditHeader,
      List<GitFileChange> changeListWithFailedChanges, GitDiffResult gitDiffResult,
      Map<String, ChangeWithErrorMsg> changeWithErrorMsgs) {
    try {
      List<GitFileChange> changeListWithSuccessfulChanges =
          gitDiffResult.getGitFileChanges()
              .stream()
              .filter(gitFileChange -> !changeWithErrorMsgs.containsKey(gitFileChange.getFilePath()))
              .collect(toList());

      StringBuilder msg = new StringBuilder(128)
                              .append("Status: AccountId: ")
                              .append(accountId)
                              .append(", CommitId: ")
                              .append(auditHeader.getGitAuditDetails().getGitCommitId())
                              .append("Repo: ")
                              .append(auditHeader.getGitAuditDetails().getRepoUrl())
                              .append("\nSuccessful Paths: \n");

      changeListWithSuccessfulChanges.forEach(change -> { msg.append(getChangePath(change)); });

      msg.append("--------------------------\nFailed Paths: \n");

      changeListWithFailedChanges.forEach(change -> { msg.append(getChangePath(change)); });
      return msg.toString();
    } catch (Exception e) {
      logger.warn("Exception in handling audit for GitChanges: " + e);
    }

    return null;
  }

  /**
   * When delete, new path will be empty and opposite for other cases
   */
  private String getChangePath(GitFileChange change) {
    return isEmpty(change.getFilePath()) ? change.getOldFilePath() + "\n" : change.getFilePath() + "\n";
  }
}
