package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.yaml.gitSync.EntityUpdateListEvent;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.Type;

public interface YamlGitSyncService {
  /**
   * Gets the yaml git sync info by entitytId
   *
   * @param entityId the uuid of the entity
   * @return the rest response
   */
  public YamlGitSync get(String entityId);

  /**
   * Gets the yaml git sync info by uuid
   *
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @return the rest response
   */
  public YamlGitSync get(String entityId, String accountId, String appId);

  /**
   * Gets the yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @return the rest response
   */
  public YamlGitSync get(Type type, String entityId, String accountId, String appId);

  public boolean exist(@NotEmpty Type type, @NotEmpty String entityId, @NotEmpty String accountId, String appId);

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param accountId the account id
   * @param appId the app id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync save(String accountId, String appId, YamlGitSync yamlGitSync);

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   *@param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync update(String entityId, String accountId, String appId, YamlGitSync yamlGitSync);

  // public boolean handleEntityUpdateEvent(EntityUpdateEvent entityUpdateEvent);

  public boolean handleEntityUpdateListEvent(EntityUpdateListEvent entityUpdateListEvent);

  public RestResponse processWebhookPost(String accountId, String webhookToken, String rawJson);

  public GitSyncWebhook getWebhook(String entityId, String accountId);
}
