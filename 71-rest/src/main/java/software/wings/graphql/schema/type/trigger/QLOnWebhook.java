package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLOnWebhookKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLOnWebhook implements QLTriggerCondition {
  QLTriggerConditionType triggerConditionType;
  QLWebhookSource webhookSource;
  QLWebhookEvent webhookEvent;
  QLWebhookDetails webhookDetails;
  String branchRegex;
}
