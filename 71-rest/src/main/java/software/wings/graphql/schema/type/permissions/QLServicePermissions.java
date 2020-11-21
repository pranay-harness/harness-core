package software.wings.graphql.schema.type.permissions;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServicePermissionsKeys")
public class QLServicePermissions {
  private QLPermissionsFilterType filterType;
  private Set<String> serviceIds;
}
