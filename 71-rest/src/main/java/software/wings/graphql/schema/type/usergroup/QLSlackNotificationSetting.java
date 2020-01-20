package software.wings.graphql.schema.type.usergroup;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSlackNotificationSettingKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLSlackNotificationSetting {
  String slackChannelName;
  String slackWebhookURL;
}
