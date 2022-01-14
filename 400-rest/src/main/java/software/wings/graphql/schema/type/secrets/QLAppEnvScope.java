package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppEnvScopesKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAppEnvScope {
  private QLAppScopeFilter application;
  private QLEnvScopeFilter environment;
}
