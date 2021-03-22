package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLProivionerPermissionsKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLProivionerPermissions {
  private QLPermissionsFilterType filterType;
  private Set<String> provisionerIds;
}
