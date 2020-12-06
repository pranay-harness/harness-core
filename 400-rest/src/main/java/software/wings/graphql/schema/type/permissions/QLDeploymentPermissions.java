package software.wings.graphql.schema.type.permissions;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeploymentPermissionsKeys")
public class QLDeploymentPermissions {
  private Set<QLDeploymentFilterType> filterTypes;
  private Set<String> envIds;
}
