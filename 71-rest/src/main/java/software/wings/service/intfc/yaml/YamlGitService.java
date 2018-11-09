package software.wings.service.intfc.yaml;

import io.harness.eraro.ErrorCode;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.YamlProcessingException.ChangeWithErrorMsg;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.HttpHeaders;

/**
 * The interface Yaml git sync service.
 */
public interface YamlGitService {
  /**
   * Gets the yaml git sync info by entitytId
   *
   * @param accountId the account id
   * @param entityId  the entity id
   * @return the rest response
   */
  YamlGitConfig get(@NotEmpty String accountId, @NotEmpty String entityId, @Valid EntityType entityType);

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  @ValidationGroups(Create.class) YamlGitConfig save(@Valid YamlGitConfig yamlGitSync);

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  @ValidationGroups(Update.class) YamlGitConfig update(@Valid YamlGitConfig yamlGitSync);

  /**
   * Push directory.
   *
   * @param accountId the account id
   * @param forcePush force push
   */
  void fullSync(
      @NotEmpty String accountId, @NotEmpty String entityId, @NotNull EntityType entityType, boolean forcePush);

  /**
   * Perform full sync dry run list.
   *
   * @param accountId the account id
   * @return the list
   */
  List<GitFileChange> performFullSyncDryRun(String accountId);
  void performFullSyncDryRunOnAllAccounts();

  List<String> getAllYamlErrorsForAccount(String accountId);
  List<String> getAllYamlErrorsForAllAccounts();

  /**
   *  Handle change set boolean.
   * @param yamlChangeSet
   * @param accountId
   * @return
   */
  boolean handleChangeSet(List<YamlChangeSet> yamlChangeSet, String accountId);

  void processWebhookPost(String accountId, String webhookToken, String yamlWebHookPayload, HttpHeaders headers);

  /**
   * Is commit already processed boolean.
   *
   * @param accountId  the account id
   * @param headCommit the head commit
   * @return the boolean
   */
  boolean isCommitAlreadyProcessed(String accountId, String headCommit);

  /**
   * Gets webhook.
   *
   * @param entityId  the entity id
   * @param accountId the account id
   * @return the webhook
   */
  GitSyncWebhook getWebhook(String entityId, String accountId);

  /**
   * Save commit git commit.
   *
   * @param gitCommit the git commit
   * @return the git commit
   */
  GitCommit saveCommit(GitCommit gitCommit);

  /**
   * Create git sync errors for failed changes
   * @param accountId
   * @param failedChangeErrorMsgMap
   */
  void processFailedChanges(
      String accountId, Map<String, ChangeWithErrorMsg> failedChangeErrorMsgMap, boolean gitToHarness);

  /**
   * Remove git sync errors.
   *
   * @param accountId         the account id
   * @param gitFileChangeList the git file change list
   */
  void removeGitSyncErrors(String accountId, List<GitFileChange> gitFileChangeList, boolean gitToHarness);

  /**
   * List git sync errors rest response.
   *
   * @param accountId the account id
   * @return the rest response
   */
  RestResponse<List<GitSyncError>> listGitSyncErrors(String accountId);

  /**
   * Gets git sync error count.
   *
   * @param accountId the account id
   * @return the git sync error count
   */
  long getGitSyncErrorCount(String accountId);

  /**
   * Discard git sync error rest response.
   *
   * @param accountId    the account id
   * @param errorId the yaml file path
   * @return the rest response
   */
  RestResponse discardGitSyncError(String accountId, String errorId);

  /**
   * Fix git sync errors rest response.
   *
   * @param accountId      the account id
   * @param yamlFilePath   the yaml file path
   * @param newYamlContent the new yaml content
   * @return the rest response
   */
  RestResponse fixGitSyncErrors(String accountId, String yamlFilePath, String newYamlContent);

  void raiseAlertForGitFailure(String accountId, String appId, ErrorCode errorCode, String message);

  void closeAlertForGitFailureIfOpen(String accountId, String appId, AlertType alertType, AlertData alertData);

  <T extends Change> void upsertGitSyncErrors(T failedChange, String errorMessage, boolean fullSyncPath);

  RestResponse discardGitSyncErrorForFullSync(String accountId, String appId);

  RestResponse discardGitSyncErrorsForGivenIds(String accountId, List<String> errorIds);

  RestResponse discardAllGitSyncError(String accountId);

  SettingAttribute getAndDecryptSettingAttribute(String sshSettingId);

  List<YamlChangeSet> obtainChangeSetFromFullSyncDryRun(String accountId);

  boolean checkApplicationChange(GitFileChange gitFileChange);

  String obtainAppNameFromGitFileChange(GitFileChange gitFileChange);

  void delete(String accountId, String entityId, EntityType entityType);

  GitConfig getGitConfig(YamlGitConfig ygs);

  YamlGitConfig save(YamlGitConfig ygs, boolean performFullSync);

  void fullSyncForEntireAccount(String accountId);
}
