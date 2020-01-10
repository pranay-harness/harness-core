package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLDeploymentPermissionsKeys")
public class QLDeploymentPermissions {
  private Set<QLDeploymentFilterType> filterTypes;
  private Set<String> envIds;
}
