package software.wings.graphql.schema.type.permissions;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowPermissionsKeys")
public class QLWorkflowPermissions {
  private Set<QLWorkflowFilterType> filterTypes;
  private Set<String> envIds;
}
