package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLProivionerPermissionsKeys")
public class QLProivionerPermissions {
  private QLPermissionsFilterType filterType;
  private Set<String> provisionerIds;
}
