package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public class QLTriggerConditionInput {
  private QLConditionType conditionType;
  private QLArtifactConditionInput artifactConditionInput;
  private QLPipelineConditionInput pipelineConditionInput;
  private QLScheduleConditionInput scheduleConditionInput;
  private QLWebhookConditionInput webhookConditionInput;
  private QLManifestConditionInput manifestConditionInput;
}
