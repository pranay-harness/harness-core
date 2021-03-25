package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnvFilterType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvScopeFilterKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEnvScopeFilter {
  private QLEnvFilterType filterType;
  private String envId;
}
