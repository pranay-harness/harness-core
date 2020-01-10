package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowPermissionsKeys")
public class QLWorkflowPermissions {
  private Set<QLWorkflowFilterType> filterTypes;
  private Set<String> envIds;
}
