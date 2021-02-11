package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.Module;
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
@TargetModule(Module._380_CG_GRAPHQL)
public class QLScheduleConditionInput {
  private Boolean onNewArtifactOnly;
  private String cronExpression;
}
