package software.wings.graphql.schema.type.usergroup;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLNotificationSettingsKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLNotificationSettings {
  Boolean sendNotificationToMembers;
  Boolean sendMailToNewMembers;
  List<String> groupEmailAddresses;
  QLSlackNotificationSetting slackNotificationSetting;
  String pagerDutyIntegrationKey;
  String microsoftTeamsWebhookUrl;
}
