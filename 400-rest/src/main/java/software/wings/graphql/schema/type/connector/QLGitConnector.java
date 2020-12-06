package software.wings.graphql.schema.type.connector;

import software.wings.beans.GitConfig.UrlType;
import software.wings.graphql.schema.type.QLCustomCommitDetails;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.SETTING)
public class QLGitConnector implements QLConnector {
  private String id;
  private String name;
  private Long createdAt;
  private QLUser createdBy;

  private String userName;
  private String URL;
  private UrlType urlType;
  private String branch;
  private String passwordSecretId;
  private String sshSettingId;
  private String webhookUrl;
  private Boolean generateWebhookUrl;
  private QLCustomCommitDetails customCommitDetails;

  public static class QLGitConnectorBuilder implements QLConnectorBuilder {}
}
