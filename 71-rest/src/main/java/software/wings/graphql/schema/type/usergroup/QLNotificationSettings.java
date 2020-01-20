package software.wings.graphql.schema.type.usergroup;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

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
}
