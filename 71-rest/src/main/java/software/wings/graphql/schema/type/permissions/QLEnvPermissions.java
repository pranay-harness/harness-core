package software.wings.graphql.schema.type.permissions;

import software.wings.graphql.schema.type.QLEnvFilterType;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvPermissionsKeys")
public class QLEnvPermissions {
  private Set<QLEnvFilterType> filterTypes;
  private Set<String> envIds;
}
