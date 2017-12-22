package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.GitCommit;
import software.wings.beans.RestResponse;
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
   */
  void fullSync(@NotEmpty String accountId);

  void syncFiles(String accountId, List<GitFileChange> gitFileChangeList);

  List<GitFileChange> performFullSyncDryRun(String accountId);

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
   * Gets webhook.
   *
   * @param entityId  the entity id
   * @param accountId the account id
   * @return the webhook
   */
  GitSyncWebhook getWebhook(String entityId, String accountId);

  GitCommit saveCommit(GitCommit gitCommit);

  void processFailedOrUnprocessedChanges(
      List<GitFileChange> failedOrPendingChanges, Change failedChange, String errorMessage);

  void removeGitSyncErrors(String accountId, List<GitFileChange> gitFileChangeList);

  RestResponse<List<GitSyncError>> listGitSyncErrors(String accountId);

  long getGitSyncErrorCount(String accountId);

  RestResponse discardGitSyncErrors(String accountId, List<String> yamlFilePathList);

  RestResponse fixGitSyncErrors(String accountId, String yamlFilePath, String newYamlContent);
}
