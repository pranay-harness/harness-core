package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnvFilterType;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvPermissionsKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEnvPermissions {
  private Set<QLEnvFilterType> filterTypes;
  private Set<String> envIds;
}
