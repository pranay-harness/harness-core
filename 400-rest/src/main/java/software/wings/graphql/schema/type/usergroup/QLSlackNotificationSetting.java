package software.wings.graphql.schema.type.usergroup;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSlackNotificationSettingKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLSlackNotificationSetting {
  String slackChannelName;
  String slackWebhookURL;
}
