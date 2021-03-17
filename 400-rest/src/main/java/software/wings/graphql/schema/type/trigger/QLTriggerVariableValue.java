package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLTriggerVariableValueKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@AllArgsConstructor
@TargetModule(Module._380_CG_GRAPHQL)
public class QLTriggerVariableValue {
  private String name;
  private String value;
}
