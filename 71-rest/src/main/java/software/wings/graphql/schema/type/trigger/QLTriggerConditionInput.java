package software.wings.graphql.schema.type.trigger;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLTriggerConditionInput {
  private QLConditionType conditionType;
  private QLArtifactConditionInput artifactConditionInput;
  private QLPipelineConditionInput pipelineConditionInput;
  private QLScheduleConditionInput scheduleConditionInput;
  private QLWebhookConditionInput webhookConditionInput;
}
