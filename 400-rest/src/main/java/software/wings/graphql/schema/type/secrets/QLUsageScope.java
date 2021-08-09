package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUsageScopeKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUsageScope {
  Set<QLAppEnvScope> appEnvScopes;
}
