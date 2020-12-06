package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

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
  String gitConnectorId;
  String gitConnectorName;
  List<String> filePaths;
  Boolean deployOnlyIfFilesChanged;
  String branchName;
  String repoName;
}
