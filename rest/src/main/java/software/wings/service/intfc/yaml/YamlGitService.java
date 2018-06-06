package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ErrorCode;
import software.wings.beans.GitCommit;
import software.wings.beans.RestResponse;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.YamlWebHookPayload;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

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
  YamlGitConfig get(@NotEmpty String accountId, @NotEmpty String entityId);

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
  void fullSync(@NotEmpty String accountId, boolean forcePush);

  /**
   * Sync files.
   *
   * @param accountId         the account id
   * @param gitFileChangeList the git file change list
   * @param forcePush         force push
   */
  void syncFiles(String accountId, List<GitFileChange> gitFileChangeList, boolean forcePush);

  /**
   * Perform full sync dry run list.
   *
   * @param accountId the account id
   * @return the list
   */
  List<GitFileChange> performFullSyncDryRun(String accountId);

  void performFullSyncDryRunOnAllAccounts();

  /**
   * Handle change set boolean.
   *
   * @param entityUpdateListEvent the entity update list event
   * @return the boolean
   */
  boolean handleChangeSet(YamlChangeSet entityUpdateListEvent);

  /**
   * Process webhook post.
   *
   * @param accountId          the account id
   * @param webhookToken       the webhook token
   * @param yamlWebHookPayload the yaml web hook payload
   */
  void processWebhookPost(String accountId, String webhookToken, YamlWebHookPayload yamlWebHookPayload);

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
  void processFailedChanges(String accountId, Map<Change, String> failedChangeErrorMsgMap, boolean gitToHarness);

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
   * @param yamlFilePath the yaml file path
   * @return the rest response
   */
  RestResponse discardGitSyncError(String accountId, String yamlFilePath);

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

  RestResponse discardGitSyncErrorForFullSync(String accountId);
}
