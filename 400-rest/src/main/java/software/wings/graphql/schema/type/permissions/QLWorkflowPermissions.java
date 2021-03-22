package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowPermissionsKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLWorkflowPermissions {
  private Set<QLWorkflowFilterType> filterTypes;
  private Set<String> envIds;
}
